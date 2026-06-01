# todo-list-api — hardening-seguranca Plan v1

- **Status:** Ready for /tasks — **condicional**: depende da aceitação de ADR-0003 e
  ADR-0004 (ambos *Proposed*). Ver § Deviations.
- **Date:** 2026-06-01
- **Confidence:** **Full** — entrada é spec formal (Status: Ready for /plan) ancorada em
  Constitution v2 (Accepted) e PDRs 0001–0004 (Accepted).
- **Spec:** [hardening-seguranca Spec v1](./spec.md)
- **Constitution:** [Constitution v2](../../memory/constitution.md)
- **Baseline (binding):** `architecture-conventions.md`, `security-conventions.md`
- **Stack pin:** Spring Boot 3.5.x (ADR-0002) — não Boot 4.x.

## Source

- **Spec:** `specs/002-hardening-seguranca/spec.md` (formal `/specify`, Ready for /plan).
- **PDRs (gate de escopo, Accepted):** `docs/pdr/0001`..`0004`.
- **Findings remediadas:** F-0002, F-0003, F-0006, F-0007 (`docs/reviews/security-findings-001.md`).
- **Spec/código existente afetado:** `specs/001-crud-tarefas/spec.md` (AC2.x) e
  `src/main/java/com/matheusmelo/todolist/`.

### Open Clarifications da spec — adotadas como decididas neste plano

As 3 Open Clarifications da spec têm default proposto; **adoto os defaults inline** (não
bloqueiam o `/plan`), conforme instrução do usuário:

- **OQ-1 (Retry-After no 429):** **Adotado — incluir** o header `Retry-After` na resposta
  429 (segundos até o reset da janela). Boa prática, baixo custo. Refletido em AC1.x e na
  tarefa T-RL-04.
