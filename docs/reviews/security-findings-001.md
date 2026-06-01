# Findings Report — todo-list-api / feature 001-crud-tarefas

- **Data:** 2026-06-01
- **Revisor:** Security Engineer (revisão estática, sem build)
- **Projeto:** todo-list-api
- **Posture:** **Prototype** (per `memory/constitution.md` § Non-Goals + § Constraints — sem auth, sem multi-tenant, sem produção real, sem dados sensíveis)
- **Escopo revisado:** todos os fontes em `src/main/java/com/matheusmelo/todolist/` + `src/main/resources/application.yml` + `pom.xml` + `.env.example` + `.gitignore`
- **Branch:** `feat/001-crud-tarefas` (sem `main` para diff — revisão de toda a árvore de fontes)
- **Frameworks aplicados:** OWASP WSTG (INPV, ERRH, ATHN/ATHZ, BUSL, APIT, CRYP, CONF, INFO), OWASP Top 10:2025, OWASP API Security Top 10, STRIDE
- **Threat model prévio:** Nenhum (projeto de teste). STRIDE leve incluído abaixo.

> **Atualização 2026-06-01 (decisão do usuário):** Matheus Melo **NÃO aceitou** os riscos das findings LOW F-0002, F-0003, F-0006 e F-0007 — optou por remediá-las in-app (constituição emendada para v2; PDRs 0001–0004, ADRs 0003/0004 Accepted).
>
> **Re-revisão pós-remediação 2026-06-01 (esta atualização):** a feature `002-hardening-seguranca` foi implementada e verificada. **F-0002, F-0003, F-0006 e F-0007 → Resolved (2026-06-01)**, cada uma com riscos residuais explícitos. A verificação rodou `./mvnw test` (**117 testes, 0 falhas**). Um novo achado foi aberto pela remediação: **F-0009 (LOW)** — chave de rate limit por URI fragmenta a cota de PUT/DELETE. Veredito permanece **GO** em Prototype (nenhum bloqueador). Detalhes em cada finding, no Resumo e no Veredito ao final.

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
| **R**epudiation | Sem trilha de auditoria | **Mitigado (002)** — auditoria estruturada por mutação (`AuditoriaLogger`), sem PII. Resíduo: persistência efêmera (F-0006). |
| **I**nformation Disclosure | Vazar stack trace / internals em erro | **Mitigado** — `GlobalExceptionHandler` com fallback 500 genérico (F-0001 Resolved); 429 também usa shape padrão sem internals. |
| **D**enial of Service | Sem rate limiting; payload/coleção sem limite | **Mitigado (002)** — rate limit 60/min nas escritas (F-0002) + paginação com teto 100 (F-0003). Resíduos: IP/NAT, estado por instância, e fragmentação de cota por id em PUT/DELETE (**F-0009**). |
| **E**levation of Privilege | Sem papéis — não há privilégio a escalar | N/A nesta posture. |

