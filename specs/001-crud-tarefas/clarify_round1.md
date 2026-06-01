# todo-list-api — crud-tarefas Clarification Round 1

- **Spec:** [crud-tarefas Spec v1](./spec.md)
- **Date:** 2026-06-01

## Q&A

1. **Q:** Codigos HTTP e contrato de erro — manter os codigos propostos
   (201/200/204/400/404)? Usar corpo de erro padronizado ou mensagem livre?
   **A:** Manter os codigos. Adotar corpo de erro padronizado simples:
   `{ "erro": "<mensagem legivel>", "campo": "<nome do campo>" }`, onde `campo` e
   opcional — presente apenas em erros de validacao de campo (`400`); ausente/`null`
   em `404`.
   **Impact:** Modelo de Dados — Tarefa ganha a subsecao "Contrato de erro padronizado".
   Acceptance Criteria atualizados para referenciar o contrato (AC1.7, AC3.2, AC4.2-4.6,
   AC5.2).

2. **Q:** Semantica de atualizacao — confirmar `PUT` (substituicao completa) vs `PATCH`?
   Se `descricao` vier ausente no corpo, limpar ou manter?
   **A:** `PUT` com substituicao completa (estrita). `descricao` ausente e limpa (`null`).
   `titulo` continua obrigatorio no `PUT`.
   **Impact:** Modelo de Dados ganha a subsecao "Semantica de atualizacao". Novo AC4.7
   cobrindo a limpeza de `descricao`. Out of Scope item 3 (PATCH) permanece valido.

3. **Q:** Nomenclatura — rota e campos em portugues ou ingles?
   **A:** Portugues — rota `/tarefas`; campos `titulo`, `descricao`, `status`,
   `dataCriacao`. Confirmado.
   **Impact:** Nenhuma mudanca estrutural — a spec ja assumia portugues; decisao agora
   registrada como firme.

4. **Q:** Limites de tamanho para `titulo`/`descricao` no v1?
   **A:** `titulo` maximo 200 caracteres; `descricao` maximo 2000 caracteres. Exceder
   retorna **400 Bad Request** com o contrato de erro padronizado.
   **Impact:** Duas novas regras no Modelo de Dados. Novos ACs: AC1.5/AC1.6 (criacao) e
   AC4.5/AC4.6 (atualizacao).

## Resulting Spec Update

- Modelo de Dados — Tarefa: adicionadas regras de tamanho (`titulo` <= 200,
  `descricao` <= 2000) e duas subsecoes ("Contrato de erro padronizado" e "Semantica de
  atualizacao").
- Acceptance Criteria — POST: adicionados AC1.5, AC1.6, AC1.7.
- Acceptance Criteria — GET por id: AC3.2 agora especifica o corpo de erro 404.
- Acceptance Criteria — PUT: AC4.2-AC4.4 referenciam o contrato de erro; adicionados
  AC4.5, AC4.6, AC4.7.
- Acceptance Criteria — DELETE: AC5.2 agora especifica o corpo de erro 404.
- Secao "Open Clarifications" substituida por "Clarifications Resolved" (2026-06-01).
- Status movido de **Ready for /clarify** para **Ready for /plan**.
- Nenhum PDR gerado: todas as decisoes sao detalhamentos compativeis com a constituicao
  § Non-Goals.
