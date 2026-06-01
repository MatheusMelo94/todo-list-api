# todo-list-api — crud-tarefas Spec v1

- **Status:** Ready for /plan
- **Date:** 2026-06-01
- **Constitution:** [todo-list-api Constitution v1](../../memory/constitution.md)

## Feature

Uma API REST que permite gerenciar o ciclo de vida completo de tarefas (criar, listar,
ver, atualizar e deletar) sobre uma unica colecao global, sem autenticacao.

## User & Value

- **User:** o desenvolvedor solo (constituicao § Users) — autor e consumidor da API,
  exercitando o stack e o fluxo de desenvolvimento.
- **Success criterion served:** avanca o criterio 1 (os 5 endpoints CRUD implementados e
  respondendo conforme spec) e habilita o criterio 2 (cobertura de testes por endpoint),
  ambos da constituicao § Success Criteria (v1).

## User Journey

O desenvolvedor, via cliente HTTP (curl / Postman / testes automatizados):

1. **Cria** uma tarefa enviando titulo e descricao; o sistema gera id, define status
   inicial `pendente` e registra a data de criacao.
2. **Lista** todas as tarefas existentes.
3. **Ve** uma tarefa especifica pelo seu id.
4. **Atualiza** uma tarefa existente (titulo, descricao e/ou status).
5. **Deleta** uma tarefa pelo id.

## Modelo de Dados — Tarefa

| Campo         | Tipo                          | Origem            | Obrigatorio na criacao |
|---------------|-------------------------------|-------------------|------------------------|
| `id`          | identificador (gerado)        | sistema           | n/a (gerado)           |
| `titulo`      | texto                         | cliente           | Sim                    |
| `descricao`   | texto                         | cliente           | Nao                    |
| `status`      | enum: `pendente` / `concluida`| cliente / sistema | Nao (default `pendente`)|
| `dataCriacao` | timestamp                     | sistema           | n/a (gerado)           |

Regras:
- `titulo` e obrigatorio e nao pode ser vazio/em-branco.
- `titulo` tem tamanho maximo de **200 caracteres**.
- `descricao` e opcional e tem tamanho maximo de **2000 caracteres**.
- `status` so aceita os valores `pendente` ou `concluida`.
  - Na **criacao** (`POST`): opcional; quando ausente, default `pendente`.
  - Na **atualizacao** (`PUT`): **obrigatorio** (substituicao completa estrita); ausente
    -> **400 Bad Request** (ver AC4.8). O default `pendente` **nao** se aplica ao `PUT`.
- `dataCriacao` e definido pelo sistema na criacao e e imutavel.
- `id` e gerado pelo sistema e e imutavel.

### Contrato de erro padronizado

Todos os erros (`400` e `404`) retornam o corpo de erro padrao da convencao
(`architecture-conventions.md § API Contracts`), com os nomes de campo exatos da
convencao (em ingles):

```json
{
  "timestamp": "<data-hora ISO-8601 da ocorrencia>",
  "status": 400,
  "error": "<codigo/rotulo do erro, ex.: Bad Request / Not Found>",
  "message": "<mensagem legivel descrevendo o problema>",
  "path": "<URI da requisicao, ex.: /tarefas/{id}>"
}
```

- `timestamp`: data-hora em que o erro ocorreu.
- `status`: codigo HTTP numerico (`400` ou `404`).
- `error`: codigo/rotulo do erro.
- `message`: mensagem legivel descrevendo o problema. Sempre presente.
- `path`: URI da requisicao que originou o erro.

A convencao **nao** define um campo dedicado para indicar qual campo de validacao
falhou. Portanto, nos erros de validacao (`400`), a identificacao do campo que falhou
(`titulo`, `descricao`, `status`) vai descrita na `message` (ex.: `"titulo nao pode ser
vazio"`, `"titulo excede 200 caracteres"`). O corpo de erro segue a convencao em ingles;
o restante da spec e o dominio (rota `/tarefas`, campos `titulo`, `descricao`, `status`,
`dataCriacao`) permanecem em portugues.