> **STRIDE leve da re-revisão (2026-06-01) — vetores tocados pela remediação.** **Spoofing/DoS (rate limit por IP):** `getRemoteAddr()` é a fonte correta sem edge (não honra `X-Forwarded-For`, que seria forjável); o resíduo é NAT compartilhado e, no futuro com proxy, o IP precisar ser o real (PDR-0001). **DoS residual (F-0009):** chave por URI completa permite contornar o limite de PUT/DELETE variando o id. **Information Disclosure (429/CORS/log):** 429 no shape padrão sem internals; CORS por allowlist não reflete origens arbitrárias (não vaza `Access-Control-Allow-Origin` para origem não-permitida); auditoria não loga `titulo`/`descricao` (sem PII). **Tampering (CSRF):** CSRF desabilitado é aceitável — API stateless JSON sem cookie de sessão/auth (Non-Goal #1); reavaliar se sessões com cookie entrarem em escopo.

---

## Achados

---

### Finding 0001: Exceções não tratadas caem no error handler padrão do Spring (risco residual de vazamento de detalhes)

- **Severity:** LOW
- **OWASP Top 10:2025:** A05 — Security Misconfiguration / A04 — Insecure Design (parcial)
- **Framework citation(s):** WSTG-ERRH-01/02, API Top 10 API8 (Security Misconfiguration), ASVS V7.4
- **Affected component:** Backend — `exception/GlobalExceptionHandler.java` (cobre apenas `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `ResourceNotFoundException`)
- **Status:** Resolved (2026-06-01) — fallback `@ExceptionHandler(Exception.class)` (500, shape `ErrorResponse`, mensagem genérica, detalhe só em `log.error`) + flags `server.error.*` fixadas em `application.yml`; cobertura por `GlobalExceptionHandlerIT`. Commit `fcfae00`.
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
- **Status:** **Resolved (2026-06-01)** — rate limit in-app (Bucket4j) nos endpoints de escrita; ver Nota de remediação e Risco residual abaixo.
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Nota de remediação (2026-06-01)
Remediado in-app pela feature `002-hardening-seguranca` (PDR-0001, ADR-0003). `config/RateLimitFilter.java` consome 1 token por requisição de escrita (POST/PUT/DELETE de `/tarefas`); leitura passa sem consumir (AC1.4). Limite 60/janela de 60s por chave via `config/RateLimitConfig.java` (Bucket4j in-memory). Ao esgotar, `config/RateLimitResponseWriter.java` retorna **429 + Retry-After** no shape `ErrorResponse` (sem information disclosure, per § Error Responses). O filtro é adicionado **após** o `HeaderWriterFilter` na cadeia (`config/SecurityConfig.java`), de modo que respostas 429 carregam os headers de segurança. A requisição barrada **não** segue a cadeia (nenhuma mutação). Cobertura: `RateLimit429IT` (shape 429, Retry-After, headers na 429, não-mutação, reset de janela com relógio controlável), `RateLimitFilterIT` (escopo de escrita; GET nunca bloqueado). Commits `ed673a1`, `5bb2ade`, `db8bda0`, `932e818`. Suíte verde (117 testes).

#### Risco residual (aceito por design — não reabre a finding)
- **IP forjável / NAT compartilhado:** a chave usa `request.getRemoteAddr()` sem confiar em `X-Forwarded-For`. Sem edge/reverse-proxy isso é o correto (não há cabeçalho confiável a honrar), mas significa que (a) clientes atrás de NAT compartilham cota e (b) com edge no futuro, o `remoteAddr` será o do proxy — o limite por IP só é significativo quando o IP real chegar ao app (via cabeçalho confiável validado no edge). Documentado em PDR-0001 § Consequences. **Revisit trigger:** primeiro deploy atrás de edge/proxy → migrar limite para o edge e/ou honrar `X-Forwarded-For` apenas de proxies confiáveis.
- **Estado em memória por instância:** sem visão distribuída; múltiplas instâncias multiplicam a cota efetiva. Aceitável em Prototype/localhost (PDR-0001). **Revisit trigger:** mais de uma instância em produção → store distribuído (Redis) ou edge.
- **Fragmentação de bucket por id de path:** ver **F-0009** (novo) — a chave inclui a URI completa, então PUT/DELETE em ids distintos não compartilham cota. Registrado como finding própria.

#### Description
Nenhum endpoint tinha rate limiting. Um cliente podia criar tarefas em loop sem limite, inflando a coleção.

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

#### Decisão do usuário e desfecho (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01. Em vez de aceite, optou por **remediar in-app** (constituição emendada para v2; PDR-0001 Accepted).
- **Status resultante:** **Resolved (2026-06-01)** — remediado pela feature `002-hardening-seguranca` (ver Nota de remediação acima), com riscos residuais explícitos (IP/NAT, estado por instância, F-0009).
- **Revisit trigger:** Deploy atrás de edge/proxy ou múltiplas instâncias → migrar/reforçar limite no edge (revisit permanece válido como hardening futuro, não como pendência bloqueante).

---

### Finding 0003: `GET /tarefas` retorna a coleção inteira sem paginação nem limite

- **Severity:** LOW
- **OWASP Top 10:2025:** A04 — Insecure Design
- **Framework citation(s):** WSTG-BUSL, API Top 10 API4 (Unrestricted Resource Consumption)
- **Affected component:** Backend — `service/TarefaService.java` `listar(Pageable)`, `controller/TarefaController.java`, `dto/PageResponse.java`
- **Status:** **Resolved (2026-06-01)** — paginação com defaults e teto; ver Nota de remediação e Risco residual abaixo.
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Nota de remediação (2026-06-01)
Remediado pela feature `002-hardening-seguranca` (PDR-0004, ADR-0004). `GET /tarefas` deixou de retornar a coleção inteira: `TarefaController.listar` aceita `page` (`@Min(0)`) e `size` (`@Min(1)`); `TarefaService.normalizar` aplica defaults (page 0, size 20) e **clamp de size em 100** (`SIZE_MAXIMO`), impedindo o cliente de forçar materialização ilimitada. Resposta no envelope `PageResponse` (não vaza `org.springframework.data.domain.Page` na borda). `page`/`size` inválidos → **400** no shape padrão (`GlobalExceptionHandler` para `ConstraintViolationException` e `MethodArgumentTypeMismatchException`). O método `listar()` não-paginado morto foi removido (commit `e8931a1`). Cobertura: `TarefaServiceListarPaginadoTest` (clamp 500→100, defaults, página vazia), `TarefaControllerPaginacaoIT` (contrato `PageResponse`, 400 para page=-1/size=0/size=abc, estabilidade entre páginas). Commits `b0e01d8`..`e8931a1`. Suíte verde.

#### Risco residual (nota, não reabre a finding)
- **Ordenação não-determinística:** `repository.findAll(Pageable)` sem `Sort` explícito depende da ordem natural do MongoDB. O teste `paginacaoEstavelEntrePaginas` passa, mas a estabilidade é incidental (ordem de inserção), não garantida após updates/compactação. Em Prototype é aceitável (INFO). **Revisit trigger:** ao expor filtros/ordenação (Non-Goal remanescente) ou em produção, fixar um `Sort` estável (ex.: por `dataCriacao` + `id`) — roteado ao Backend Engineer quando o item entrar em escopo.

#### Description
`listar()` fazia `findAll()` e materializava toda a coleção em memória. Com a coleção grande (ver F-0002), isso virava um vetor de consumo de memória/latência.

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

#### Decisão do usuário e desfecho (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01. Optou por **remediar in-app** (PDR-0004, ADR-0004; constituição v2 incorpora paginação básica).
- **Status resultante:** **Resolved (2026-06-01)** — paginação com teto de size implementada (ver Nota de remediação acima).
- **Revisit trigger:** Filtros/ordenação avançada (continuam fora de escopo) e ordenação estável explícita em produção.

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
- **Affected component:** Backend — `service/AuditoriaLogger.java`, `service/TarefaService.java`
- **Status:** **Resolved (2026-06-01)** — auditoria estruturada por mutação, sem PII; ver Nota de remediação e Risco residual abaixo.
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Nota de remediação (2026-06-01)
Remediado pela feature `002-hardening-seguranca` (PDR-0002). `service/AuditoriaLogger.java` emite um evento estruturado **INFO** por mutação bem-sucedida com exatamente 4 campos canônicos: `operacao` (create|update|delete), `tarefaId` (id pós-save), `outcome=sucesso`, `timestamp`. O `TarefaService` chama `registrar(...)` **após** `save`/`deleteById` — mutação em id inexistente (404) **não** emite evento. Verificação de não-vazamento: `AuditoriaLoggerTest.naoVazaTituloNemDescricaoNoEvento` assegura que `titulo`/`descricao` (os únicos campos de texto livre do usuário) nunca entram no log, alinhado a § Logging & Monitoring (never log full request bodies / PII at INFO). Cobertura: `AuditoriaLoggerTest` (4 campos, sem PII, delete), `TarefaServiceAuditoriaTest` (um evento por mutação, leitura não audita, 404 não audita). Commits `d5a97b3`, `6bb57f6`, `9d97b9f`. Suíte verde.

#### Risco residual (aceito por design — não reabre a finding)
- **Persistência efêmera:** auditoria vai para o log da aplicação (stdout/SLF4J), não para uma coleção/observability platform durável. Sem retenção nem correlação por request-id, e sem trilha de leituras/acessos (apenas mutações). Aceitável em Prototype (PDR-0002; sem usuários reais). **Revisit trigger:** transição para MVP/produção → coleção de auditoria dedicada + envio a plataforma de observabilidade (Sentry/APM), retenção e request-id, per § Logging & Monitoring.

#### Description
Não havia logging de eventos de segurança nem trilha de auditoria de mutações (create/update/delete). O único logging era `log.warn` no `GlobalExceptionHandler`. Sem auth, não há eventos de autenticação/autorização a logar.

#### Impact
Sem capacidade de investigar abuso/incidentes. Em Prototype sem usuários, irrelevante na prática.

#### Likelihood
N/A nesta posture.

#### Remediation
Diferir. Per `security-build-vs-buy.md`: audit logging inicial = BUILD (coleção Mongo + logging estruturado), ativado em produção. Sentry free para error tracking quando houver deploy.

#### Remediation Route
- **Owner role:** Backend Architect (decisão de design de auditoria na transição para MVP/produção)

#### Decisão do usuário e desfecho (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01. Optou por **remediar in-app** (PDR-0002).
- **Status resultante:** **Resolved (2026-06-01)** — auditoria estruturada de mutações sem PII (ver Nota de remediação acima).
- **Revisit trigger:** Persistência/retenção durável e observabilidade avançada na transição para MVP/produção.

---

### Finding 0007: Headers de segurança e CORS não configurados; sem TLS forçado

- **Severity:** LOW
- **OWASP Top 10:2025:** A05 — Security Misconfiguration
- **Framework citation(s):** WSTG-CONF-07 (HSTS), WSTG-CLNT, API Top 10 API8, ASVS V14 (Config), `security-conventions.md § Default Headers` / `§ CORS Default`
- **Affected component:** `config/SecurityConfig.java`, `application.yml`, `.env.example`
- **Status:** **Resolved (2026-06-01)** — headers de segurança + CORS por allowlist; ver Nota de remediação e Risco residual abaixo.
- **Posture:** Prototype
- **Blocking at this posture:** No

#### Nota de remediação (2026-06-01)
Remediado pela feature `002-hardening-seguranca` (PDR-0003). `config/SecurityConfig.java` introduz o Spring Security **apenas** para headers + CORS (`anyRequest().permitAll()` — Non-Goal #1 intacto, AC4.6). Headers aplicados a toda resposta: `Strict-Transport-Security` (max-age 31536000, includeSubDomains, preload, via `requestMatcher` que força o header também fora de HTTPS local), `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`. CORS por **allowlist explícita via env** (`cors.allowed-origins` / `CORS_ALLOWED_ORIGINS`), métodos e headers explícitos, **nunca `*`** (per § CORS Default); origem fora da allowlist não recebe `Access-Control-Allow-Origin`. `.env.example` documenta a env com aviso "NUNCA use '*'". Cobertura: `SecurityHeadersIT` (HSTS/nosniff/DENY/Referrer presentes; CSP e Permissions-Policy ausentes por OQ-2/Non-Goal #6), `CorsConfigIT` (allowlist atende origem permitida, rejeita não-permitida, nunca wildcard, métodos explícitos), `SecurityConfigIT` (permanece anônimo, sem 401/403), `RateLimit429IT` (429 carrega os headers). Commits `6e4a680`, `ddb5d76`. Suíte verde.

#### Risco residual (nota, não reabre a finding)
- **CSP e Permissions-Policy ausentes (por decisão):** OQ-2/Non-Goal #6 — API JSON sem frontend servido; CSP só protege documentos HTML. Decisão correta para o escopo atual. **Revisit trigger:** se a app passar a servir HTML/frontend, adicionar CSP estrita (nonce-based) e Permissions-Policy, per § Default Headers.
- **HSTS depende de TLS real:** o header é emitido mas só tem efeito sob HTTPS; em localhost é inócuo. Em produção o TLS precisa terminar no edge/servidor — confirmar na transição.

#### Description
Não havia HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy nem política de CORS explícita. A convenção pede esses headers para serviços public-facing.

#### Impact
Para uma API pública, ausência de headers facilita clickjacking/MIME-sniffing e CORS aberto pode permitir acesso cross-origin indevido. Em Prototype (somente localhost, só API JSON, sem frontend — constituição § Non-Goals #6), o impacto efetivo é baixo.

#### Likelihood
Hard nesta posture (não exposto). Easy se houver deploy público sem reverse proxy adicionando os headers.

#### Remediation
Diferir, com nota: os headers podem ser entregues no edge (Cloudflare/reverse proxy) ou via `spring-boot-starter-security` com `HttpSecurity.headers(...)`. Per `§ Default Headers` (aplicar para serviço public-facing) e `§ CORS Default` (allowlist explícita, nunca `*`). Decisão build-vs-buy de edge na transição para deploy público.

#### Remediation Route
- **Owner role:** Backend Architect (decidir edge vs in-app na transição para deploy), depois Backend Engineer / DevOps para aplicar.

#### Decisão do usuário e desfecho (2026-06-01)
- **Risco aceito?** **NÃO** — Matheus Melo recusou aceitar o risco em 2026-06-01. Optou por **remediar in-app** (PDR-0003).
- **Status resultante:** **Resolved (2026-06-01)** — headers de segurança + CORS por allowlist implementados (ver Nota de remediação acima).
- **Revisit trigger:** Servir frontend/HTML → adicionar CSP/Permissions-Policy; garantir TLS no edge em produção.

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

### Finding 0009: Chave de rate limit inclui o id de path — fragmenta a cota de PUT/DELETE (desvio do intento da spec)

- **Severity:** LOW
- **OWASP Top 10:2025:** A04 — Insecure Design
- **Framework citation(s):** WSTG-BUSL, API Top 10 API4 (Unrestricted Resource Consumption), ASVS V11.x (anti-automation)
- **Affected component:** Backend — `config/RateLimitFilter.java` método `chave(...)` (`request.getRemoteAddr() + "|" + getMethod() + "|" + getRequestURI()`)
- **Status:** Resolved (2026-06-01) — `RateLimitFilter.chave()` agora deriva a **rota lógica** normalizando `/tarefas/{qualquer-id}` → `/tarefas/{id}` (regex no path), de modo que PUT/DELETE em ids distintos compartilham a cota (IP + verbo + rota lógica); POST `/tarefas` (URI fixa) inalterado. Limite/janela (60/60s) inalterados — só a granularidade da chave mudou. Cobertura: `RateLimitRotaLogicaIT` (DELETE e PUT em ids distintos → 429 na 61ª, demonstrando que a cota é agregada por rota). Suíte verde (**119 testes**). Commit `499a590`. Alinha o código ao AC1.1 da spec `002-hardening-seguranca`.
- **Posture:** Prototype
- **Blocking at this posture:** No (Prototype bloqueia só CRITICAL) — corrigir antes de deploy público (ver Revisit de F-0002)
- **Introduzida por:** remediação da feature `002-hardening-seguranca` (referencia F-0002)

#### Description
A chave do bucket é `IP | método | URI`, e `getRequestURI()` inclui o id de path. Para `POST /tarefas` (URI fixa) o limite funciona como esperado. Mas `PUT /tarefas/{id}` e `DELETE /tarefas/{id}` produzem uma URI distinta por id, logo **cada id recebe seu próprio bucket de 60**. Um atacante que itere sobre ids distintos (ou os crie e os apague) nunca atinge o limite de DELETE/PUT — o teto efetivo de escrita por IP deixa de ser 60/min. Isso diverge do intento documentado na spec (OQ-3 / AC1.1: "60/min por IP **por endpoint de escrita**", i.e. por método/rota lógica, não por instância de recurso).

#### Evidence
`RateLimitFilter.chave()` usa `request.getRequestURI()`. Os ITs só exercitam `POST /tarefas` (URI constante) e GET; nenhum teste cobre PUT/DELETE em ids distintos contra o limite — a fragmentação passa despercebida e está viva (suíte verde não a captura).

#### Impact
Bypass parcial do controle anti-abuso para os verbos PUT/DELETE: exaustão de recursos / churn ilimitado de mutações por IP. Em Prototype/localhost o impacto é teórico; vira real em deploy público.

#### Likelihood
Easy (basta variar o id na URL). Mitigado na prática hoje por não haver exposição pública.

#### Remediation
Derivar a chave da **rota lógica** (template do endpoint) em vez da URI concreta — ex.: chave `IP | método | "/tarefas"` para os três verbos de escrita, ou normalizar substituindo o segmento de id por um placeholder (`/tarefas/{id}`). Per § Rate Limiting (limite por IP significativo por endpoint) e alinha o código ao AC1.1 da spec 002. Adicionar IT cobrindo PUT/DELETE em ids distintos compartilhando a cota.

#### Remediation Route
- **Owner role:** Backend Engineer
- **Reason routed there:** correção de implementação localizada no filtro (cálculo da chave) + teste; padrão já estabelecido na feature 002. Decisão de design já existe (spec AC1.1 / PDR-0001); não requer Architect.

---

## Resumo

| Severidade | Qtd | Resolvidas | Em aberto | Bloqueia em Prototype? |
|------------|-----|------------|-----------|------------------------|
| CRITICAL | 0 | 0 | 0 | — |
| HIGH | 0 | 0 | 0 | — |
| MEDIUM | 0 | 0 | 0 | — |
| LOW | 6 (F-0001, F-0002, F-0003, F-0006, F-0007, F-0009) | 6 (F-0001/02/03/06/07/09) | 0 | Não |
| INFO | 3 (F-0004, F-0005, F-0008) | 3 | 0 | Não |
| **Total** | **9** | **9** | **0** | **0 bloqueadores** |

**Achados bloqueadores em aberto:** Nenhum. **F-0009 → Resolved (2026-06-01)**, commit `499a590` (chave de rate limit por rota lógica; suíte 119 testes verde).

**Estado pós-remediação (2026-06-01):** F-0002, F-0003, F-0006 e F-0007 — que o usuário recusou aceitar como risco — foram **remediadas in-app** pela feature `002-hardening-seguranca` (constituição emendada para v2; PDRs 0001–0004 e ADRs 0003/0004 Accepted) e estão **Resolved (2026-06-01)**, cada uma com riscos residuais explícitos registrados na própria finding (IP/NAT e estado por instância no rate limit; ordenação não-determinística na paginação; persistência efêmera na auditoria; ausência intencional de CSP). F-0001 permanece Resolved. A re-revisão descobriu **um novo achado (F-0009, LOW)** introduzido pela remediação do rate limit: a chave de bucket inclui o id de path, fragmentando a cota de PUT/DELETE.

### Re-revisão — verificação de testes (2026-06-01)
Suíte executada localmente: **`./mvnw test` → 117 testes, 0 falhas, 0 erros, 0 skipped.** Cobertura confirmada por finding: rate limit (`RateLimit429IT`, `RateLimitFilterIT`, `RateLimitConfigTest`), headers/CORS (`SecurityHeadersIT`, `CorsConfigIT`, `SecurityConfigIT`), auditoria (`AuditoriaLoggerTest`, `TarefaServiceAuditoriaTest`), paginação (`TarefaServiceListarPaginadoTest`, `TarefaControllerPaginacaoIT`).

### Veredito (posture Prototype)

**GO.**

Nenhum achado CRITICAL — o único limiar de bloqueio em Prototype. As quatro remediações estão adequadas à posture e às convenções: 429 + Retry-After no shape padrão com headers de segurança também nas respostas barradas; CORS por allowlist via env, nunca `*`; HSTS/nosniff/DENY/Referrer presentes; auditoria estruturada sem vazar `titulo`/`descricao`; paginação com teto de size 100 e 400 para parâmetros inválidos. Os controles de base do v1 permanecem limpos (validação tipada, sem injeção NoSQL, sem mass assignment, erro sem internals com fallback 500, segredos via env). 

**Condição não-bloqueante:** ~~corrigir **F-0009** (chave de rate limit por rota lógica em vez de URI com id) antes de qualquer deploy público~~ — **Resolved (2026-06-01)**, commit `499a590`: a chave passou a ser derivada da rota lógica, fechando o bypass parcial do limite de PUT/DELETE e alinhando o código ao AC1.1 da spec 002. Os riscos residuais por design (IP/NAT, estado por instância, persistência efêmera de auditoria, CSP ausente) seguem como revisit triggers válidos na transição para MVP/produção, não como pendências bloqueantes em Prototype.

---

## Revisit triggers consolidados (na transição para MVP/produção)

> F-0002/0003/0006/0007 estão **Resolved** in-app; os triggers abaixo são para **reforçar/migrar** a remediação na transição de posture, não para reabrir as findings (exceto se a regressão for detectada).

1. ~~**F-0009 (Open, LOW)** — corrigir a chave do rate limit (por rota lógica, não URI com id) **antes de deploy público**. Owner: Backend Engineer.~~ **Resolved (2026-06-01)**, commit `499a590` — chave por rota lógica; cobertura `RateLimitRotaLogicaIT`.
2. **Deploy atrás de edge/proxy** → migrar/reforçar rate limit no edge e honrar `X-Forwarded-For` apenas de proxies confiáveis (resíduo de F-0002); garantir TLS real para o HSTS ter efeito (F-0007).
3. **Múltiplas instâncias** → rate limit distribuído (Redis) — estado in-memory por instância (resíduo de F-0002).
4. **Servir frontend/HTML** → adicionar CSP estrita + Permissions-Policy (resíduo de F-0007).
5. **MVP/produção** → auditoria durável (coleção dedicada + observability + request-id), retenção (resíduo de F-0006); `Sort` estável explícito na paginação (resíduo de F-0003).
6. **Auth entra em escopo** (sai do Non-Goal #1) → novo threat model completo (S/E do STRIDE passam a aplicar; CSRF reavaliado se houver cookie de sessão); decisão build-vs-buy de Auth.
7. **Produção real** → migrar segredos para vendor (F-0008), Dependabot/Snyk no repo, encryption at rest via MongoDB Atlas/KMS.
