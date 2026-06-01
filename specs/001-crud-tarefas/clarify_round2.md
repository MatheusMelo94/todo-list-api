# todo-list-api — crud-tarefas Clarification Round 2

- **Spec:** [crud-tarefas Spec v1](./spec.md)
- **Date:** 2026-06-01

## Q&A

1. **Q:** Contrato de erro padronizado — manter o formato enxuto
   `{ "erro": "<mensagem>", "campo": "<campo>" }` (adotado na Rodada 1 e formalizado
   como desvio no ADR-0001), ou alinhar ao formato completo da convencao
   (`architecture-conventions.md § API Contracts`)?
   **A:** Adotar o **formato completo da convencao**: `timestamp`, `status` (codigo HTTP
   numerico), `error` (codigo/rotulo do erro), `message` (mensagem legivel) e `path` (URI
   da requisicao) — com os nomes de campo exatos da convencao, em ingles. O corpo de erro
   passa a ser em ingles (alinhado a convencao); o restante da spec e o dominio (rota
   `/tarefas`, campos `titulo`, `descricao`, `status`, `dataCriacao`) permanecem em
   portugues. O **ADR-0001 (desvio) foi REJEITADO** pelo usuario (Matheus, autoridade
   unica de decisao de produto) em 2026-06-01.
   **Impact:** Substitui a subsecao "Contrato de erro padronizado" em "Modelo de Dados —
   Tarefa". Atualiza os Acceptance Criteria que referenciavam `{ erro, campo }` (AC1.5,
   AC1.6, AC1.7 do POST; AC3.2 do GET por id; AC4.2-AC4.6 do PUT; AC5.2 do DELETE).

2. **Q:** A convencao define algum campo dedicado para indicar qual campo de validacao
   falhou (ex.: lista de `violations`/`errors`)?
   **A:** Nao. A convencao define apenas os cinco campos (`timestamp`, `status`, `error`,
   `message`, `path`). Portanto, nas validacoes `400`, a identificacao do campo que falhou
   (`titulo`, `descricao`, `status`) vai descrita na `message`. Nenhum campo novo e
   inventado alem do que a convencao especifica.
   **Impact:** ACs de validacao `400` reescritos para exigir que a `message` identifique o
   campo que falhou, em vez do antigo campo `campo`.

## Resulting Spec Update

- Modelo de Dados — Tarefa, subsecao "Contrato de erro padronizado": substituida pelo
  formato completo da convencao (`timestamp`, `status`, `error`, `message`, `path`), com
  nota de que a identificacao do campo de validacao vai na `message`.
- Acceptance Criteria — intro: passa a citar o contrato completo da convencao.
- Acceptance Criteria — POST: AC1.5, AC1.6, AC1.7 reescritos (sem `{ erro, campo }`).
- Acceptance Criteria — GET por id: AC3.2 reescrito (contrato completo, `status = 404`).
- Acceptance Criteria — PUT: AC4.2-AC4.6 reescritos (contrato completo).
- Acceptance Criteria — DELETE: AC5.2 reescrito (contrato completo, `status = 404`).
- Clarifications Resolved: item 1 atualizado para o formato completo da convencao e
  registro da rejeicao do ADR-0001.
- Decisao de produto registrada: **ADR-0001 REJEITADO** (status do ADR atualizado para
  *Rejected* em `docs/adr/0001-contrato-de-erro-simplificado.md`).
- Status da spec: permanece **Ready for /plan**.
