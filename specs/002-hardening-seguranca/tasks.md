# todo-list-api — hardening-seguranca Tasks v1

- **Status:** Ready for /implement — **condicional**: fases RL e PG dependem de ADR-0003 e
  ADR-0004 (*Proposed*) aceitas. Ver § Gate de ADRs.
- **Date:** 2026-06-01
- **Plan:** [hardening-seguranca Plan v1](./plan.md)
- **Spec:** [hardening-seguranca Spec v1](./spec.md)
- **Baseline (binding):** `architecture-conventions.md`, `security-conventions.md`
- **Stack pin:** Spring Boot 3.5.x (ADR-0002).

## Como usar

- **Uma tarefa por vez** (`architecture-conventions.md § Engineering Workflow — one task at
  a time`), na ordem listada. Cada tarefa de código segue **TDD** (Red → Green → Suite →
  Refactor → Commit).
- Granularidade: ≈ 30–90 min (um ciclo red-green-refactor). Tarefas de build/config não têm
  ciclo de teste próprio.
- Cada commit cita a(s) seção(ões) de convenção/PDR no corpo e lista `Tests: N passed
  (./mvnw test)` (`§ Engineering Workflow — Conventional Commits`).
- Branch: `feat/002-hardening-seguranca` (`§ Repository Hygiene`).
- Testes de integração usam o `AbstractMongoIntegrationTest` existente (MockMvc + flapdoodle,
  full context — a cadeia de segurança é exercida nos ITs).

## Gate de ADRs (bloqueante)

- **T-RL-\*** (rate limiting) bloqueadas até **ADR-0003** = *Accepted*. Se rejeitada,
  substituir Bucket4j por filtro próprio (fallback da ADR) — sem mudar os ACs.
- **T-PG-\*** e **T-REG-\*** (paginação + regressão) bloqueadas até **ADR-0004** = *Accepted*.
- **T-HDR-\*** (headers/CORS) e **T-AUD-\*** (auditoria) **não** dependem de ADR — podem
  iniciar imediatamente.

---

## Fase A — Headers de segurança + CORS (F-0007 / PDR-0003) — sem ADR

### T-HDR-01 — Declarar `spring-boot-starter-security` no pom
- **O quê:** Adicionar `spring-boot-starter-security` ao `pom.xml` (linha compatível com
  Boot 3.5.x via parent).
- **Camada:** build
- **Convenção/PDR:** `§ Stack Baseline` (Security é baseline); PDR-0003.
- **Aceite:**
  - `./mvnw dependency:resolve` resolve sem erro.
  - **Atenção:** após adicionar o starter, todos os endpoints passam a exigir auth por
    default — a suíte existente vai quebrar até T-HDR-02. Esperado; T-HDR-02 corrige.
- **Depende de:** —

### T-HDR-02 — SecurityFilterChain: permitAll + CSRF off (preservar anônimo)
- **O quê:** Criar `config/SecurityConfig` com `SecurityFilterChain` que faz
  `authorizeHttpRequests().anyRequest().permitAll()`, desabilita CSRF, sem
  `formLogin`/`httpBasic`. Confirma Non-Goal #1 intacto.
- **Camada:** config
- **Convenção/PDR:** Constitution v2 § Non-Goals #1; PDR-0003; `§ Package Layout` (config/).
- **Aceite (TDD — AC4.6):**
  - **Red:** IT `GET /tarefas` anônimo espera **200**, não 401/403.
  - **Green:** filter chain `permitAll` faz o teste passar.
  - Toda a suíte 001 volta a passar (acesso anônimo restaurado).
- **Depende de:** T-HDR-01

