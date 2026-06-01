# todo-list-api — hardening-seguranca Spec v1

- **Status:** Ready for /plan
- **Date:** 2026-06-01
- **Constitution:** [todo-list-api Constitution v2](../../memory/constitution.md)

## Feature

Remediar in-app (Spring) as 4 findings de segurança recusadas por Matheus Melo —
rate limiting (F-0002), paginação (F-0003), logging/auditoria (F-0006) e headers de
segurança + CORS (F-0007) — sobre a API CRUD de tarefas existente, sem introduzir
autenticação.

> Nota de granularidade (Spec Quality Bar): esta spec agrupa 4 itens de hardening que
> compartilham um único objetivo (a remediação de segurança) e um único consumidor (o
> desenvolvedor), entregues como um pacote coeso aprovado em bloco pelos PDRs 0001–0004.
> Não são 4 jornadas de usuário distintas — é um endurecimento transversal da mesma API.
> Se o Backend Architect preferir fatiar no `/plan`/`/tasks`, é livre para fazê-lo; a spec
> mantém critérios de aceitação independentes por item para permitir isso.

## User & Value

- **User:** o desenvolvedor solo (constituição v2 § Users) — autor e consumidor da API,
  agora também exercitando o ciclo de **remediação de segurança orientada a findings**
  (constituição v2 § Problem, adendo).
- **Success criterion served:** avança diretamente o **critério 4** da constituição v2
  § Success Criteria — "as 4 findings F-0002/F-0003/F-0006/F-0007 remediadas in-app e
  cobertas por testes que passam".

## User Journey

O desenvolvedor, via cliente HTTP (curl / Postman / testes automatizados), exercita os
controles de hardening sobre a API existente:

1. **Rate limiting:** dispara requisições de escrita (`POST`/`PUT`/`DELETE` de `/tarefas`)
   acima do limite configurado e observa **429 Too Many Requests** ao exceder; abaixo do
   limite, as operações funcionam normalmente.
2. **Paginação:** chama `GET /tarefas?page=&size=` e recebe uma página de tarefas com
   metadados de paginação; tenta um `size` acima do teto e observa o teto sendo aplicado.
3. **Auditoria:** cria/atualiza/deleta uma tarefa e confirma, no log, um registro de
   auditoria estruturado da mutação, sem dados sensíveis.
4. **Headers + CORS:** inspeciona as respostas e confirma os headers de segurança
   presentes; faz uma requisição cross-origin de uma origem não permitida e observa o
   bloqueio de CORS; de uma origem da allowlist, observa a liberação.

## Itens de remediação e parâmetros concretos

Parâmetros são pontos de partida ancorados em `security-conventions.md`; ajustáveis sem
nova constituição (constituição v2 § Assumptions A4).

| Item | Finding | Decisão (PDR) | Parâmetros |
|------|---------|---------------|------------|
| Rate limiting | F-0002 | PDR-0001 | **60 requisições por IP por minuto, por endpoint de escrita** (janela fixa de 60s; aplica-se a `POST`, `PUT`, `DELETE` de `/tarefas`). Exceder → **429**. |
| Paginação | F-0003 | PDR-0004 | `page` default **0**, `size` default **20**, `size` **máximo 100**. |
| Logging/auditoria | F-0006 | PDR-0002 | Evento estruturado por mutação bem-sucedida em nível **INFO**; campos: `timestamp`, `operacao`, `tarefaId`, `outcome`. |
| Headers + CORS | F-0007 | PDR-0003 | Headers da convenção § Default Headers; CORS allowlist via env, nunca `*`. |

