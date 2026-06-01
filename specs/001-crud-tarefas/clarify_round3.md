# todo-list-api — crud-tarefas Clarification Round 3

- **Spec:** [crud-tarefas Spec v1](./spec.md)
- **Date:** 2026-06-01

> Sync upstream: o `plan.md` e o `tasks.md` ja referenciavam o criterio **AC4.8** e a
> decisao **OQ-1** (`status` obrigatorio no `PUT`), mas a spec terminava em AC4.7. Esta
> rodada sincroniza a spec com a decisao ja tomada — nao introduz decisao nova.

## Q&A

1. **Q:** No `PUT /tarefas/{id}` (substituicao completa estrita), o campo `status` deve ser
   opcional (aplicando o default `pendente`, como na criacao) ou obrigatorio?
   **A:** **Obrigatorio** (`@NotNull`). Decisao tomada por Matheus (autoridade unica de
   decisao de produto) em 2026-06-01, registrada como **OQ-1 — Resolved, opcao (b)** no
   `plan.md` (§ Open Questions / Decisao OQ-1) e refletida em `tasks.md` (T08, T10, T14,
   T17). Como o `PUT` e substituicao completa, o default `pendente` da criacao **nao** se
   aplica; um `PUT` sem `status` e uma representacao incompleta do recurso e deve ser
   barrado. Garante que nunca exista `status = null` no recurso apos um `PUT`.
   **Impact:** Adiciona **AC4.8** a secao "Atualizar tarefa — `PUT /tarefas/{id}`":
   `status` ausente no corpo -> **400 Bad Request** com o contrato de erro completo da
   convencao (`timestamp`, `status`, `error`, `message`, `path`), `message` indicando que
   `status` e obrigatorio na atualizacao; nada e alterado. Atualiza o "Modelo de Dados —
   Tarefa" (regra de `status`) e a subsecao "Semantica de atualizacao (`PUT`)" para tornar
   explicito que `status` e obrigatorio no `PUT` e que o default so vale na criacao.

## Resulting Spec Update

- Acceptance Criteria — PUT: adicionado **AC4.8** (`status` ausente -> 400 com contrato de
  erro completo da convencao; nada alterado).
- Modelo de Dados — Tarefa, regra de `status`: desmembrada em criacao (opcional, default
  `pendente`) vs. atualizacao (`PUT` obrigatorio; ausente -> 400, AC4.8; default nao se
  aplica).
- Semantica de atualizacao (`PUT` — substituicao completa): adicionada regra explicita de
  `status` obrigatorio no `PUT`, com referencia a AC4.8 e a garantia de nunca haver
  `status = null` apos um `PUT`.
- Clarifications Resolved: adicionado item registrando a sincronizacao da Rodada 3 e a
  decisao OQ-1.
- Status da spec: permanece **Ready for /plan** (sem alteracao — a spec ja estava em
  plan/tasks; esta rodada apenas sincroniza).
