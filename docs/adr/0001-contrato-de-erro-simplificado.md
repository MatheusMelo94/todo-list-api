# ADR-0001: Contrato de erro simplificado `{ erro, campo }` em vez do shape padrao da convencao

- **Status:** Rejected (2026-06-01)
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo

> **REJEITADO em 2026-06-01** por Matheus Melo (autoridade unica). O contrato de erro do
> projeto seguira o **formato completo da convencao** (`architecture-conventions.md
> § API Contracts`: `timestamp`, `status`, `error`, `message`, `path`), nao o desvio
> enxuto proposto abaixo. Decisao registrada em
> `specs/001-crud-tarefas/clarify_round2.md`. O texto abaixo permanece como registro
> historico da proposta rejeitada.

## Context

A `architecture-conventions.md § API Contracts` define que respostas de erro devem usar
um shape padrao com cinco campos: `(timestamp, status, error code, message, path)`.

A spec desta feature (`specs/001-crud-tarefas/spec.md § Contrato de erro padronizado`),
ja com status *Ready for /plan* e clarificacoes resolvidas em `/clarify` (2026-06-01),
fixa um contrato de erro deliberadamente mais enxuto:

```json
{ "erro": "<mensagem legivel>", "campo": "<nome do campo>" }
```

onde `campo` so esta presente em erros de validacao de campo (`400`) e e ausente/`null`
em `404`.

Ha um conflito direto entre o baseline (shape de 5 campos) e o contrato exigido pela spec
(shape de 2 campos, em portugues). A constituicao (§ Why This Way, § Constraints) declara
o projeto como exercicio de fluxo, com escopo minimo e nomenclatura em portugues. A
decisao precisa ser tomada agora porque o tratamento global de erros (`@RestControllerAdvice`)
e a forma das respostas de erro permeiam todos os 5 endpoints e seus testes.

## Decision

Adotar, **apenas para o projeto `todo-list-api`**, o contrato de erro simplificado
`{ "erro": "<mensagem>", "campo": "<campo>" }` definido na spec, no lugar do shape padrao
de cinco campos da `architecture-conventions.md § API Contracts`.

Especificamente:
- Um unico DTO de resposta de erro (record imutavel) com os campos `erro` (sempre
  presente) e `campo` (presente em `400` de validacao de campo; ausente/`null` em `404`).
- Serializacao omitindo `campo` quando `null` (`@JsonInclude(NON_NULL)`), satisfazendo
  "ausente ou null em 404" da spec.
- Todas as demais clausulas de `§ Error Handling` permanecem em vigor: handler global via
  `@RestControllerAdvice`, tipos de excecao de dominio (ex.: `ResourceNotFoundException`),
  e nenhuma exposicao de detalhes internos (sem stack trace, nomes de DB, paths) ao
  cliente.

## Consequences

- **Positive:** O contrato de erro fica alinhado a spec aprovada, em portugues, e
  trivialmente testavel contra os Acceptance Criteria (AC1.x, AC3.2, AC4.x, AC5.2).
  Mantem o escopo minimo que a constituicao exige.
- **Negative:** Divergencia do baseline; respostas de erro deste projeto nao sao
  uniformes com outros projetos do time (sem `timestamp`/`status`/`path`), o que reduz a
  utilidade caso este vire referencia de observabilidade. Aceitavel: a constituicao
  § Constraints exclui observabilidade avancada do v1.
- **Neutral:** A divergencia fica restrita a forma do corpo de erro; a disciplina de
  tratamento de erros (handler global, tipos de excecao, sem vazamento de internals)
  segue identica a convencao.

## Alternatives Considered

- **Usar o shape padrao de 5 campos da convencao (timestamp, status, error code, message,
  path):** rejeitado por contradizer diretamente a spec aprovada e introduzir campos que a
  constituicao § Constraints considera fora do escopo minimo do v1.
- **Hibrido (5 campos + `erro`/`campo` em portugues):** rejeitado por inflar o contrato,
  duplicar semantica (`message` vs `erro`) e violar o principio de escopo minimo.
- **Promover a mudanca direto na `architecture-conventions.md`:** rejeitado por agora — a
  forma enxuta e justificada pelo carater de exercicio deste projeto; generalizar para
  todos os projetos do time e uma decisao separada, oferecida ao final do `/plan` como
  proposta de drift, nao imposta por este ADR.

## References

- `specs/001-crud-tarefas/spec.md § Contrato de erro padronizado`
- `memory/constitution.md § Why This Way`, `§ Constraints`, `§ Non-Goals`
- `architecture-conventions.md § API Contracts`, `§ Error Handling`