> Por que 60/min por IP nos endpoints de escrita: `security-conventions.md § Rate Limiting`
> usa "General API: 60 req/min per user" como ponto de partida; sem auth (Non-Goal #1), a
> chave de limite é o IP. Não há endpoint de login/reset (auth fora de escopo), então os
> limites de login/reset da convenção não se aplicam.

## Acceptance Criteria

Critérios observáveis e testáveis, agrupados por item. Todos os erros (incluindo 429)
seguem o contrato de erro padrão da convenção (`timestamp`, `status`, `error`, `message`,
`path`), consistente com a spec 001.

### 1. Rate limiting — F-0002 / PDR-0001
- AC1.1: Até **60 requisições de escrita por IP em uma janela de 60s** para um endpoint de
  escrita são processadas normalmente (status de sucesso conforme a spec 001).
- AC1.2: A **61ª** requisição de escrita do mesmo IP dentro da mesma janela de 60s retorna
  **429 Too Many Requests** com o contrato de erro padrão (`status = 429`, `path` = a URI
  requisitada); nenhuma tarefa é criada/alterada/removida por essa requisição barrada.
- AC1.3: Após a janela expirar (passados 60s), o mesmo IP volta a ser atendido
  normalmente (o contador reseta).
- AC1.4: Os endpoints de **leitura** (`GET /tarefas`, `GET /tarefas/{id}`) **não** são
  bloqueados por este limite de escrita (per PDR-0001, escopo limitado à escrita).
- AC1.5: A contagem é **por IP**: dois IPs distintos têm contadores independentes (um IP
  atingir o limite não bloqueia outro IP).

### 2. Paginação — F-0003 / PDR-0004
- AC2.1: `GET /tarefas` sem parâmetros retorna **200 OK** com a **primeira página**
  (`page = 0`) de no máximo **20** itens, acompanhada de metadados de paginação
  (no mínimo: número da página atual, tamanho da página, total de elementos e total de
  páginas).
- AC2.2: `GET /tarefas?page=1&size=5` retorna **200 OK** com a segunda página de até 5
  itens e os metadados refletindo `page = 1` e `size = 5`.
- AC2.3: `GET /tarefas?size=500` (acima do teto) é atendido aplicando o **teto de 100**:
  a página retornada tem no máximo 100 itens e os metadados refletem `size = 100`.
- AC2.4: Sem tarefas, `GET /tarefas` retorna **200 OK** com uma página vazia (lista de
  conteúdo vazia) e metadados (`totalElements = 0`), **não** um erro.
- AC2.5: `page`/`size` inválidos (negativos ou não-numéricos) retornam **400 Bad Request**
  com o contrato de erro padrão (`status = 400`), com `message` indicando o parâmetro
  inválido; nenhuma tarefa é retornada.
- AC2.6: O resultado é estável/determinístico para uma mesma coleção entre páginas (ordem
  consistente de paginação — ordenação default da plataforma; **sem** ordenação
  customizada por campo, que está fora de escopo).

### 3. Logging / auditoria — F-0006 / PDR-0002
- AC3.1: Um `POST /tarefas` bem-sucedido emite **um** registro de log de auditoria em
  nível **INFO** identificando a operação `create` e o `id` da tarefa criada.
- AC3.2: Um `PUT /tarefas/{id}` bem-sucedido emite um registro de auditoria `update` com o
  `id` afetado.
- AC3.3: Um `DELETE /tarefas/{id}` bem-sucedido emite um registro de auditoria `delete`
  com o `id` afetado.
- AC3.4: O registro de auditoria contém, no mínimo, os campos `timestamp`, `operacao`
  (`create`/`update`/`delete`), `tarefaId` e `outcome` (sucesso) — em formato estruturado.
- AC3.5: O registro de auditoria **não** contém dados sensíveis nem o corpo completo da
  requisição/resposta — concretamente, **não loga** o conteúdo de `titulo`/`descricao`
  (per `security-conventions.md § Logging & Monitoring` — never log full request bodies;
  e `architecture-conventions.md § Logging`). _(Decisão de escopo: auditamos a ocorrência
  e o `id` da mutação, não o conteúdo dos campos.)_
- AC3.6: Operações de **leitura** (`GET`) **não** emitem registro de auditoria de mutação.

### 4. Headers de segurança + CORS — F-0007 / PDR-0003
- AC4.1: Toda resposta da API carrega, no mínimo, os headers: `Strict-Transport-Security`,
  `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy:
  strict-origin-when-cross-origin` (per `security-conventions.md § Default Headers`).
- AC4.2: O CORS opera por **allowlist explícita de origens**, configurável via variável de
  ambiente; o valor **nunca é `*`** (per `security-conventions.md § CORS Default`).
- AC4.3: Uma requisição (preflight `OPTIONS` e/ou requisição real) de uma origem **na**
  allowlist recebe os headers `Access-Control-Allow-Origin` correspondentes e é permitida.
- AC4.4: Uma requisição de uma origem **fora** da allowlist **não** recebe
  `Access-Control-Allow-Origin` para aquela origem (bloqueio de CORS).
- AC4.5: O CORS expõe métodos e headers **explícitos** (não wildcard) coerentes com os
  endpoints existentes (`GET`, `POST`, `PUT`, `DELETE`).
- AC4.6: **Todos os endpoints permanecem anônimos** — nenhuma requisição passa a exigir
  autenticação após a introdução do Spring Security (confirma que Non-Goal #1 permanece
  intacto): um `GET /tarefas` sem qualquer credencial retorna **200 OK**, não 401/403.

### 5. Regressão / não-quebra
- AC5.1: Todos os critérios de aceitação da spec 001 (CRUD) **continuam válidos**, exceto
  o contrato de resposta de `GET /tarefas`, que muda de array puro para resposta paginada
  (AC2.x acima) — esta é a única quebra de contrato intencional, derivada de PDR-0004.
- AC5.2: A suíte de testes existente passa (ajustada apenas no ponto do contrato de
  listagem); os novos controles têm cobertura de teste própria (caminho feliz + caminho de
  bloqueio/erro por item), satisfazendo a constituição v2 § Success Criteria #4.

## Casos de erro

- **429 (rate limit):** contrato de erro padrão, `status = 429`. _[Ver OQ-1: incluir
  `Retry-After`?]_
- **400 (paginação inválida):** `page`/`size` negativos ou não-numéricos → contrato de erro
  padrão, `status = 400`, `message` indica o parâmetro.
- **CORS bloqueado:** ausência dos headers `Access-Control-Allow-*` para a origem não
  permitida (comportamento padrão do mecanismo de CORS — não é um corpo de erro da API).
- **Erros não mapeados:** continuam cobertos pelo `GlobalExceptionHandler` (fallback 500
  da F-0001, já Resolved) — sem vazamento de detalhes.

## Out of Scope (this feature)

1. **Autenticação / autorização** — Spring Security entra apenas para headers + CORS;
   nenhum endpoint passa a exigir login/token (constituição v2 § Non-Goals #1, intacto;
   PDR-0003). Confirmado por AC4.6.
2. **Filtros, busca textual e ordenação customizada** no `GET /tarefas` — apenas paginação
   básica entra; a parte "avançada/filtros/ordenação" do antigo Non-Goal #3 permanece fora
   (PDR-0004; constituição v2 § Non-Goals #3).
3. **Rate limiting nos endpoints de leitura** e rate limiting distribuído/edge — limite
   in-app por IP só nos endpoints de escrita (PDR-0001). Edge fica para quando houver
   deploy (revisit trigger da finding).
4. **Observabilidade avançada com vendor** (Sentry/APM/tracing distribuído), persistência
   de auditoria em coleção dedicada, segredos via vault e encryption at rest — continuam
   fora até existir deploy real (constituição v2 § Constraints, "Limites de hardening").
5. **CSP e Permissions-Policy completos** — não há frontend (Non-Goal #6); os headers de
   conteúdo voltados a navegador (CSP nonce-based) ficam fora desta remediação de API.
   _[Ver OQ-2.]_

## Open Clarifications

Pontos genuinamente ambíguos; **não bloqueiam o `/plan`** (têm default proposto). O
Backend Architect pode confirmar/ajustar no `/plan` ou via `/clarify`.

1. **OQ-1 — `Retry-After` no 429:** incluir o header `Retry-After` na resposta 429 para
   indicar quando reenviar? **Default proposto:** sim, incluir (boa prática; baixo custo).
2. **OQ-2 — CSP/Permissions-Policy:** confirmar que CSP e Permissions-Policy ficam fora
   por ser API sem frontend (Non-Goal #6). **Default proposto:** fora de escopo (apenas os
   headers de AC4.1). Reabrir se um frontend entrar em escopo.
3. **OQ-3 — granularidade do limite de rate:** "60/min por IP **por endpoint de escrita**"
   (contadores separados por endpoint) vs "60/min por IP **somando** todos os endpoints de
   escrita". **Default proposto:** por endpoint de escrita (mais permissivo, mais simples
   de raciocinar). Confirmar com o Architect.

## Related PDRs

- **PDR-0001** — Rate limiting in-app (F-0002).
- **PDR-0002** — Logging/auditoria in-app (F-0006).
- **PDR-0003** — Headers de segurança + CORS via Spring Security in-app (F-0007).
- **PDR-0004** — Paginação básica no `GET /tarefas`, reverte Non-Goal #3 (F-0003).

Todos Accepted (2026-06-01). Esta spec implementa o pacote de remediação aprovado por
esses PDRs, ancorada na Constitution v2 (§ Success Criteria #4).