### Semantica de atualizacao (`PUT` — substituicao completa)

- O `PUT /tarefas/{id}` faz substituicao completa do recurso (semantica PUT estrita).
- `titulo` continua obrigatorio no `PUT`.
- `status` e **obrigatorio** no `PUT` (substituicao completa): ao contrario da criacao, o
  `PUT` nao aplica o default `pendente`. Se `status` vier ausente, a requisicao e barrada
  com **400 Bad Request** (ver AC4.8); nada e alterado. Isso garante que nunca exista
  `status = null` no recurso apos um `PUT`.
- Se `descricao` vier ausente no corpo, ela e **limpa** (passa a `null`); o valor anterior
  nao e preservado.

## Acceptance Criteria

Criterios observaveis e testaveis, organizados por endpoint. Todos os erros seguem o
contrato de erro padronizado da convencao (`timestamp`, `status`, `error`, `message`,
`path`) descrito em "Modelo de Dados — Tarefa".

### 1. Criar tarefa — `POST /tarefas`
- AC1.1: Com `titulo` valido, retorna **201 Created** com o corpo da tarefa criada,
  incluindo `id`, `status = pendente` e `dataCriacao` preenchidos pelo sistema.
- AC1.2: Sem `titulo` (ausente, vazio ou somente espacos), retorna **400 Bad Request**
  com mensagem indicando que `titulo` e obrigatorio; nenhuma tarefa e persistida.
- AC1.3: Com `status` invalido (diferente de `pendente`/`concluida`), retorna
  **400 Bad Request**; nenhuma tarefa e persistida.
- AC1.4: Se `descricao` for omitida, a tarefa e criada com sucesso (descricao
  nula/vazia).
- AC1.5: Com `titulo` acima de 200 caracteres, retorna **400 Bad Request** com o contrato
  de erro padronizado (`status = 400`), com `message` indicando que `titulo` excede o
  limite; nenhuma tarefa e persistida.
- AC1.6: Com `descricao` acima de 2000 caracteres, retorna **400 Bad Request** com o
  contrato de erro padronizado (`status = 400`), com `message` indicando que `descricao`
  excede o limite; nenhuma tarefa e persistida.
- AC1.7: Erros de validacao (`titulo` ausente/vazio, `status` invalido, limites de
  tamanho) retornam o contrato de erro padronizado completo (`timestamp`, `status`,
  `error`, `message`, `path`), com a `message` identificando o campo que falhou.

### 2. Listar tarefas — `GET /tarefas`
- AC2.1: Com tarefas existentes, retorna **200 OK** com um array de todas as tarefas.
- AC2.2: Sem tarefas, retorna **200 OK** com um array vazio (`[]`), nao um erro.

### 3. Ver tarefa por id — `GET /tarefas/{id}`
- AC3.1: Com `id` existente, retorna **200 OK** com o corpo da tarefa correspondente.
- AC3.2: Com `id` inexistente, retorna **404 Not Found** com o contrato de erro
  padronizado (`status = 404`, `path` = a URI requisitada).

### 4. Atualizar tarefa — `PUT /tarefas/{id}`
- AC4.1: Com `id` existente e corpo valido, retorna **200 OK** com a tarefa atualizada;
  `id` e `dataCriacao` permanecem inalterados.
- AC4.2: Com `id` inexistente, retorna **404 Not Found** com o contrato de erro
  padronizado (`status = 404`, `path` = a URI requisitada); nada e alterado.
- AC4.3: Com `titulo` ausente/vazio/em-branco no corpo, retorna **400 Bad Request** com o
  contrato de erro padronizado (`status = 400`), com `message` indicando que `titulo` e
  obrigatorio; nada e alterado.
- AC4.4: Com `status` invalido no corpo, retorna **400 Bad Request** com o contrato de
  erro padronizado (`status = 400`), com `message` indicando que `status` e invalido; nada
  e alterado.
- AC4.5: Com `titulo` acima de 200 caracteres, retorna **400 Bad Request** com o contrato
  de erro padronizado (`status = 400`), com `message` indicando que `titulo` excede o
  limite; nada e alterado.
