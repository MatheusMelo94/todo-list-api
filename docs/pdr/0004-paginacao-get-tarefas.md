# PDR-0004: Paginação básica no GET /tarefas entra no escopo — reverte Non-Goal #3 (remedia F-0003)

- **Status:** Accepted
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo (autoridade única de produto)

## Context

A finding **F-0003** (`docs/reviews/security-findings-001.md` — LOW, OWASP A04 Insecure
Design / API4 Unrestricted Resource Consumption) registra que `GET /tarefas` faz
`findAll()` e materializa a coleção inteira em memória — vetor de consumo de memória/
latência (OOM) se a coleção crescer. Em 2026-06-01 Matheus Melo **recusou aceitar** esse
risco.

Diferentemente das outras três findings, esta colide com um **Non-Goal explícito**: a
Constitution v1 § Non-Goals #3 ("Paginacao avancada / filtros / ordenacao — listar
retorna todas as tarefas"). A própria finding roteou a remediação ao Product Strategist
"via PDR" justamente por isso. Este PDR reverte esse não-objetivo de forma controlada.

## Decision

**Incluir paginação básica no `GET /tarefas` via Spring Data `Pageable`** como remediação
de F-0003. A listagem passa a aceitar `page` e `size` com defaults e um **`size` máximo**
(teto), retornando metadados de paginação. Valores concretos (defaults, teto,
comportamento default) definidos na spec `002-hardening-seguranca`, ancorados em
`architecture-conventions.md § API Contracts` e nas convenções de consumo de recursos.

**Escopo limitado a paginação básica.** Reverte apenas a parte "paginação" do Non-Goal #3.
**Permanecem FORA de escopo:** filtros, busca textual e ordenação customizada — a parte
"avançada / filtros / ordenacao" do antigo não-objetivo continua valendo.

## Consequences

- **Positive:** remedia F-0003 (limita memória/latência por requisição via teto de
  `size`); melhora previsibilidade da listagem; exercita `Pageable` no projeto de
  referência.
- **Negative:** muda o contrato de resposta do `GET /tarefas` (de array puro para resposta
  com metadados de paginação) — quebra de contrato relativa à spec 001. O comportamento
  default e a compatibilidade são definidos na spec 002 para minimizar surpresa. Adiciona
  parâmetros e validação de `page`/`size`.
- **Neutral:** abre caminho natural para filtros/ordenação no futuro, se algum dia entrarem
  em escopo (novo PDR).

## Alternatives Considered

- **Manter `findAll()` e aceitar o risco.** Rejeitado: usuário recusou aceitar F-0003.
- **Limite fixo (cap "top N") sem `page`/`size`.** Rejeitado: corta dados silenciosamente
  e não dá ao cliente como navegar o restante; `Pageable` é o padrão da plataforma e
  resolve o vetor de consumo sem perda de acesso.
- **Paginação + filtros/ordenação completos.** Rejeitado (scope creep): F-0003 é só sobre
  consumo de recursos da listagem. Filtros/busca/ordenação permanecem fora — manter o
  mínimo que remedia a finding.

## Constitution Impact

**Sim — exige emenda.** **Reverte o Non-Goal #3 da v1** (paginação básica passa a estar em
escopo; filtros/busca/ordenação seguem fora). Formalizado na **Constitution v2**,
§ Non-Goals #3 (marcado como removido/revertido) e § Success Criteria critério 4. A v2
supersede a v1 (constituições imutáveis; emenda = nova versão).

## References

- Finding: `docs/reviews/security-findings-001.md` § Finding 0003
- Constituição: `memory/constitution.md` (v2) § Non-Goals #3, § Success Criteria #4
- Convenção: `architecture-conventions.md § API Contracts`
- Spec original afetada: `specs/001-crud-tarefas/spec.md` (AC2.x — contrato de `GET /tarefas` muda)
- Spec: `specs/002-hardening-seguranca/spec.md`
- PDRs relacionados: 0001, 0002, 0003
