# Findings Report — todo-list-api / feature 001-crud-tarefas

- **Data:** 2026-06-01
- **Revisor:** Security Engineer (revisão estática, sem build)
- **Projeto:** todo-list-api
- **Posture:** **Prototype** (per `memory/constitution.md` § Non-Goals + § Constraints — sem auth, sem multi-tenant, sem produção real, sem dados sensíveis)
- **Escopo revisado:** todos os fontes em `src/main/java/com/matheusmelo/todolist/` + `src/main/resources/application.yml` + `pom.xml` + `.env.example` + `.gitignore`
- **Branch:** `feat/001-crud-tarefas` (sem `main` para diff — revisão de toda a árvore de fontes)
- **Frameworks aplicados:** OWASP WSTG (INPV, ERRH, ATHN/ATHZ, BUSL, APIT, CRYP, CONF, INFO), OWASP Top 10:2025, OWASP API Security Top 10, STRIDE
- **Threat model prévio:** Nenhum (projeto de teste). STRIDE leve incluído abaixo.

> **Atualização 2026-06-01 — decisão do usuário:** Matheus Melo **NÃO aceitou** os riscos das findings LOW F-0002, F-0003, F-0006 e F-0007. Elas ficam registradas como **pendências abertas (TODO), a remediar antes de qualquer deploy público** — não foram implementadas nesta feature e a constituição **não** foi alterada (escopo mínimo do v1 mantido). O veredito de gate permanece **GO** para a posture Prototype (nenhum bloqueador), mas a feature carrega débito de segurança explicitamente registrado, não silenciado nem aceito.

---

## Limiar de bloqueio nesta posture

Per `security-conventions.md` § Project Posture → **Prototype bloqueia apenas CRITICAL**. HIGH/MEDIUM/LOW são registrados com aceitação de risco documentada e diferíveis para pós-protótipo. Nenhum achado CRITICAL foi encontrado.

---

## STRIDE leve (pós-implementação)

**Limites de confiança:** cliente ↔ servidor (HTTP, sem auth — anônimo total por decisão da constituição); servidor ↔ MongoDB. Não há fronteira anônimo↔autenticado nem role↔role (auth fora de escopo).

