# Test Pass: todo-list-api — crud-tarefas v1

- **Date:** 2026-06-01
- **Reviewed commits:** branch `feat/001-crud-tarefas` (base `0a4e26e`)
- **Touchpoint:** Pós-implementação (QA E2E + Regressão + Exploratório)
- **Test Plan reference:** derivado na hora (sem Test Plan pré-implementação formal); ACs extraídos de `specs/001-crud-tarefas/spec.md`
- **Posture:** MVP / Prototype (constituição § Constraints — projeto de teste, sem produção real)
- **Framework:** MockMvc + Mongo embedded (flapdoodle), conforme a suíte existente. Sem Docker/Testcontainers/REST Assured (desvio justificado vs. `qa-conventions.md § Framework Defaults` — o projeto já usa MockMvc).
- **Gate Recommendation:** **PASS**

---

## Matriz AC → Teste

Legenda de camada: **DTO** = Bean Validation unit (`TarefaRequestValidationTest`); **GEH** = `GlobalExceptionHandlerIT` (stub controller); **E2E** = teste de integração HTTP no endpoint real `/tarefas` (controllers IT); **QA-NEW** = teste E2E adicionado nesta passagem.

| AC # | Critério | Cobertura prévia (Engineer) | Cobertura E2E pós-QA | Status |
|------|----------|-----------------------------|----------------------|--------|
| AC1.1 | POST válido → 201 + corpo (id, status=pendente, dataCriacao) | E2E `postValidoRetorna201ComCorpoELocation` | — | OK |
| AC1.2 | POST sem título → 400, nada persiste | E2E `postInvalidoRetorna400ComCorpoDeErroPadrao`; DTO | — | OK |
| AC1.3 | POST status inválido → 400, nada persiste | DTO; GEH (stub) | **QA-NEW** `postStatusInvalidoRetorna400ENadaPersiste` (endpoint real + assert nada persiste) | OK — **lacuna E2E fechada** |
| AC1.4 | POST sem descrição → 201 (descrição nula) | E2E `postComStatusInformadoPreserva` (cria sem descrição); `getListaRetornaTarefas` | **QA-NEW** `postDescricaoNullExplicitaRetorna201ComDescricaoNull` (descricao:null explícito) | OK |
| AC1.5 | POST título > 200 → 400 + contrato | DTO `createComTituloAcima200Viola` | **QA-NEW** `postTituloAcima200Retorna400ContratoCompletoENadaPersiste` | OK — **lacuna E2E fechada** |
| AC1.6 | POST descrição > 2000 → 400 + contrato | DTO `createComDescricaoAcima2000Viola` | **QA-NEW** `postDescricaoAcima2000Retorna400ContratoCompletoENadaPersiste` | OK — **lacuna E2E fechada** |
| AC1.7 | Validações 400 → contrato completo (5 campos) com campo na message | GEH (stub `/stub`) | **QA-NEW** assert 5 campos (`timestamp/status/error/message/path`) no endpoint real `/tarefas` | OK — **lacuna E2E fechada** |
| AC2.1 | GET com tarefas → 200 + array | E2E `getListaRetornaTarefas` | — | OK |
| AC2.2 | GET sem tarefas → 200 `[]` | E2E `getListaVaziaRetorna200ArrayVazio` | — | OK |
| AC3.1 | GET por id existente → 200 | E2E `getPorIdExistenteRetorna200` | — | OK |
| AC3.2 | GET por id inexistente → 404 + contrato (path) | E2E `getPorIdInexistenteRetorna404` | — | OK |
| AC4.1 | PUT válido → 200; id/dataCriacao intactos | E2E `putValidoRetorna200ComIdEDataCriacaoIntactos` | — | OK |
| AC4.2 | PUT id inexistente → 404, nada alterado | E2E `putInexistenteRetorna404` | — | OK |
| AC4.3 | PUT título ausente/vazio → 400, nada alterado | E2E `putComTituloInvalidoRetorna400`; DTO | — | OK |
| AC4.4 | PUT status inválido → 400, nada alterado | E2E `putComStatusInvalidoRetorna400` | — | OK |
| AC4.5 | PUT título > 200 → 400, nada alterado | DTO `updateComTituloAcima200Viola` | **QA-NEW** `putTituloAcima200Retorna400ENadaAltera` (+ assert título original preservado) | OK — **lacuna E2E fechada** |
| AC4.6 | PUT descrição > 2000 → 400, nada alterado | DTO `updateComDescricaoAcima2000Viola` | **QA-NEW** `putDescricaoAcima2000Retorna400ENadaAltera` (+ assert título/descrição originais preservados) | OK — **lacuna E2E fechada** |
| AC4.7 | PUT sem descrição → 200; descrição vira null (PUT estrito) | E2E `putComDescricaoAusenteRetorna200ComDescricaoNull` | — | OK |
| AC4.8 | PUT sem status → 400 + contrato completo (status obrigatório) | E2E `putSemStatusRetorna400`; DTO `updateComStatusNuloViola` | — | OK |
| AC5.1 | DELETE existente → 204 sem corpo | E2E `deleteExistenteRetorna204SemCorpo` | — | OK |
| AC5.2 | DELETE inexistente → 404 + contrato | E2E `deleteInexistenteRetorna404` | — | OK |
| AC5.3 | DELETE seguido de GET → 404 | E2E `deleteSeguidoDeGetRetorna404` | — | OK |

**Cobertura de ACs: 22/22.** Crédito ao Engineer: todos os caminhos felizes e a maioria dos caminhos de erro já estavam cobertos a nível de E2E e/ou DTO. Sem testes duplicados — QA só adicionou onde havia lacuna de camada.

