# Code Review: todo-list-api — crud-tarefas v1

- **Date:** 2026-06-01
- **Reviewer:** Matheus Melo (Code Reviewer)
- **Scope:** branch `feat/001-crud-tarefas` — 19 commits (`a5c2d48`..`7f8d7bf`). `main` está vazia (sem merge-base), portanto a revisão cobre o conjunto de commits da branch e toda a árvore `src/`.
- **Upstream artifacts:**
  - Spec: `specs/001-crud-tarefas/spec.md` (AC1.1–AC5.3, incl. AC4.8)
  - Plan: `specs/001-crud-tarefas/plan.md`
  - Tasks: `specs/001-crud-tarefas/tasks.md`
  - Constituição: `memory/constitution.md` (posture Prototype, projeto de teste)
  - ADRs: `docs/adr/0001-*` (Rejected), `docs/adr/0002-*` (Accepted)
  - Security Findings: `docs/reviews/security-findings-001.md` (veredito GO, 0 CRITICAL, 5 LOW)
  - QA Report: `docs/reviews/qa-report-001.md` (gate PASS, 72 testes verdes)
- **Recommendation:** **APPROVE**

---

## Summary

- Commits reviewed: **19**
- Tests added: **72** (16 classes de teste — unit + integration)
- Suite status (`./mvnw test`, rodado pelo Reviewer): **PASS** — 72 testes, 0 falhas, 0 erros, 0 skipped (exit 0)
- BLOCKING findings: **0**
- NON-BLOCKING findings: **2**
- NIT findings: **2**
- Open Security findings referenced: F-0001 (LOW, handler 500 de fallback — Open), F-0002/F-0003/F-0006/F-0007 (LOW, aceitação de risco recomendada). Nenhum bloqueador (posture Prototype bloqueia só CRITICAL; há 0 CRITICAL).

**Veredito em uma frase:** implementação limpa, em camadas, fiel à spec (22/22 ACs, incluindo AC4.8) e ao plano (T01–T19 todos com commit correspondente), com TDD disciplinado (teste no mesmo commit da implementação, em todas as tarefas de código) e suíte 100% verde verificada pelo Reviewer. Nada bloqueia o merge.

---

## Frameworks aplicados

### 1. Convention Adherence Check — PASS
- **Package Layout (§ Package Layout):** estrutura `com.matheusmelo.todolist/{controller,service,repository,model,dto,mapper,exception}` exatamente como a convenção. `config/` não tem beans extras (Mongo via auto-config — coerente com o plano). MATCH.
- **Layer Rules (§ Layer Rules):**
  - Controller thin: `TarefaController.java:36-64` apenas delega ao service, recebe/retorna DTO, nunca expõe entidade. MATCH.
  - Service detém a lógica (defaults, semântica PUT, lançamento de 404): `TarefaService.java:31-75`. MATCH.
  - Repository thin Spring Data sem query custom: `TarefaRepository.java:12`. MATCH.
  - Mapper em todo boundary entidade↔DTO: `TarefaMapper.java:20-45`, nunca bypassado. MATCH.
  - DTOs como records imutáveis: `TarefaCreateRequest`, `TarefaUpdateRequest`, `TarefaResponse`, `ErrorResponse`. MATCH.