- **OQ-2 (CSP / Permissions-Policy):** **Adotado — fora de escopo.** API sem frontend
  (Constitution v2 § Non-Goals #6). Apenas os headers de AC4.1 entram. Registrado como
  revisit se um frontend entrar em escopo.
- **OQ-3 (granularidade do rate limit):** **Adotado — por endpoint de escrita** (contadores
  separados por `POST`/`PUT`/`DELETE`), o default da spec: mais permissivo e mais simples de
  raciocinar. Refletido em ADR-0003 e AC1.x.

## Architecture Overview

Endurecimento transversal **in-app** da API CRUD existente, sem introduzir auth (Non-Goal
#1 intacto). Quatro controles, todos sobre a app single-module Spring Boot 3.5.x:

```
Request HTTP
   │
   ▼
[ SecurityFilterChain (Spring Security) ]      ── F-0007 / PDR-0003 / ADR n/a (starter é baseline)
   │  • aplica headers de segurança (HSTS, nosniff, DENY, Referrer-Policy)
   │  • aplica CORS por allowlist (env, nunca *)
   │  • permitAll em todos os endpoints (anônimo — confirma Non-Goal #1)
   ▼
[ RateLimitFilter (Bucket4j in-memory) ]       ── F-0002 / PDR-0001 / ADR-0003 (Proposed)
   │  • só endpoints de escrita (POST/PUT/DELETE /tarefas)
   │  • 60 req / IP / 60s por endpoint; excedeu → 429 + Retry-After (shape ErrorResponse)
   ▼
[ TarefaController ]  → [ TarefaService ]  → [ TarefaRepository / MongoDB ]
                              │
                              ├─ paginação: findAll(Pageable) → Page → PageResponse
                              │              ── F-0003 / PDR-0004 / ADR-0004 (Proposed)
                              └─ auditoria: log estruturado INFO por mutação bem-sucedida
                                             ── F-0006 / PDR-0002 / sem ADR (alinhado a § Logging)
```

**Pontos de integração:** ordem dos filtros importa — o `SecurityFilterChain` (headers/CORS)
roda antes; o `RateLimitFilter` deve rodar dentro/depois da cadeia de segurança para que
respostas 429 também carreguem os headers (AC4.1 vale para "toda resposta"). Registrar o
`RateLimitFilter` na cadeia do Spring Security (ou via `FilterRegistrationBean` com ordem
explícita posterior ao header writer).

**Camadas tocadas** (per `architecture-conventions.md § Package Layout`):
- `config/` — `SecurityConfig` (SecurityFilterChain headers+CORS), `RateLimitConfig`
  (beans de Bucket4j / registro do filtro), props de CORS/limite.
- `controller/` — `TarefaController.listar()` muda assinatura para `Pageable`/retorno
  paginado.
- `service/` — `TarefaService.listar(Pageable)`; emissão de log de auditoria nas mutações.
- `dto/` — novo `PageResponse<T>` (ADR-0004).
- `mapper/` — mapeamento `Page<Tarefa>` → `PageResponse<TarefaResponse>`.
- `util/` (ou `service/`) — `AuditoriaLogger` (componente de log estruturado; opção
  service-level, ver decisão abaixo).
- `exception/GlobalExceptionHandler` — 400 de paginação inválida já é coberto por
  `MethodArgumentTypeMismatchException`/binding; confirmar/estender (AC2.5).

## Itens de remediação — decisões e ancoragem

### 1. Rate limiting in-app — F-0002 / PDR-0001 / ADR-0003 (Proposed)

- **Biblioteca:** **Bucket4j**, armazenamento in-memory (`ConcurrentHashMap<String,Bucket>`,
  thread-safe). Decisão de tecnologia nova → **ADR-0003** (gate de
  `architecture-conventions.md § Stack Baseline`). Se ADR-0003 for rejeitada, fallback =
  filtro próprio (registrado na ADR).
- **Onde aplicar:** `OncePerRequestFilter` restrito aos endpoints de escrita
  (`POST`/`PUT`/`DELETE` de `/tarefas`); leitura não limitada (PDR-0001; AC1.4).
- **Limite:** 60 req / IP / janela fixa 60s, **por endpoint** (OQ-3 default; AC1.1/AC1.5).
- **Chave:** IP de origem + método/endpoint. Contadores independentes por IP (AC1.5) e por
  endpoint (OQ-3).
- **Resposta ao exceder:** 429 no shape `ErrorResponse` (status=429, path = URI) +
  `Retry-After` (OQ-1) — per `security-conventions.md § Error Responses` (sem information
  disclosure) e § Rate Limiting. Nada é persistido pela requisição barrada (AC1.2).
- **Thread-safety / armazenamento:** Bucket4j é thread-safe; mapa concorrente por chave;
  estado por instância (aceitável Prototype/localhost — PDR-0001 § Consequences).

### 2. Logging / auditoria in-app — F-0006 / PDR-0002 (sem ADR — alinhado a convenção)

- **O quê:** um evento de auditoria **estruturado** em nível **INFO** por mutação
  bem-sucedida (`create`/`update`/`delete`). Campos mínimos: `timestamp`, `operacao`,
  `tarefaId`, `outcome` (AC3.4).
- **Sem dados sensíveis:** **não** logar `titulo`/`descricao` nem corpo da req/resp (AC3.5)
  — per `security-conventions.md § Logging & Monitoring` ("never log full request bodies")
  e `architecture-conventions.md § Logging` ("Never log secrets, tokens, PII").
- **Onde interceptar — decisão: na camada de service** (não aspecto). Justificativa: o
  `id` da tarefa criada (AC3.1) só existe **após** o `save`, e "mutação bem-sucedida"
  (AC3.x) é precisamente o ponto pós-persistência no service — o service é a única camada
  que orquestra (per `§ Layer Rules`). Um `@Aspect` em torno do controller não tem o `id`
  pós-save de forma limpa e exigiria pointcuts frágeis. Encapsular a emissão num componente
  `AuditoriaLogger` (SLF4J, formato estruturado via MDC/`StructuredArguments` ou JSON)
  injetado no service mantém o service fino e o formato testável.
- **Formato estruturado:** SLF4J + Logback (já no stack, `§ Stack Baseline`). Em localhost,
  formato legível; campos como key-value estruturados (MDC ou marcadores) para satisfazer
  "estruturado" (AC3.4) sem depender de agregador (vendor fora de escopo — PDR-0002).
- **Leitura não audita** (AC3.6).

### 3. Headers de segurança + CORS — F-0007 / PDR-0003 (sem ADR — starter é baseline)

- **Dependência:** adicionar `spring-boot-starter-security`. **Não exige ADR** — Spring
  Security já consta em `architecture-conventions.md § Stack Baseline`; o uso restrito a
  headers+CORS está gated por PDR-0003 (Accepted).
- **Headers (AC4.1):** via `HttpSecurity.headers(...)` — `Strict-Transport-Security`
  (HSTS), `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy:
  strict-origin-when-cross-origin` — per `security-conventions.md § Default Headers`.
  **CSP e Permissions-Policy fora** (OQ-2 / Non-Goal #6).
- **CORS (AC4.2–AC4.5):** allowlist explícita de origens via **variável de ambiente**,
  **nunca `*`**; métodos explícitos (`GET`,`POST`,`PUT`,`DELETE`) e headers explícitos
  (sem wildcard) — per `security-conventions.md § CORS Default` e
  `architecture-conventions.md § CORS`. Origem fora da allowlist não recebe
  `Access-Control-Allow-Origin` (AC4.4).
- **Anônimo preservado (AC4.6 / Non-Goal #1):** `SecurityFilterChain` com
  `authorizeHttpRequests(...).anyRequest().permitAll()`; CSRF desabilitado (API stateless
  sem sessão/cookie de auth); sem `formLogin`/`httpBasic`. Teste confirma `GET /tarefas`
  anônimo → 200, nunca 401/403.

### 4. Paginação — F-0003 / PDR-0004 / ADR-0004 (Proposed)

- **Mecanismo:** Spring Data `Pageable` no `GET /tarefas`; `repository.findAll(Pageable)`.
- **Parâmetros:** `page` default 0, `size` default 20, **`size` máximo 100** (teto aplicado
  no service/config — AC2.3). `page`/`size` inválidos → 400 no shape padrão (AC2.5).
- **Formato da resposta — decisão: DTO envelope próprio `PageResponse<T>`** (ADR-0004,
  Proposed), **não** serializar `Page` diretamente. Quebra de contrato vs spec 001 (array →
  objeto) documentada em AC5.1 e ADR-0004. Campos: `content`, `page`, `size`,
  `totalElements`, `totalPages` (AC2.1).
- **Ordenação default da plataforma** (estável entre páginas — AC2.6); **sem** ordenação
  customizada/filtros (fora de escopo — PDR-0004).

## Convention Alignment

| Decisão | Convenção / PDR / Constituição |
|---------|--------------------------------|
| Rate limit in-app só na escrita, 429 + Retry-After | `security-conventions.md § Rate Limiting`, `§ Error Responses`; PDR-0001; OQ-1 |
| 429 no shape padrão (timestamp/status/error/message/path) | `security-conventions.md § Error Responses`; `architecture-conventions.md § API Contracts` |
| Bucket4j (tecnologia nova) gated por ADR | `architecture-conventions.md § Stack Baseline` ("no new core tech without ADR") → **ADR-0003** |
| Auditoria estruturada INFO, sem corpo/PII | `architecture-conventions.md § Logging`; `security-conventions.md § Logging & Monitoring`; PDR-0002 |
| Auditoria na camada de service (não aspecto) | `architecture-conventions.md § Layer Rules` (service orquestra) |
| `spring-boot-starter-security` p/ headers+CORS | `architecture-conventions.md § Stack Baseline` (Security é baseline); PDR-0003 |
| Headers de segurança | `security-conventions.md § Default Headers` (CSP/Permissions fora — OQ-2/Non-Goal #6) |
| CORS allowlist via env, nunca `*`, métodos/headers explícitos | `security-conventions.md § CORS Default`; `architecture-conventions.md § CORS`; PDR-0003 |
| Anônimo preservado (`permitAll`) | Constitution v2 § Non-Goals #1; PDR-0003; AC4.6 |
| Paginação via `Pageable`, `size` teto 100 | `architecture-conventions.md § API Contracts`; PDR-0004 |
| Envelope `PageResponse` (não expor `Page`/entidade) | `architecture-conventions.md § Layer Rules`, `§ API Contracts` → **ADR-0004** (lacuna de convenção) |
| DTO imutável (record), mapper no boundary | `architecture-conventions.md § Layer Rules` (records preferidos; mapper sempre) |
| Erro de paginação inválida 400 no shape padrão | `security-conventions.md § Error Responses`; spec 001 contrato de erro |
| Spring Boot 3.5.x (não 4.x) | ADR-0002 (escopo deste projeto) |

## Deviations

Duas decisões divergem/preenchem lacuna de convenção e foram levantadas como ADR
**antes** de finalizar (Deviation Gate). **Este plano é condicional à aceitação delas.**

1. **ADR-0003 — Bucket4j para rate limiting in-app.** *Status: Proposed.* Desvio de
   `§ Stack Baseline` (tecnologia nova). Tarefas T-RL-* bloqueadas até *Accepted*; fallback
   = filtro próprio se rejeitada.
2. **ADR-0004 — Formato da resposta paginada (`PageResponse` envelope).** *Status:
   Proposed.* Preenche lacuna (não há shape paginado na convenção) e diverge do default de
   serializar `Page`. Tarefas T-PG-* bloqueadas até *Accepted*.

Decisões **sem ADR** (alinhadas / já gated por PDR): adicionar `spring-boot-starter-security`
(baseline + PDR-0003), auditoria estruturada (§ Logging + PDR-0002), paginação via
`Pageable` (PDR-0004). 

> **Nota para o Security Engineer (build-vs-buy):** as escolhas BUILD in-app de rate
> limiting e logging/auditoria já constam dos PDRs 0001/0002 como BUILD por inviabilidade de
> edge/vendor (localhost). ADR-0003 formaliza a build-vs-buy de **biblioteca** (Bucket4j vs
> filtro próprio). Recomenda-se um threat model STRIDE leve da feature (per
> `security-conventions.md § Threat Model Template`) cobrindo DoS (rate limit por IP — NAT/IP
> compartilhado), Information Disclosure (não vazar campos no log de auditoria; 429/CORS sem
> internals) e Spoofing residual (IP forjável sem edge). Não é sign-off do Architect.

## Open Questions

Nenhuma bloqueante. As 3 OQs da spec foram adotadas como decididas (ver § Source). Pendência
de processo: **aprovação de ADR-0003 e ADR-0004** pelo usuário antes de iniciar T-RL-* e
T-PG-*. As demais fases (headers/CORS, auditoria) podem iniciar independentemente.

## Convention Drift Detection — propostas (usuário aprova)

Padrões recorrentes/gaps observados que merecem virar convenção (não altero
`architecture-conventions.md` unilateralmente):

1. **Shape de resposta paginada ausente da convenção.** `§ API Contracts` define shape de
   erro, mas não de paginação — gap que esta feature precisou preencher (ADR-0004). Sugestão:
   adicionar `§ API Contracts → Paginated Response` padronizando `content`/`page`/`size`/
   `totalElements`/`totalPages` para todos os projetos. **Quer que eu rascunhe a edição?**
2. **Rate limiting in-app não tem padrão na convenção.** `security-conventions.md § Rate
   Limiting` só cobre edge/quotas, não o "como" in-app. Sugestão: nota de padrão in-app
   (filtro + token-bucket + 429/Retry-After) para quando edge for inviável. **Rascunho?**
3. **Formato de log de auditoria de mutação não padronizado.** `§ Logging` lista o que
   logar/não logar, mas não um esquema de evento de auditoria (campos canônicos). Sugestão:
   `§ Logging → Audit Event Schema` (operacao/recursoId/outcome/timestamp). **Rascunho?**