| Categoria | Ameaça | Estado na implementação |
|-----------|--------|--------------------------|
| **S**poofing | Sem identidade/auth — qualquer cliente é anônimo | Aceito por decisão da constituição (Non-Goal #1). Não aplicável à posture. |
| **T**ampering | Cliente injetar `id`/`dataCriacao` via body | **Mitigado** — DTOs não expõem esses campos; mapper nunca os define a partir de entrada (F-0005). |
| **T**ampering | NoSQL injection via campos de entrada | **Mitigado** — só `MongoRepository` derivado, sem query construída por string; binding tipado (F-0004). |
| **R**epudiation | Sem trilha de auditoria | Diferível (Prototype, sem usuários). Ver F-0006. |
| **I**nformation Disclosure | Vazar stack trace / internals em erro | **Mitigado** — `GlobalExceptionHandler` retorna shape padrão; sem handler genérico expondo internals, mas há lacuna no fallback 500 (F-0001). |
| **D**enial of Service | Sem rate limiting; payload/coleção sem limite | Parcial — `@Size` limita campos; sem rate limit nem paginação. Diferível (F-0002, F-0003). |
| **E**levation of Privilege | Sem papéis — não há privilégio a escalar | N/A nesta posture. |

---

## Achados

---

### Finding 0001: Exceções não tratadas caem no error handler padrão do Spring (risco residual de vazamento de detalhes)

- **Severity:** LOW
- **OWASP Top 10:2025:** A05 — Security Misconfiguration / A04 — Insecure Design (parcial)
- **Framework citation(s):** WSTG-ERRH-01/02, API Top 10 API8 (Security Misconfiguration), ASVS V7.4
- **Affected component:** Backend — `exception/GlobalExceptionHandler.java` (cobre apenas `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `ResourceNotFoundException`)
- **Status:** Open
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
O `@RestControllerAdvice` trata três exceções específicas. Qualquer outra exceção não mapeada (ex.: `IllegalArgumentException` de um `ObjectId` mal-formado no `findById`, `DataAccessException` do Mongo, `NullPointerException`) não tem handler e cai no `BasicErrorController` padrão do Spring Boot, que pode expor mais detalhes do que o shape padrão da convenção e não garante a mensagem genérica.

#### Evidence
`GlobalExceptionHandler.java` não possui `@ExceptionHandler(Exception.class)` de fallback. Em `application.yml` não há `server.error.include-stacktrace=never` nem `include-message`/`include-exception` configurados, então o comportamento depende do default do Spring Boot 3.5 (stacktrace `never` por padrão, mas message/exception podem variar e o shape diverge do padrão da convenção).

#### Impact
Resposta de erro inconsistente com a convenção (`security-conventions.md § Error Responses`) e possível exposição de detalhes de framework/exceção em casos não mapeados. Em Prototype, sem dados sensíveis, o impacto é baixo.

#### Likelihood
Easy — basta um `GET /tarefas/{id}` com um id em formato inválido para o MongoDB para potencialmente disparar uma exceção não mapeada.

#### Remediation
Adicionar um `@ExceptionHandler(Exception.class)` de fallback retornando 500 com mensagem genérica ("erro interno") no shape `ErrorResponse`, logando o detalhe apenas no servidor. Adicionalmente fixar em `application.yml`: `server.error.include-stacktrace: never`, `include-message: never`, `include-exception: false`. Per `§ Error Responses` (sem information disclosure) e `§ Logging` (detalhe só no servidor).

#### Remediation Route
- **Owner role:** Backend Engineer
- **Reason routed there:** correção de implementação (novo handler) + ajuste de config; padrão já estabelecido no arquivo existente.

---

### Finding 0002: Ausência de rate limiting / controle de abuso

- **Severity:** LOW
- **OWASP Top 10:2025:** A04 — Insecure Design
- **Framework citation(s):** WSTG-BUSL, API Top 10 API4 (Unrestricted Resource Consumption), ASVS V11.x (anti-automation)
- **Affected component:** Backend — `controller/TarefaController.java` (todos os endpoints)
- **Status:** Open — Risco NÃO aceito (TODO: remediar antes de qualquer deploy público)
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
Nenhum endpoint tem rate limiting. Um cliente pode criar tarefas em loop sem limite, inflando a coleção.

#### Evidence
Sem filtro/interceptor de rate limit; sem dependência de rate limiting no `pom.xml`.

#### Impact
Exaustão de recursos (crescimento ilimitado da coleção `tarefas`, pressão de I/O). Sem usuários reais nem exposição pública, o impacto é teórico nesta posture.

#### Likelihood
Hard (não exposto — roda localmente, sem tráfego real).

#### Remediation
Diferir. Per `security-conventions.md § Rate Limiting` e `security-build-vs-buy.md`: rate limiting no edge (Cloudflare) é a escolha BUY quando houver deploy público; rate limit in-app como defesa em profundidade. A própria constituição (§ Constraints) coloca rate limiting fora do v1 — alinhado.

#### Remediation Route
- **Owner role:** Backend Architect (decisão de onde aplicar o limite — edge vs in-app, na transição para deploy público)

#### Decisão do usuário (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01.
- **Status resultante:** Pendência aberta (TODO) — **a remediar ANTES de qualquer deploy público**. Não implementado nesta feature; constituição inalterada (escopo do v1 mantido).
- **Reason:** Posture Prototype sem exposição pública; explicitamente fora do v1 per constituição § Constraints.
- **Revisit trigger:** Qualquer deploy acessível fora de localhost / transição para MVP.

---

### Finding 0003: `GET /tarefas` retorna a coleção inteira sem paginação nem limite

- **Severity:** LOW
- **OWASP Top 10:2025:** A04 — Insecure Design
- **Framework citation(s):** WSTG-BUSL, API Top 10 API4 (Unrestricted Resource Consumption)
- **Affected component:** Backend — `service/TarefaService.java` `listar()` (`repository.findAll()`)
- **Status:** Open — Risco NÃO aceito (TODO: remediar antes de qualquer deploy público)
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
`listar()` faz `findAll()` e materializa toda a coleção em memória. Com a coleção grande (ver F-0002), isso vira um vetor de consumo de memória/latência.

#### Evidence
`TarefaService.listar()` → `repository.findAll().stream()...toList()`.

#### Impact
Degradação de performance / OOM com coleção grande. Em Prototype, dataset pequeno e local — impacto baixo.

#### Likelihood
Moderate (somente se a coleção crescer muito).

#### Remediation
Diferir. Paginação está explicitamente fora do v1 (constituição § Non-Goals #3). Ao sair do protótipo, adotar `Pageable`/limite por página.

#### Remediation Route
- **Owner role:** Product Strategist (paginação é Non-Goal declarado — qualquer inclusão passa por PDR per constituição § Constraints), depois Backend Engineer para implementar.

#### Decisão do usuário (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01.
- **Status resultante:** Pendência aberta (TODO) — **a remediar ANTES de qualquer deploy público**. Não implementado nesta feature; constituição inalterada (escopo do v1 mantido).
- **Reason:** Non-Goal explícito do v1; dataset de protótipo pequeno.
- **Revisit trigger:** Volume de tarefas que torne a listagem lenta, ou transição para MVP/produção.

---

### Finding 0004: Validação de entrada e injeção NoSQL — sem achados (cobertura confirmada)

- **Severity:** INFO
- **OWASP Top 10:2025:** A03 — Injection (verificado, não explorável)
- **Framework citation(s):** WSTG-INPV (incl. NoSQL injection), API Top 10 API8
- **Affected component:** Backend — DTOs (`TarefaCreateRequest`, `TarefaUpdateRequest`), `TarefaRepository`
- **Status:** Resolved (positivo — sem ação)
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
Entrada validada via Jakarta Validation (`@NotBlank`, `@Size`, `@NotNull`) com `@Valid` nos controllers. O enum `StatusTarefa` rejeita valores fora do contrato no desserializador Jackson. Persistência só via `MongoRepository` derivado, sem queries construídas por concatenação/`Criteria` dinâmico — sem superfície de NoSQL injection. Binding tipado para POJO impede operadores `$`-injetados em campos escalares.

#### Evidence
DTOs com constraints; `TarefaController` usa `@Valid`; `TarefaRepository` sem métodos customizados; `StatusTarefa` com `@JsonValue`.

#### Impact / Likelihood
N/A — controle presente, alinhado a `security-build-vs-buy.md` (Input validation in-app = BUILD com Jakarta Validation, padrão estabelecido).

#### Remediation
Nenhuma. Manter o padrão.

#### Remediation Route
- **Owner role:** N/A (informativo)

---

### Finding 0005: Mass assignment de campos servidor (id/dataCriacao) — mitigado por design

- **Severity:** INFO
- **OWASP Top 10:2025:** A08 — Software and Data Integrity Failures / A04 (parcial)
- **Framework citation(s):** WSTG-BUSL, API Top 10 API6 (Mass Assignment / BOPLA)
- **Affected component:** Backend — `mapper/TarefaMapper.java`, DTOs
- **Status:** Resolved (positivo — sem ação)
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
`id` e `dataCriacao` não são aceitos como entrada: os DTOs de request não os declaram e o mapper só define `titulo`/`descricao`/`status`. `dataCriacao` é setada server-side no `criar()` e preservada no update; `id` é gerado pelo Mongo. Não há risco de o cliente sobrescrever campos imutáveis (mass assignment).

#### Evidence
`TarefaCreateRequest`/`TarefaUpdateRequest` sem `id`/`dataCriacao`; `TarefaMapper.toEntity`/`applyUpdate` não tocam esses campos; `TarefaService.criar` seta `dataCriacao = Instant.now()`.

#### Remediation
Nenhuma. Padrão correto de boundary DTO↔entidade.

#### Remediation Route
- **Owner role:** N/A (informativo)

---

### Finding 0006: Ausência de logging de segurança / trilha de auditoria

- **Severity:** LOW
- **OWASP Top 10:2025:** A09 — Security Logging and Monitoring Failures
- **Framework citation(s):** WSTG-ERRH, ASVS V7 (Logging)
- **Affected component:** Backend — toda a aplicação (apenas log de warn em exceções no handler)
- **Status:** Open — Risco NÃO aceito (TODO: remediar antes de qualquer deploy público)
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
Não há logging de eventos de segurança nem trilha de auditoria de mutações (create/update/delete). O único logging é `log.warn` no `GlobalExceptionHandler`. Sem auth, não há eventos de autenticação/autorização a logar.

#### Impact
Sem capacidade de investigar abuso/incidentes. Em Prototype sem usuários, irrelevante na prática.

#### Likelihood
N/A nesta posture.

#### Remediation
Diferir. Per `security-build-vs-buy.md`: audit logging inicial = BUILD (coleção Mongo + logging estruturado), ativado em produção. Sentry free para error tracking quando houver deploy.

#### Remediation Route
- **Owner role:** Backend Architect (decisão de design de auditoria na transição para MVP/produção)

#### Decisão do usuário (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01.
- **Status resultante:** Pendência aberta (TODO) — **a remediar ANTES de qualquer deploy público**. Não implementado nesta feature; constituição inalterada (escopo do v1 mantido).
- **Reason:** Posture Prototype, sem usuários nem produção; observabilidade avançada fora do v1 per constituição § Constraints.
- **Revisit trigger:** Transição para MVP/produção ou primeiro deploy público.

---

### Finding 0007: Headers de segurança e CORS não configurados; sem TLS forçado

- **Severity:** LOW
- **OWASP Top 10:2025:** A05 — Security Misconfiguration
- **Framework citation(s):** WSTG-CONF-07 (HSTS), WSTG-CLNT, API Top 10 API8, ASVS V14 (Config), `security-conventions.md § Default Headers` / `§ CORS Default`
- **Affected component:** Configuração — sem `WebSecurityConfig`/filtro de headers; `pom.xml` sem `spring-boot-starter-security`
- **Status:** Open — Risco NÃO aceito (TODO: remediar antes de qualquer deploy público)
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
Não há HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy nem CSP; não há política de CORS explícita. A convenção pede esses headers para serviços public-facing. Como não há `spring-boot-starter-security`, os defaults de hardening do Spring Security também não estão presentes.

#### Impact
Para uma API pública, ausência de headers facilita clickjacking/MIME-sniffing e CORS aberto pode permitir acesso cross-origin indevido. Em Prototype (somente localhost, só API JSON, sem frontend — constituição § Non-Goals #6), o impacto efetivo é baixo.

#### Likelihood
Hard nesta posture (não exposto). Easy se houver deploy público sem reverse proxy adicionando os headers.

#### Remediation
Diferir, com nota: os headers podem ser entregues no edge (Cloudflare/reverse proxy) ou via `spring-boot-starter-security` com `HttpSecurity.headers(...)`. Per `§ Default Headers` (aplicar para serviço public-facing) e `§ CORS Default` (allowlist explícita, nunca `*`). Decisão build-vs-buy de edge na transição para deploy público.

#### Remediation Route
- **Owner role:** Backend Architect (decidir edge vs in-app na transição para deploy), depois Backend Engineer / DevOps para aplicar.

#### Decisão do usuário (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01.
- **Status resultante:** Pendência aberta (TODO) — **a remediar ANTES de qualquer deploy público**. Não implementado nesta feature; constituição inalterada (escopo do v1 mantido).
- **Reason:** Sem exposição pública, sem frontend, posture Prototype; hardening de borda explicitamente fora do v1.
- **Revisit trigger:** Primeiro deploy acessível fora de localhost.

---

### Finding 0008: Profile `prod` e credenciais de DB — gestão de segredos adequada para Prototype

- **Severity:** INFO
- **OWASP Top 10:2025:** A05 — Security Misconfiguration (verificado)
- **Framework citation(s):** WSTG-CONF, `security-conventions.md § Secrets Handling`, `security-build-vs-buy.md` (Secrets management)
- **Affected component:** `application.yml`, `.env.example`, `.gitignore`
- **Status:** Resolved (positivo — sem ação no Prototype)
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Description
`MONGODB_URI` vem de variável de ambiente; o profile `prod` exige a variável sem default (`${MONGODB_URI}`), evitando fallback inseguro. `.env` está no `.gitignore`, `.env.example` traz placeholder com comentário e nenhum segredo real está versionado. Isso satisfaz a convenção para Prototype (".env aceitável apenas para protótipo").

#### Evidence
`application.yml` profile `prod`: `uri: ${MONGODB_URI}` (sem default). `.gitignore` contém `.env`. `git ls-files` confirma que só `.env.example` e os `application.yml` estão versionados — nenhum segredo.

#### Remediation
Nenhuma agora. Per `security-build-vs-buy.md`: ao ir para MVP/produção, migrar segredos para Doppler/Infisical/AWS Secrets Manager (BUY). Registrar como revisit trigger.

#### Remediation Route
- **Owner role:** DevOps / Backend Architect (na transição para produção)

---

## Resumo

| Severidade | Qtd | Bloqueia em Prototype? |
|------------|-----|------------------------|
| CRITICAL | 0 | — |
| HIGH | 0 | — |
| MEDIUM | 0 | — |
| LOW | 5 (F-0001, F-0002, F-0003, F-0006, F-0007) | Não |
| INFO | 3 (F-0004, F-0005, F-0008) | Não |
| **Total** | **8** | **0 bloqueadores** |

**Achados bloqueadores em aberto:** Nenhum.

**Itens de risco — decisão do usuário (2026-06-01): NÃO aceitos.** F-0002, F-0003, F-0006, F-0007 ficam como **pendências abertas (TODO), a remediar antes de qualquer deploy público** (risco recusado, não diferido por aceite). F-0001 é correção barata recomendada mesmo em Prototype (não bloqueante).

### Veredito (posture Prototype)

**GO.**

Nenhum achado CRITICAL — o único limiar de bloqueio em Prototype. A implementação está limpa nos controles que importam para esta posture: validação de entrada tipada, sem superfície de injeção NoSQL, boundary DTO↔entidade correto (sem mass assignment de `id`/`dataCriacao`), tratamento de erro sem vazamento explícito de stack trace, e gestão de segredos via env adequada para protótipo. Os 5 achados LOW são, em sua maioria, hardening de produção explicitamente fora do escopo do v1 pela constituição e devem ser aceitos como risco diferido. Recomenda-se aplicar F-0001 (handler 500 de fallback + flags `server.error.*`) por ser barato e fechar o único vetor residual de inconsistência de erro.

**Condição não-bloqueante (atualizada):** o usuário **recusou** aceitar o risco de F-0002/F-0003/F-0006/F-0007 (2026-06-01). Em vez de aceite de risco, ficam como **pendências abertas obrigatórias antes de qualquer deploy público** — devem ser remediadas (não apenas reavaliadas) na transição para MVP/produção.

---

## Revisit triggers consolidados (na transição para MVP/produção)

1. **Deploy público / fora de localhost** → reabrir F-0002 (rate limiting, BUY edge), F-0007 (headers + CORS), F-0006 (logging/auditoria).
2. **Auth entra em escopo** (sai do Non-Goal #1) → novo threat model completo (S/E do STRIDE passam a aplicar); decisão build-vs-buy de Auth (default lean BUY para B2C, BUILD para interno/admin).
3. **Volume de tarefas cresce** → F-0003 (paginação, via PDR pelo Product Strategist).
4. **Produção real** → migrar segredos para vendor (F-0008), Dependabot/Snyk no repo, encryption at rest via MongoDB Atlas/KMS.