---

## Lacunas encontradas e fechadas

Todas as lacunas eram de **camada de cobertura**, não de funcionalidade: os limites de tamanho (200/2000) e o status inválido estavam validados no nível de Bean Validation (DTO) ou no stub do `GlobalExceptionHandler`, mas **não exercitados ponta-a-ponta pelos endpoints reais `POST/PUT /tarefas`**. Em uma posture com production-ready bar, um E2E real garante que a anotação de validação está de fato ligada ao endpoint (e não apenas presente no DTO).

Lacunas fechadas (arquivo novo `src/test/java/.../controller/TarefaControllerBoundaryIT.java`):

1. **AC1.3** — status inválido no endpoint real `POST /tarefas` + assert de que nada persiste.
2. **AC1.5 / AC1.7** — título > 200 via HTTP → 400 com contrato completo de 5 campos + nada persiste.
3. **AC1.6** — descrição > 2000 via HTTP → 400 + nada persiste.
4. **AC4.5** — PUT título > 200 → 400 + assert de que o recurso permanece inalterado.
5. **AC4.6** — PUT descrição > 2000 → 400 + assert de que título/descrição originais são preservados.
6. **Limites EXATOS (boundary value analysis)** — `titulo`=200 e `descricao`=2000 são **aceitos** (201). Não havia teste do limite exato em nenhuma camada; agora confirmado que o limite é inclusivo (≤200 / ≤2000 ok; 201/2001 rejeitado).

Total: **11 testes E2E novos**, todos verdes.

---

## E2E Results

- Tests run (novos E2E QA): **11**
- Passed: **11**
- Failed: **0**
- Failure details: nenhum.

## Regression Results

- **Escopo de regressão** (`qa-conventions.md § Regression Scope`): a mudança não alterou código de produção (QA só adicionou testes), portanto a suíte inteira de features prévias é o escopo. Reexecutada por completo.
- **Suite status: PASS**
- Suíte completa: **72 testes** (61 prévios + 11 novos), **0 falhas, 0 erros, 0 skipped**.
- Os 61 testes pré-existentes (controllers, service, repository, mapper, DTO, model, exception, contexto) permaneceram verdes — nenhuma quebra introduzida.
- Failure details: nenhum.

## Exploratory Findings

- **Time-boxed:** 15 min (posture Prototype/MVP — `qa-conventions.md § Exploratory Time-Box`).
- **Bugs encontrados:** 0.
- Probes aplicados (`qa-conventions.md § Exploratory Checklist`):
  - **Boundary inputs:** limites exatos 200/2000 → aceitos; 201/2001 → 400. OK.
  - **Malformed inputs:** JSON malformado em `POST /tarefas` → 400 com contrato (`status`/`path`). OK.
  - **Wrong type / enum case:** `status:"PENDENTE"` (maiúsculo) → 400 (enum é case-sensitive; não há coerção silenciosa). Comportamento correto e estrito.
  - **Extra fields:** campo `id` forjado e `campoExtra` no corpo do POST → 201, e o `id` retornado é gerado pelo sistema (forjado ignorado). Não há mass-assignment do `id`. OK.
  - **Explicit null:** `descricao:null` explícito no POST → 201 com `descricao=null`, `status=pendente`. OK.
- **Explorações fora de escopo / não tentadas (e por quê):**
  - **Concorrência / writes simultâneos / idempotência:** fora de escopo para esta posture; coleção global sem locking otimista é aceito pela constituição (sem produção real). Não há campo de versão para testar.
  - **Autenticação / autorização / IDOR:** fora de escopo por design (constituição § Non-Goals 1 e 2 — sem auth, coleção global).
  - **DB indisponível / timeout externo:** não exercitado — flapdoodle não simula queda; baixo valor nesta posture.
  - **Smoke de performance:** não executado — apenas em posture MVP+ com requisito explícito; constituição § Constraints declara sem SLA.

## Bugs by Severity

- CRITICAL: 0
- HIGH: 0
- MEDIUM: 0
- LOW: 0

Nenhum bug aberto.

## Routing Summary

- Routed to Backend Engineer: 0
- Routed to Backend Architect: 0
- Routed to Product Strategist: 0

## Observações (não-bloqueantes, para o usuário decidir)

1. **Cobertura de tamanho vivia só no DTO/stub.** Não é um defeito — a implementação está correta — mas em produção o ideal é manter o E2E real como rede de segurança contra alguém remover a anotação `@Size`/`@Valid` do controller sem que um teste de unidade DTO perceba. Agora coberto.
2. **Mensagem de validação de status inválido:** quando o enum não casa, a resposta vem do desserializador (corpo ilegível), então a `message` para status inválido no endpoint real não necessariamente contém a palavra "status" (diferente do PUT com `@NotNull`, que sim). A spec (AC1.3) exige apenas 400 + nada persistido — atendido. Se o usuário quiser que a `message` sempre nomeie "status", isso é uma melhoria de UX de erro, roteável ao Engineer; **não é bug** contra a spec atual.

## Sign-Off

- **Status:** Reviewed (aguardando aceitação do usuário)
- **Gate:** **PASS** — 22/22 ACs cobertos a nível E2E, 72/72 testes verdes, 0 regressões, 0 bugs CRITICAL/HIGH. Seguro prosseguir para Code Review.
- **Critério 2 da constituição** (≥1 feliz + ≥1 erro por endpoint, ≥10 testes, todos passando): atendido com folga (72 testes verdes, cobertura de erro por endpoint).
- **Notes:** Autoridade final de merge permanece com o usuário; este PASS é um input ao lado do APPROVE do Code Reviewer.