- **API Contracts (§ API Contracts):** validação declarativa Jakarta (`@NotBlank`/`@Size`/`@NotNull`) em `TarefaCreateRequest.java:14-21` e `TarefaUpdateRequest.java:15-23`; `@Valid` no controller (`TarefaController.java:38,56`). Corpo de erro padrão de 5 campos em ingês (`ErrorResponse`) honrado. MATCH.
- **Error Handling (§ Error Handling):** `GlobalExceptionHandler.java:21-60` é `@RestControllerAdvice`; sem try/catch de controle nos controllers; mensagens sem stack trace/DB/path de classe; loga com SLF4J. MATCH (ressalva: cobertura de exceções — ver NON-BLOCKING #1, cross-ref F-0001).
- **Configuration & Secrets (§):** `application.yml` lê `${MONGODB_URI:...}` com profiles `local`/`docker`/`prod` (prod sem default inseguro); `.env` no `.gitignore`. MATCH.
- **Stack Baseline (§):** desvio de Boot 4.x → 3.5.4 **formalizado e Aceito em ADR-0002**, com escopo restrito a este projeto e gatilho de revisão. É desvio documentado, não improvisado. MATCH (via ADR).
- **Conventional Commits (§ Engineering Workflow):** todos os commits de código seguem `<type>: <subject>`, com corpo citando seções da convenção e linha `Tests: N passed, 0 failed (./mvnw test)`. MATCH.

### 2. TDD Discipline Audit — PASS
- Teste presente **no mesmo commit** da implementação em todas as tarefas de código (§ Engineering Workflow admite "test commit ou test added in same commit"). Verificado via `git show --stat`:
  - T08 `d8426c5`: DTOs + `TarefaRequestValidationTest` + `TarefaResponseTest`.
  - T12 `c45530a`: `GlobalExceptionHandler` + `GlobalExceptionHandlerIT`.
  - T13 `a823809`: `TarefaService` + `TarefaServiceTest`.
  - T14 `d573e62`: `atualizar` + `TarefaServiceAtualizarTest`.
  - T16 `0a28707`: controller POST/GET + `TarefaControllerPostGetIT`.
  - T17 `1f34b97`: controller PUT/DELETE + `TarefaControllerPutDeleteIT`.
- **Unit + integration por feature:** unit em service/mapper/model/dto/exception; integration (MockMvc + Mongo embedded) em repository/controller/handler. MATCH (§ Engineering Workflow — ambos os níveis).
- **Nomes comportamentais:** ex. `verPorIdInexistenteLancaResourceNotFound`, `putComDescricaoAusenteRetorna200ComDescricaoNull`, `postTituloAcima200Retorna400ContratoCompletoENadaPersiste`. Descrevem comportamento, não implementação. MATCH.
- **Suíte verde rodada pelo Reviewer:** `./mvnw test` → 72/72 verdes (não confiei só na linha do commit). MATCH.

### 3. Spec & Plan Alignment Check — PASS
- **22/22 ACs implementados e testados**, conforme a matriz AC→Teste do QA e confirmado na leitura do código/testes. Destaque **AC4.8** (PUT sem `status` → 400): garantido por `@NotNull` em `TarefaUpdateRequest.java:22` e exercitado em `TarefaControllerPutDeleteIT.putSemStatusRetorna400` (`:70-81`), que assere os 5 campos do contrato e `message` contendo "status". OK.
- **Plano T01–T19:** todas as tarefas têm commit correspondente (`git log --oneline` mapeia 1:1, T01..T19 + commit T-QA `7f8d7bf`). Sem tarefas órfãs; sem trabalho fora do plano. OK.
- **OQ-1 (status obrigatório no PUT):** dois DTOs de request distintos, exatamente como o plano § DTOs decidiu. OK.
- **Scope creep:** nenhum. As não-aplicações de Auth/CORS/Correlation IDs/Multi-tenant são reduções de escopo derivadas da constituição § Non-Goals, documentadas no plano § Convenções de escopo reduzido — não são desvios. OK.

### 4. Security Findings Cross-Reference — incorporado
- Findings Report existe e tem veredito **GO** (0 CRITICAL). Em posture Prototype, só CRITICAL bloqueia → **nenhum bloqueador de segurança**.
- **F-0001 (LOW, Open)** — ausência de `@ExceptionHandler(Exception.class)` de fallback 500. Confirmado independentemente: `GlobalExceptionHandler.java` cobre apenas 3 exceções (`:26`, `:37`, `:44`); `application.yml` não fixa `server.error.*`. Incorporado como **NON-BLOCKING #1** abaixo (o Security já roteou ao Backend Engineer). Não duplico a análise de segurança; apenas referencio e reforço como gap de robustez/consistência de erro.
- **F-0002/F-0003/F-0006/F-0007 (LOW, aceitação de risco recomendada):** hardening de produção explicitamente fora do v1 pela constituição § Constraints. Sem ação nesta feature; pendente aceite de risco do usuário (não bloqueia merge).

### 5. Test Quality Audit — PASS
- **Comportamental:** nomes descrevem o que o sistema faz (ver §2). MATCH.
- **Independente:** sem `@Order`; `AbstractMongoIntegrationTest.java:32-36` faz `mongoTemplate.getDb().drop()` + reconstrói o MockMvc em `@BeforeEach`, garantindo estado limpo entre casos. Unit tests recriam mocks em `@BeforeEach`. MATCH.
- **Determinístico:** sem `Thread.sleep`, sem clock real não-mockado, sem dados aleatórios. O teste de PUT lê o `dataCriacao` persistido antes de comparar (`TarefaControllerPutDeleteIT.java:34-40`), evitando flakiness por precisão de timestamp Mongo (ms) vs `Instant.now` (ns) — cuidado correto. MATCH.
- **Diagnóstico:** asserts via `jsonPath`/AssertJ produzem falhas legíveis. MATCH.
- **Nível certo:** unit para service/mapper/util-logic; integration para controller/repository/handler (§ Layer Rules). MATCH.
- **Não-tautológicos:** os testes exercem comportamento real ponta-a-ponta (HTTP→controller→service→Mongo embedded), incluindo assert de "nada persiste/nada altera" (`TarefaControllerBoundaryIT.java:72,119,135-137,151-154`). Não são testes que só reafirmam o mock. MATCH.

---

## Findings

### BLOCKING

Nenhum.

---

### NON-BLOCKING

#### Finding 0001: Sem handler de fallback 500 (`@ExceptionHandler(Exception.class)`) — consistência do contrato de erro
- **Location:** `src/main/java/com/matheusmelo/todolist/exception/GlobalExceptionHandler.java` (handlers nas linhas `:26`, `:37`, `:44` — falta o fallback genérico); `src/main/resources/application.yml` (sem `server.error.*`).
- **Convention citation:** per `architecture-conventions.md § Error Handling` ("Error messages exposed to clients carry no internal details") e § API Contracts (shape de erro padrão). Cross-ref **Security Finding F-0001**.
- **Issue:** Apenas três exceções têm handler. Uma exceção não mapeada (ex.: `IllegalArgumentException` de id mal-formado, `DataAccessException`) cairia no `BasicErrorController` padrão do Spring Boot, retornando um corpo que **não** segue o `ErrorResponse` de 5 campos da convenção. Em Prototype o impacto é baixo (sem dados sensíveis; stacktrace default = `never` no Boot 3.5), mas é uma inconsistência real de contrato de erro e robustez. O Security Engineer já registrou isto como F-0001 (Open, não bloqueante nesta posture).
- **Remediation:** adicionar `@ExceptionHandler(Exception.class)` retornando 500 no shape `ErrorResponse` com `message` genérica ("erro interno"), logando o detalhe só no servidor; e fixar em `application.yml`: `server.error.include-stacktrace: never`, `include-message: never`, `include-exception: false`. (Idêntico à remediação de F-0001.)
- **Routed to:** Backend Engineer.

#### Finding 0002: Versionamento de rota (`/v1/`) ausente — drift conhecido, decisão pendente
- **Location:** `src/main/java/com/matheusmelo/todolist/controller/TarefaController.java:27` (`@RequestMapping("/tarefas")`, sem prefixo de versão).
- **Convention citation:** per `architecture-conventions.md § API Contracts` ("DTOs are versioned at the route level when breaking changes occur").
- **Issue:** A convenção versiona rota "quando ocorrem breaking changes"; em greenfield sem consumidores, não exigir `/v1/` agora é defensável e o **plano já registrou isto explicitamente** como drift proposal #1 (§ Convention Drift), aguardando decisão do usuário. Não é um desvio silencioso — está documentado. Surge aqui apenas para fechar o loop: é uma decisão de baseline pendente, não um defeito de implementação.
- **Remediation:** decidir no nível de baseline (Backend Architect rascunha diretriz para § API Contracts: começar com `/v1/` no dia 1 vs. só na primeira breaking change). Sem ação na implementação até a decisão.
- **Routed to:** Backend Architect (diretriz de convenção) / Product Strategist+usuário (priorização). Não bloqueia esta feature.

---

### NIT

#### Finding 0003: `message` de status inválido no endpoint real não nomeia "status"
- **Location:** `src/main/java/com/matheusmelo/todolist/exception/GlobalExceptionHandler.java:37-42` (`handleUnreadable` → "corpo da requisicao invalido").
- **Issue:** Quando o enum não casa (ex.: `status:"arquivada"`), a falha vem como `HttpMessageNotReadableException` e a `message` é genérica, sem nomear o campo `status`. A spec AC1.3/AC4.4 exige apenas 400 + nada persistido (atendido), então **não é defeito**. É uma melhoria de UX de erro — alinhar com os demais 400 que identificam o campo. O QA já levantou a mesma observação (qa-report-001 § Observações #2).
- **Remediation (opcional):** detectar `InvalidFormatException` causada por `StatusTarefa` no `handleUnreadable` e emitir `message` "status invalido". Custo baixo.
- **Routed to:** Backend Engineer (se o usuário quiser a melhoria de UX).

#### Finding 0004: Imports de FQN inline nos testes em vez de `import`
- **Location:** ex. `TarefaControllerPutDeleteIT.java:31` e `:40` (`com.jayway.jsonpath.JsonPath.read(...)`), `TarefaControllerBoundaryIT.java:55,72,178` (`org.assertj...`, `org.jayway...` inline).
- **Issue:** Uso de nomes totalmente qualificados inline reduz a legibilidade vs. `import` no topo. Puramente estilístico; não afeta comportamento nem cobertura.
- **Remediation (opcional):** extrair para `import static`/`import`.
- **Routed to:** Backend Engineer (nit; opcional).

---

## Process Gaps

Nenhum gap bloqueante. Cadeia de artefatos completa e referenciada: constituição → spec (com 3 rodadas de `/clarify`) → plan → tasks → Security Findings → QA Report. Observações:

1. **ADR-0001 (contrato de erro enxuto) Rejected e ADR-0002 (pin Boot 3.5.x) Accepted** estão corretamente registrados; o desvio de § Stack Baseline está coberto por ADR com escopo de projeto e gatilho de revisão — exatamente o processo esperado. Sem gap.
2. **Aceitação de risco das Security Findings F-0002/F-0003/F-0006/F-0007** está pendente de assinatura do usuário (campos "Accepted by / Date accepted" em branco no relatório de segurança). Não bloqueia merge, mas recomenda-se registrar o aceite com os revisit triggers antes de considerar a feature "fechada".

---

## Sign-Off

- **Status:** Reviewed (aguardando decisão de merge do usuário)
- **Notes:** Recomendação **APPROVE**. Nenhum BLOCKING; suíte 72/72 verde verificada pelo Reviewer; 22/22 ACs (incl. AC4.8) implementados e testados; TDD e convenções respeitados; desvio de stack formalizado em ADR-0002 aceito. As duas pendências NON-BLOCKING (F-0001 / handler 500; versionamento de rota) e os dois NITs podem ser endereçados antes ou depois do merge a critério do usuário — nenhuma impede o merge. A autoridade final de merge é do usuário; sugere-se aplicar F-0001 (barato) e registrar o aceite de risco das Security Findings LOW como follow-ups.