### T-HDR-03 — Headers de segurança via HttpSecurity.headers(...)
- **O quê:** No `SecurityConfig`, configurar HSTS, `X-Content-Type-Options: nosniff`,
  `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`. **Sem** CSP
  nem Permissions-Policy (OQ-2 / Non-Goal #6).
- **Camada:** config
- **Convenção/PDR:** `security-conventions.md § Default Headers`; PDR-0003.
- **Aceite (TDD — AC4.1):**
  - **Red:** IT inspeciona resposta de `GET /tarefas` e espera os 4 headers presentes com os
    valores acima.
  - **Green:** config dos header writers passa o teste.
- **Depende de:** T-HDR-02

### T-HDR-04 — CORS por allowlist via env (nunca `*`)
- **O quê:** `CorsConfigurationSource` no `SecurityConfig` lendo a allowlist de origens de
  variável de ambiente (ex.: `CORS_ALLOWED_ORIGINS`), métodos explícitos
  (`GET`,`POST`,`PUT`,`DELETE`) e headers explícitos; **nunca `*`**. Adicionar a env ao
  `application.yml` (`${CORS_ALLOWED_ORIGINS:...}`) e ao `.env.example` com comentário.
- **Camada:** config
- **Convenção/PDR:** `security-conventions.md § CORS Default`; `§ CORS`;
  `§ Configuration & Secrets` (env + `.env.example`); PDR-0003.
- **Aceite (TDD — AC4.2/AC4.3/AC4.4/AC4.5):**
  - **Red:** IT preflight `OPTIONS` de origem **na** allowlist recebe
    `Access-Control-Allow-Origin` correspondente; origem **fora** não recebe; métodos/headers
    não-wildcard.
  - **Green:** config de CORS passa.
  - Asserção explícita de que o valor configurado nunca é `*`.
- **Depende de:** T-HDR-03

---

## Fase B — Logging / auditoria de mutações (F-0006 / PDR-0002) — sem ADR

### T-AUD-01 — Componente AuditoriaLogger (estruturado, sem PII)
- **O quê:** Criar componente (ex.: `service/AuditoriaLogger` ou `util/`) que emite um evento
  estruturado em **INFO** com `timestamp`, `operacao` (`create`/`update`/`delete`),
  `tarefaId`, `outcome`. **Não** recebe nem loga `titulo`/`descricao` nem corpo.
- **Camada:** service (ou util) — componente fino, injetável.
- **Convenção/PDR:** `§ Logging`; `security-conventions.md § Logging & Monitoring`; PDR-0002.
- **Aceite (TDD — AC3.4/AC3.5):**
  - **Red:** unit test (captura de log / appender) verifica que o evento contém os 4 campos
    e **não** contém `titulo`/`descricao`.
  - **Green:** componente passa.
- **Depende de:** —

### T-AUD-02 — Emitir auditoria no create (pós-save)
- **O quê:** Em `TarefaService.criar`, após o `save`, chamar o `AuditoriaLogger` com
  `operacao=create` e o `id` da tarefa criada.
- **Camada:** service
- **Convenção/PDR:** `§ Layer Rules` (service orquestra — id existe pós-save); PDR-0002.
- **Aceite (TDD — AC3.1):**
  - **Red:** IT/teste de service: `POST /tarefas` bem-sucedido emite **um** log `create` com
    o `id` da tarefa criada.
  - **Green:** chamada no service passa; nenhum log em caminho de erro.
- **Depende de:** T-AUD-01

### T-AUD-03 — Emitir auditoria no update e delete
- **O quê:** Em `TarefaService.atualizar` e `deletar`, após a operação bem-sucedida, emitir
  `operacao=update`/`delete` com o `id` afetado.
- **Camada:** service
- **Convenção/PDR:** `§ Layer Rules`; PDR-0002.
- **Aceite (TDD — AC3.2/AC3.3/AC3.6):**
  - **Red:** testes: `PUT` ok emite `update`; `DELETE` ok emite `delete`; `GET` **não** emite
    auditoria de mutação; update/delete em id inexistente (404) **não** emite log de sucesso.
  - **Green:** passa.
- **Depende de:** T-AUD-02

---

## Fase C — Rate limiting in-app (F-0002 / PDR-0001) — **requer ADR-0003 Accepted**

### T-RL-01 — Declarar Bucket4j no pom (gate: ADR-0003)
- **O quê:** Adicionar a dependência Bucket4j (core, in-memory) ao `pom.xml`, versão
  compatível com Boot 3.5.x. Só após ADR-0003 *Accepted* (senão, fallback filtro próprio —
  sem esta dependência).
- **Camada:** build
- **Convenção/PDR:** `§ Stack Baseline` (gate de tech nova → ADR-0003); PDR-0001.
- **Aceite:** `./mvnw dependency:resolve` resolve; ADR-0003 referenciada no commit.
- **Depende de:** **ADR-0003 (Accepted)**

### T-RL-02 — RateLimitConfig: buckets in-memory por chave (IP+endpoint)
- **O quê:** `config/RateLimitConfig` com `ConcurrentHashMap<String,Bucket>` (thread-safe) e
  fábrica de bucket 60 tokens / 60s (janela fixa). Chave = IP + método/endpoint de escrita
  (OQ-3: por endpoint).
- **Camada:** config
- **Convenção/PDR:** `security-conventions.md § Rate Limiting`; PDR-0001; ADR-0003 (chave,
  armazenamento, thread-safety).
- **Aceite (TDD — AC1.5):**
  - **Red:** unit test: dois IPs distintos têm buckets independentes; mesma chave reusa o
    mesmo bucket.
  - **Green:** passa.
- **Depende de:** T-RL-01

### T-RL-03 — RateLimitFilter só nos endpoints de escrita
- **O quê:** `OncePerRequestFilter` que consome 1 token por requisição de escrita
  (`POST`/`PUT`/`DELETE` de `/tarefas`); leitura passa sem consumir. Registrar na cadeia de
  forma que respostas carreguem os headers de segurança (ordem após o header writer —
  ver plan § Architecture Overview).
- **Camada:** config
- **Convenção/PDR:** PDR-0001 (só escrita); `§ Layer Rules`.
- **Aceite (TDD — AC1.1/AC1.4):**
  - **Red:** IT: até 60 escritas/IP/60s passam; `GET /tarefas` e `GET /tarefas/{id}` **nunca**
    são bloqueados.
  - **Green:** passa.
- **Depende de:** T-RL-02

### T-RL-04 — Resposta 429 + Retry-After no shape ErrorResponse
- **O quê:** Quando o bucket esgota, responder **429** com corpo `ErrorResponse`
  (timestamp/status=429/error/message/path=URI) e header **`Retry-After`** (segundos até o
  reset). Garantir que nenhuma mutação ocorra na requisição barrada.
- **Camada:** config (filtro) + dto (reuso de `ErrorResponse`)
- **Convenção/PDR:** `security-conventions.md § Error Responses`, `§ Rate Limiting`; OQ-1
  (Retry-After); PDR-0001.
- **Aceite (TDD — AC1.2/AC1.3):**
  - **Red:** IT: 61ª escrita/IP/60s → 429 com shape padrão + `Retry-After`; nenhuma tarefa
    criada/alterada por ela; após a janela (simulada/relógio controlável) o IP volta a ser
    atendido.
  - **Green:** passa. (Usar relógio injetável/`Clock` para testar reset sem `sleep` real.)
- **Depende de:** T-RL-03

---

## Fase D — Paginação no GET /tarefas (F-0003 / PDR-0004) — **requer ADR-0004 Accepted**

### T-PG-01 — DTO PageResponse<T> (envelope) (gate: ADR-0004)
- **O quê:** Criar `dto/PageResponse<T>` (record imutável) com `content`, `page`, `size`,
  `totalElements`, `totalPages` (ADR-0004).
- **Camada:** dto
- **Convenção/PDR:** `§ API Contracts`, `§ Layer Rules` (record, imutável); ADR-0004; PDR-0004.
- **Aceite (TDD):**
  - **Red:** unit test do record (campos, imutabilidade, serialização JSON com as chaves
    esperadas).
  - **Green:** passa.
- **Depende de:** **ADR-0004 (Accepted)**

### T-PG-02 — Mapper Page<Tarefa> → PageResponse<TarefaResponse>
- **O quê:** Em `TarefaMapper`, mapear `Page<Tarefa>` para `PageResponse<TarefaResponse>`
  (content via `toResponse`, metadados do `Page`).
- **Camada:** mapper
- **Convenção/PDR:** `§ Layer Rules` (mapper no boundary; nunca expor entidade/`Page`);
  ADR-0004.
- **Aceite (TDD):**
  - **Red:** unit test: `Page` com N itens → `PageResponse` com content mapeado e metadados
    corretos.
  - **Green:** passa.
- **Depende de:** T-PG-01

### T-PG-03 — Service listar(Pageable) com teto de size 100
- **O quê:** `TarefaService.listar(Pageable)` → `repository.findAll(pageable)` →
  `mapper` → `PageResponse`. Aplicar **teto de `size` = 100** (clamp) e defaults (page 0,
  size 20).
- **Camada:** service
- **Convenção/PDR:** PDR-0004; ADR-0004; `§ Layer Rules` (lógica no service).
- **Aceite (TDD — AC2.1/AC2.2/AC2.3/AC2.4):**
  - **Red:** testes de service: sem params → page 0 / size 20; `size=500` → size efetivo 100;
    coleção vazia → page vazia + `totalElements=0` (não erro).
  - **Green:** passa.
- **Depende de:** T-PG-02

### T-PG-04 — Controller GET /tarefas paginado + 400 para page/size inválidos
- **O quê:** Alterar `TarefaController.listar()` para receber `Pageable` (ou
  `page`/`size`) e retornar `PageResponse`. Garantir 400 (shape padrão) para `page`/`size`
  inválidos (negativos/não-numéricos), confirmando o mapeamento no `GlobalExceptionHandler`
  (estender se necessário para `MethodArgumentTypeMismatchException`/`ConstraintViolation`).
- **Camada:** controller (+ exception, se necessário)
- **Convenção/PDR:** `§ API Contracts`; `security-conventions.md § Error Responses`;
  PDR-0004; ADR-0004.
- **Aceite (TDD — AC2.1/AC2.2/AC2.5/AC2.6):**
  - **Red:** IT: `GET /tarefas` → 200 PageResponse; `?page=1&size=5` → metadados refletem;
    `?page=-1`/`?size=abc` → 400 no shape padrão com `message` indicando o parâmetro;
    ordenação estável entre páginas.
  - **Green:** passa.
- **Depende de:** T-PG-03

---

## Fase E — Regressão: atualizar testes da spec 001 afetados pela paginação

### T-REG-01 — Atualizar ITs de GET /tarefas da 001 (array → PageResponse)
- **O quê:** Atualizar os testes que assumem **array puro** em `GET /tarefas` para o novo
  contrato `PageResponse`. Alvos identificados:
  `src/test/java/com/matheusmelo/todolist/controller/TarefaControllerPostGetIT.java`
  — métodos `getListaVaziaRetorna200ArrayVazio` (`$` array → `$.content` vazio,
  `$.totalElements`=0) e `getListaRetornaTarefas` (`$[0]` → `$.content[0]`,
  `$.totalElements`/`$.totalPages`). Varrer o `src/test` por outras asserções
  `jsonPath("$")` / `$.length()` sobre `/tarefas` (não confundir com `GET /tarefas/{id}`,
  que continua objeto único e **não** muda).
- **Camada:** test
- **Convenção/PDR:** spec 002 AC5.1 (única quebra de contrato intencional);
  `§ Engineering Workflow` (suite verde); ADR-0004; PDR-0004.
- **Aceite:**
  - Os ITs de `GET /tarefas` afirmam o shape `PageResponse` e passam.
  - Os demais ACs da spec 001 (POST/PUT/DELETE/GET-por-id, erros 400/404) **continuam
    passando inalterados** (AC5.1: só o contrato da listagem muda).
  - `./mvnw test` verde em toda a suíte.
- **Depende de:** T-PG-04

---

## Fase F — Fechamento

### T-CLOSE-01 — Atualizar README (env de CORS, contrato paginado) e marcar findings
- **O quê:** Atualizar `README.md`: nova env `CORS_ALLOWED_ORIGINS` na tabela de env;
  contrato paginado do `GET /tarefas` na tabela de API; nota dos headers de segurança.
  Sinalizar (handoff ao Security Engineer) que F-0002/F-0003/F-0006/F-0007 estão prontas
  para re-revisão → **Resolved** em `docs/reviews/security-findings-001.md` (a marcação como
  Resolved é decisão do Security Engineer, não do Architect/Engineer).
- **Camada:** docs
- **Convenção/PDR:** `§ Documentation` (README com env table + API table); Constitution v2
  § Success Criteria #4.
- **Aceite:**
  - README reflete CORS env + contrato paginado + headers.
  - Handoff de re-revisão das 4 findings registrado (chat/PR), sem o Architect marcar
    Resolved por conta própria.
- **Depende de:** T-REG-01

---

## Reviewer / Security handoff (per § Engineering Workflow)

Ao concluir: anunciar branch `feat/002-hardening-seguranca`, faixa de commits, referências
(esta spec/plan, PDRs 0001–0004, ADR-0003/0004, findings F-0002/3/6/7), comando de teste
`./mvnw test`, e quaisquer labels *Pre-Convention* abertos. As 3 propostas de Convention
Drift do plano (shape paginado, padrão rate-limit in-app, schema de evento de auditoria)
ficam como follow-up de arquitetura para o usuário aprovar.