- AC4.6: Com `descricao` acima de 2000 caracteres, retorna **400 Bad Request** com o
  contrato de erro padronizado (`status = 400`), com `message` indicando que `descricao`
  excede o limite; nada e alterado.
- AC4.7: Com `descricao` ausente no corpo (e demais campos validos), retorna **200 OK** e
  a `descricao` da tarefa passa a `null` (substituicao completa — semantica PUT estrita).
- AC4.8: Com `status` ausente no corpo do `PUT`, retorna **400 Bad Request** com o contrato
  de erro completo da convencao (`timestamp`, `status`, `error`, `message`, `path`), com a
  `message` indicando que `status` e obrigatorio na atualizacao; nada e alterado.

### 5. Deletar tarefa — `DELETE /tarefas/{id}`
- AC5.1: Com `id` existente, remove a tarefa e retorna **204 No Content**.
- AC5.2: Com `id` inexistente, retorna **404 Not Found** com o contrato de erro
  padronizado (`status = 404`, `path` = a URI requisitada).
- AC5.3: Apos delecao bem-sucedida, um `GET /tarefas/{id}` para o mesmo id retorna
  **404 Not Found**.

## Out of Scope (this feature)

1. **Autenticacao / autorizacao** em qualquer endpoint (constituicao § Non-Goals 1).
2. **Filtros, busca, ordenacao ou paginacao** no `GET /tarefas` — sempre retorna todas
   (constituicao § Non-Goals 3).
3. **Atualizacao parcial (PATCH)** — o v1 usa `PUT` com o corpo completo; semantica
   PATCH fica fora.
4. **Soft delete / lixeira** — delecao e permanente.

## Clarifications Resolved

Rodada de `/clarify` em **2026-06-01** — 4 clarificacoes resolvidas (ver
`clarify_round1.md`). Rodada 2 em **2026-06-01** revisa o contrato de erro (ver
`clarify_round2.md`). Rodada 3 em **2026-06-01** sincroniza a spec com a decisao OQ-1
(`status` obrigatorio no `PUT`, AC4.8) ja presente no plan/tasks (ver `clarify_round3.md`).
Nenhuma decisao conflita com a constituicao § Non-Goals; nenhum PDR necessario.

1. **Codigos HTTP e contrato de erro:** mantidos os codigos 201/200/204/400/404. O
   contrato de erro segue o **formato completo da convencao**
   (`architecture-conventions.md § API Contracts`): `timestamp`, `status`, `error`,
   `message`, `path` (nomes de campo em ingles conforme a convencao). A identificacao do
   campo que falhou em validacoes `400` vai na `message` — a convencao nao define campo
   dedicado. **Revisado na Rodada 2 (2026-06-01):** o contrato enxuto
   `{ erro, campo }` foi descartado e o ADR-0001 (desvio) foi **REJEITADO** pelo usuario.
2. **Semantica de atualizacao:** `PUT` com substituicao completa (estrita). `descricao`
   ausente no corpo e limpa (`null`); `titulo` continua obrigatorio. **`status` e
   obrigatorio no `PUT`** (decisao OQ-1, Resolved 2026-06-01, opcao (b) — ver
   `plan.md` § Open Questions e `clarify_round3.md`): o default `pendente` so se aplica na
   criacao; `status` ausente no `PUT` retorna **400** (AC4.8), garantindo que nunca exista
   `status = null` no recurso apos um `PUT`.
3. **Nomenclatura:** portugues — rota `/tarefas`, campos `titulo`, `descricao`, `status`,
   `dataCriacao`. Confirmado.
4. **Limites de tamanho:** `titulo` maximo 200 caracteres; `descricao` maximo 2000
   caracteres. Exceder retorna **400 Bad Request** com o contrato de erro padronizado.

## Related PDRs

Nenhum. Nenhuma decisao desta feature conflita com a constituicao § Non-Goals; todos os
itens fora de escopo derivam diretamente dos nao-objetivos ja aprovados, sem necessidade
de PDR.
