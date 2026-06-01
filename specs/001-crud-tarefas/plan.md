# todo-list-api — crud-tarefas Plan v1

- **Status:** Ready for /tasks
- **Date:** 2026-06-01
- **Confidence:** Full (deriva de spec `/specify` + `/clarify` resolvido, status *Ready for /plan*)
- **Spec:** [crud-tarefas Spec v1](./spec.md)
- **Constitution:** [todo-list-api Constitution v1](../../memory/constitution.md)
- **Baseline:** `architecture-conventions.md` (binding)

## Source

Spec-Kit `/specify` output (`specs/001-crud-tarefas/spec.md`), com `/clarify` resolvido
em 2 rodadas (2026-06-01). O contrato de erro foi fixado no **formato completo da
convencao** apos a **rejeicao do ADR-0001** (contrato enxuto `{ erro, campo }` descartado).
Nao ha desvio de convencao pendente neste plano.

## Architecture Overview

API REST monolitica Spring Boot, single-module Maven, persistindo em uma colecao MongoDB
global `tarefas`. Componente unico (sem integracoes externas, sem mensageria, sem auth).
Fluxo de cada requisicao segue o pipeline de camadas da convencao:

```
HTTP  ->  Controller  ->  Service  ->  Repository  ->  MongoDB
              |  (DTO)       |  (entidade)
           Mapper  <-------- Service
              |
           DTO de resposta  ->  HTTP

Erros: qualquer camada lanca excecao -> @RestControllerAdvice -> corpo de erro padrao
```

### Camadas e responsabilidades

Pacote raiz: `com.matheusmelo.todolist` (per `architecture-conventions.md § Package Layout`).

| Pacote        | Artefato                          | Responsabilidade                                                                 |
|---------------|-----------------------------------|---------------------------------------------------------------------------------|
| `controller/` | `TarefaController`                 | Endpoints REST, thin, sem logica de negocio (§ Layer Rules)                       |
| `service/`    | `TarefaService`                   | Toda a logica de negocio: geracao de defaults, semantica PUT, lancar 404 (§ Layer Rules) |
| `repository/` | `TarefaRepository`                | `MongoRepository<Tarefa, String>` (§ Layer Rules — thin Spring Data)             |
| `model/`      | `Tarefa` (`@Document`), `StatusTarefa` (enum) | Entidade de dominio + enum (§ Package Layout)                          |
| `dto/`        | `TarefaCreateRequest`, `TarefaUpdateRequest`, `TarefaResponse`, `ErrorResponse` | Contratos publicos, records imutaveis (§ Layer Rules); create/update separados por regra de `status` (OQ-1) |
| `mapper/`     | `TarefaMapper`                    | Conversao entidade <-> DTO em ambos os sentidos (§ Layer Rules — nunca bypass)   |
| `exception/`  | `ResourceNotFoundException`, `GlobalExceptionHandler` | Excecao de dominio + handler global (§ Error Handling)        |
| `config/`     | (MongoDB via Spring Boot auto-config; sem beans extras no v1) | Config Spring (§ Package Layout)                       |

### Convencoes de escopo reduzido (inline, sem ADR)

Reducoes derivadas da constituicao § Non-Goals — documentadas aqui, **nao** sao desvios de
convencao (sao nao-aplicacao de secoes da convencao por escopo do projeto):

- **Authentication & Authorization (§):** NAO aplicada. Constituicao § Non-Goals 1 (sem
  login/tokens/papeis). Sem Spring Security no classpath; nenhum endpoint protegido.
- **CORS (§):** NAO configurado. Constituicao § Non-Goals 6 (sem frontend) — nao ha origem
  de browser a permitir no v1.
- **Logging — Correlation IDs (§):** NAO implementados. Constituicao § Constraints exclui
  observabilidade avancada do v1. SLF4J/Logback fica disponivel por padrao do Spring Boot;
  o requisito de "todas as excecoes logadas com correlation IDs" da § Error Handling fica
  reduzido a log simples no handler global, sem propagacao de correlation ID.
- **Multi-tenant / tenant isolation (§ Auth):** NAO aplicado. Constituicao § Non-Goals 2
  (colecao global unica).

Se qualquer um destes voltar ao escopo (ex.: adicionar frontend), reabrir como nova spec;
a convencao ja cobre o padrao.

## HTTP -> Camadas por endpoint

Base path: `/tarefas`. Versionamento de rota (`/v1/`) **nao** aplicado no v1 — a convencao
§ API Contracts versiona "quando ocorrem breaking changes"; greenfield sem consumidores nao
exige `/v1/` agora. (Anotado como item de drift ao final.)

| # | Metodo + rota          | Sucesso | Controller faz                          | Service faz                                                                 | Erros                                  |
|---|------------------------|---------|-----------------------------------------|----------------------------------------------------------------------------|----------------------------------------|
| 1 | `POST /tarefas`        | 201     | Recebe `@Valid TarefaCreateRequest`, retorna `TarefaResponse` + `Location` | Mapeia para `Tarefa`, gera `id`+`dataCriacao`, default `status=pendente` se nulo, salva | 400 (validacao Bean)                   |
| 2 | `GET /tarefas`         | 200     | Retorna `List<TarefaResponse>`          | `findAll()`, mapeia. Lista vazia -> `[]`                                    | —                                      |
| 3 | `GET /tarefas/{id}`    | 200     | Retorna `TarefaResponse`                | `findById` ou lanca `ResourceNotFoundException`                             | 404                                    |
| 4 | `PUT /tarefas/{id}`    | 200     | Recebe `@Valid TarefaUpdateRequest`, retorna `TarefaResponse` | Carrega existente (ou 404), **substitui completamente** preservando `id`+`dataCriacao`, salva (`status` ja garantido nao-nulo pela validacao) | 400 (validacao, inclui `status` ausente), 404 |
| 5 | `DELETE /tarefas/{id}` | 204     | Sem corpo                               | Verifica existencia (ou 404), `deleteById`                                  | 404                                    |

## Modelo de dados — documento `Tarefa`

Colecao MongoDB: `tarefas`. Entidade `@Document` em `model/` (§ Package Layout).

| Campo         | Tipo Java       | Mapeamento Mongo        | Regra                                                                 |
|---------------|-----------------|-------------------------|----------------------------------------------------------------------|
| `id`          | `String`        | `@Id` (ObjectId string) | Gerado pelo Mongo na insercao; imutavel. Nunca recebido do cliente.  |
| `titulo`      | `String`        | campo                   | Obrigatorio, <= 200 chars                                             |
| `descricao`   | `String`        | campo (nullable)        | Opcional, <= 2000 chars                                               |
| `status`      | `StatusTarefa`  | campo (string)          | enum `PENDENTE` / `CONCLUIDA`; default `PENDENTE`                     |
| `dataCriacao` | `Instant`       | campo                   | Definido pelo service na criacao; **imutavel** (nunca sobrescrito no PUT) |

### Geracao de `id` e `dataCriacao` (imutavel)

- `id`: gerado pelo MongoDB (`@Id String`, sem valor no insert -> ObjectId gerado e
  serializado como string). O DTO de request **nao** contem `id`; o cliente nunca o envia.
- `dataCriacao`: definido no `TarefaService.criar()` como `Instant.now()` no momento da
  criacao. No `TarefaService.atualizar()`, a tarefa carregada do banco mantem o
  `dataCriacao` original — o mapper de atualizacao **nao** toca nesse campo. Mesmo se um
  cliente enviar `dataCriacao` no corpo, ela e ignorada (nao existe no `TarefaRequest`).
  Garante AC4.1 (`id` e `dataCriacao` inalterados no PUT).

### enum `StatusTarefa`

- Valores internos: `PENDENTE`, `CONCLUIDA`.
- Contrato externo (JSON): `pendente` / `concluida` (minusculas, per spec § Modelo de
  Dados). Estrategia de serializacao: enum com valor JSON em minuscula via `@JsonValue` na
  representacao, **ou** deserializacao case-insensitive — a definir na implementacao,
  mantendo o contrato externo em minusculas. Valor invalido na deserializacao -> tratado
  como erro de validacao 400 (ver Estrategia de validacao).

## DTOs (records imutaveis — § Layer Rules)

### `TarefaRequest` (entrada — POST e PUT)

**Decisao OQ-1 (Resolved 2026-06-01): dois DTOs de request distintos.** Como `status` e
opcional no POST (default `pendente`) mas **obrigatorio** no PUT (`@NotNull`, substituicao
completa estrita), um unico record nao consegue carregar as duas regras de validacao do
mesmo campo. Separa-se em `TarefaCreateRequest` (POST) e `TarefaUpdateRequest` (PUT):

```
// POST — status opcional (null -> default PENDENTE no service)
record TarefaCreateRequest(
    @NotBlank @Size(max = 200) String titulo,
    @Size(max = 2000) String descricao,
    StatusTarefa status        // opcional; null -> default PENDENTE no service
)

// PUT — status obrigatorio (substituicao completa estrita; nunca status=null no banco)
record TarefaUpdateRequest(
    @NotBlank @Size(max = 200) String titulo,
    @Size(max = 2000) String descricao,
    @NotNull StatusTarefa status   // OBRIGATORIO; ausente -> 400
)
```

- Sem `id` nem `dataCriacao` em nenhum dos dois — campos de sistema, nunca recebidos do
  cliente.
- `titulo`/`descricao` seguem as mesmas regras de validacao nos dois DTOs. A unica
  diferenca e `status`: opcional no create, `@NotNull` no update.
- PUT sem `status` no corpo -> `MethodArgumentNotValidException` -> 400 (handler global).
  Documentos com `status=null` via PUT tornam-se impossiveis (decisao OQ-1 opcao (b)).

### `TarefaResponse` (saida)

```
record TarefaResponse(
    String id,
    String titulo,
    String descricao,
    StatusTarefa status,
    Instant dataCriacao
)
```

### `ErrorResponse` (saida de erro — corpo padrao da convencao)

Per `architecture-conventions.md § API Contracts` (shape de 5 campos, nomes em ingles):

```
record ErrorResponse(
    Instant timestamp,   // data-hora da ocorrencia
    int status,          // codigo HTTP numerico (400 | 404)
    String error,        // rotulo HTTP: "Bad Request" | "Not Found"
    String message,      // mensagem legivel; em validacao 400, identifica o campo que falhou
    String path          // URI da requisicao (ex.: /tarefas/{id} resolvido)
)
```

- **Decisao registrada (ADR-0001 REJEITADO):** este e o contrato definitivo. A
  identificacao do campo que falhou vai na `message` — a convencao nao define campo
  dedicado (spec § Contrato de erro padronizado).
- Sem detalhes internos (sem stack trace, nome de DB, path de classe) per § Error Handling.

## Exception handling global — `@RestControllerAdvice`

`GlobalExceptionHandler` em `exception/` (§ Error Handling — handler global, nunca
`try/catch` para controle de fluxo em controllers). Mapeamentos:

| Excecao capturada                              | Origem                                  | HTTP | `error`       | `message`                                                                 |
|------------------------------------------------|-----------------------------------------|------|---------------|---------------------------------------------------------------------------|
| `MethodArgumentNotValidException`              | `@Valid` falhou (`@NotBlank`, `@Size`)  | 400  | `Bad Request` | Derivada do primeiro `FieldError`: nome do campo + violacao (ex.: `titulo nao pode ser vazio`, `titulo excede 200 caracteres`) |
| `HttpMessageNotReadableException`              | JSON malformado / enum `status` invalido | 400  | `Bad Request` | Mensagem generica indicando corpo invalido; se causada por enum, `status invalido` |
| `ResourceNotFoundException` (custom de dominio)| Service nao achou o `id`                 | 404  | `Not Found`   | ex.: `Tarefa nao encontrada`                                              |

- `timestamp` = `Instant.now()` no handler. `path` = `request.getRequestURI()` (resolvido,
  ex.: `/tarefas/abc123`). `status` = codigo numerico correspondente.
- Cada handler **loga** a excecao (SLF4J) sem expor internals ao cliente (§ Error
  Handling). Sem correlation ID no v1 (escopo reduzido — ver acima).
- `ResourceNotFoundException` e a unica excecao de dominio customizada necessaria no v1
  (§ Error Handling — tipos de excecao por dominio).

## Estrategia de validacao (Bean Validation — § API Contracts)

Validacao declarativa via Jakarta annotations (§ API Contracts — "Validation is
declarative via Jakarta annotations"). Sem validadores customizados (regras cabem nas
anotacoes padrao).

| Regra (spec)                          | Anotacao                | AC coberto              |
|---------------------------------------|-------------------------|-------------------------|
| `titulo` obrigatorio, nao vazio/branco| `@NotBlank`             | AC1.2, AC4.3            |
| `titulo` <= 200 caracteres            | `@Size(max = 200)`      | AC1.5, AC4.5            |
| `descricao` <= 2000 caracteres        | `@Size(max = 2000)`     | AC1.6, AC4.6            |
| `status` so `pendente`/`concluida`    | tipagem do enum `StatusTarefa` + falha de deserializacao -> 400 | AC1.3, AC4.4 |
| `status` obrigatorio no PUT (OQ-1)    | `@NotNull` em `TarefaUpdateRequest.status` -> 400 se ausente | AC4.8 (PUT sem `status`) |

- Controllers anotam o body com `@Valid`; violacoes disparam
  `MethodArgumentNotValidException` -> handler global -> 400 com `message` identificando o
  campo (AC1.7).
- `status` invalido nao e um Bean Validation tipico: chega como falha de deserializacao do
  enum (`HttpMessageNotReadableException`). O handler global a converte em 400 com `message`
  indicando `status invalido` (AC1.3, AC4.4).

## Semantica PUT — substituicao completa

Per spec § Semantica de atualizacao (PUT estrita):

- `PUT /tarefas/{id}` faz **substituicao completa** do recurso.
- Recebe `TarefaUpdateRequest` (`status` `@NotNull` — OQ-1 Resolved).
- Fluxo no `TarefaService.atualizar(id, request)`:
  1. `findById(id)` -> se ausente, lanca `ResourceNotFoundException` (404, AC4.2).
  2. Carrega a tarefa existente apenas para preservar `id` e `dataCriacao`.
  3. Sobrescreve `titulo`, `descricao`, `status` com os valores do request.
  4. **`descricao` ausente no corpo -> `null`** (limpa; o valor anterior NAO e preservado).
     Isso e consequencia direta da substituicao completa (AC4.7).
  5. `status` **e obrigatorio** no PUT: `@NotNull` em `TarefaUpdateRequest.status` garante
     que o request invalido (sem `status`) e barrado na validacao (400) **antes** de chegar
     ao service. Portanto o service nunca recebe `status=null`; nao ha default no PUT e
     documentos com `status=null` via PUT sao impossiveis (OQ-1, Resolved 2026-06-01,
     opcao (b) — substituicao completa estrita).
  6. `id` e `dataCriacao` permanecem os carregados do banco (AC4.1).
  7. Salva e retorna `TarefaResponse`.

## Estrategia de testes (unit + integration)

Cumpre constituicao § Success Criteria criterio 2: **>= 1 teste de caminho feliz + >= 1 de
erro por endpoint** (total >= 10). TDD per `architecture-conventions.md § Engineering
Workflow` (red-green-refactor por tarefa).

### Camadas de teste

- **Unit (service + mapper):** `TarefaServiceTest` com repository mockado (Mockito).
  Cobre logica de negocio isolada: geracao de defaults, semantica PUT (descricao -> null,
  imutabilidade de `id`/`dataCriacao`), lancamento de `ResourceNotFoundException`.
  `TarefaMapperTest` cobre conversoes entidade <-> DTO.
- **Integration (web + Mongo):** `TarefaControllerIT` com `@SpringBootTest` +
  `MockMvc`, persistencia via **Mongo embedded (flapdoodle)** — OQ-2 Resolved (2026-06-01).
  Dependencia `de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x` (test scope); o
  Spring Boot auto-configura uma instancia embedded para os testes (sem URI dinamica,
  sem container). **Sem dependencia de Docker** para rodar a suite — `./mvnw test` roda
  em qualquer ambiente (incl. CI sem Docker daemon). Valida HTTP status, corpo de resposta
  e — crucialmente — o **corpo de erro padrao** (`timestamp`/`status`/`error`/`message`/
  `path`) end-to-end.

  > **Trade-off registrado (OQ-2):** flapdoodle baixa/executa um binario do Mongo, com
  > fidelidade ligeiramente menor que um Mongo real (Testcontainers). Aceitavel para o
  > escopo CRUD do v1 (sem features dependentes de versao/engine). Eliminar a dependencia
  > de Docker foi o criterio decisivo. Se features futuras exigirem fidelidade total,
  > reabrir via § Testing Strategy (ver drift proposal #2).

### Matriz minima (feliz + erro por endpoint)

| Endpoint              | Teste feliz                                  | Teste(s) de erro                                                   |
|-----------------------|----------------------------------------------|-------------------------------------------------------------------|
| `POST /tarefas`       | 201, corpo com `id`/`status=pendente`/`dataCriacao` (AC1.1) | 400 `titulo` ausente (AC1.2); 400 limites/`status` invalido (AC1.3/1.5/1.6) |
| `GET /tarefas`        | 200 com array (AC2.1); 200 `[]` vazio (AC2.2)| —                                                                 |
| `GET /tarefas/{id}`   | 200 tarefa existente (AC3.1)                  | 404 id inexistente + corpo de erro padrao (AC3.2)                 |
| `PUT /tarefas/{id}`   | 200 atualizada, `id`/`dataCriacao` intactos (AC4.1); 200 `descricao` -> null (AC4.7) | 404 (AC4.2); 400 `titulo` vazio (AC4.3); 400 `status`/limites (AC4.4/4.5/4.6); **400 `status` ausente (AC4.8)** |
| `DELETE /tarefas/{id}`| 204 (AC5.1); GET pos-delete -> 404 (AC5.3)   | 404 id inexistente (AC5.2)                                        |

- Pelo menos 1 teste de integracao assere o **shape completo do corpo de erro** para 400
  e para 404 (presenca dos 5 campos com nomes em ingles), validando o contrato da convencao.
- Comando da suite: `./mvnw test` (unit + integration) per § Engineering Workflow.

## Convention Alignment

Mapeamento decisao -> secao da convencao (decisoes que **seguem** o baseline):

- **Stack:** Java 17 + Spring Boot 4.x + Spring Data MongoDB + Jakarta Validation + Maven
  wrapper + SLF4J/Logback -> § Stack Baseline.
- **Package layout** `com.matheusmelo.todolist/{config,controller,dto,exception,mapper,model,repository,service}`
  -> § Package Layout.
- **Controllers thin, sem logica; service orquestra; repository thin Spring Data; mapper em
  todo boundary; DTOs como records imutaveis** -> § Layer Rules.
- **Validacao declarativa Jakarta (`@NotBlank`/`@Size`)** -> § API Contracts.
- **Corpo de erro padrao (`timestamp`/`status`/`error`/`message`/`path`)** -> § API
  Contracts. (ADR-0001 que propunha desvio foi REJEITADO.)
- **Handler global `@RestControllerAdvice`, excecao de dominio `ResourceNotFoundException`,
  sem vazamento de internals** -> § Error Handling.
- **Config env-driven (`application.yml` com `${VAR:default}`), profiles `local`/`docker`/
  `prod`, `.env.example`** -> § Configuration & Secrets.
- **`.gitignore` cobrindo `.env`/`target/`, conventional commits, branch `feat/001-...`**
  -> § Repository Hygiene.
- **TDD red-green-refactor, `./mvnw test`, commits com citacao de convencao** -> §
  Engineering Workflow.
- **`README.md` (setup, env table, API table, cURL) + `CLAUDE.md` + `docs/`** -> §
  Documentation. (Producao de README/CLAUDE/deploy configs sai do escopo deste `/plan` de
  feature; Dockerfile/compose/CI sao do DevOps Engineer — OQ-3 Resolved.)

## Deviations

**Nenhuma.** O unico desvio proposto (contrato de erro enxuto) foi formalizado no ADR-0001
e **REJEITADO** pelo usuario em 2026-06-01. O plano segue `architecture-conventions.md`
integralmente. As nao-aplicacoes de § Auth / § CORS / § Correlation IDs / § Multi-tenant
sao **reducoes de escopo** da constituicao § Non-Goals (documentadas em "Convencoes de
escopo reduzido"), nao desvios de convencao — nao requerem ADR.

## Open Questions — Resolved (2026-06-01)

As tres questoes foram decididas por Matheus em 2026-06-01. Nenhuma introduz desvio de
convencao (verificado contra `architecture-conventions.md`); nao foi necessario ADR.

- **OQ-1 (PUT com `status` ausente) — RESOLVED: opcao (b), `status` OBRIGATORIO no PUT.**
  `status` e `@NotNull` no `TarefaUpdateRequest` (substituicao completa estrita). PUT sem
  `status` -> 400. Documentos com `status=null` via PUT tornam-se impossiveis. Impacto:
  DTO de update separado do de create (ver § DTOs); service `atualizar` nunca recebe
  `status=null` (ver § Semantica PUT); novo criterio de teste AC4.8 (PUT sem `status` ->
  400). Tarefas afetadas: T08, T14, T17. Alinhado a § API Contracts (validacao declarativa
  Jakarta).
- **OQ-2 (estrategia de testes Mongo) — RESOLVED: Mongo embedded (flapdoodle).** Testes de
  integracao usam flapdoodle embedded, **nao** Testcontainers. Sem dependencia de Docker
  para `./mvnw test`. Impacto: dependencia e setup de teste mudam (ver § Estrategia de
  testes). Tarefas afetadas: T02 (dependencia), T04 (base de teste). Area nao codificada na
  convencao — reforça a drift proposal #2 (§ Testing Strategy).
- **OQ-3 (escopo de artefatos DevOps) — RESOLVED: manter MINIMO.** Este plano inclui apenas
  o minimo para rodar (build Maven, `application.yml`, `.env.example`, `.gitignore`, README
  minimo, `CLAUDE.md`). Dockerfile, docker-compose, run-local.sh e CI workflows ficam
  **fora** deste plano — dominio do DevOps Engineer numa etapa futura (consistente com §
  Multi-target Deployment, que atribui esses artefatos ao DevOps Engineer). Confirmado que
  o `/tasks` ja reflete isso (sem tarefas de Docker/CI).

## Convention Drift — propostas (nao aplicadas; usuario aprova)

1. **Versionamento de rota desde o dia 1:** a convencao § API Contracts versiona rotas
   "quando ocorrem breaking changes". Em greenfield, decidir entre comecar ja com `/v1/`
   (custo zero agora, evita migracao depois) ou so na primeira breaking change. Sugestao:
   adicionar uma diretriz explicita a § API Contracts. Quer que eu rascunhe?
2. **Convencao de estrategia de testes (reforçada por OQ-2):** a convencao tem §
   Engineering Workflow (TDD) mas nao codifica **niveis de teste** (unit vs integration),
   o **padrao de Mongo em teste** (flapdoodle embedded vs Testcontainers), nem convencao de
   nomenclatura (`*Test` vs `*IT`). A decisao OQ-2 (flapdoodle embedded, sem Docker) deveria
   virar baseline para nao re-decidir a cada projeto. Recorrente em todo projeto do stack.
   Sugestao: adicionar § Testing Strategy ao baseline, codificando flapdoodle embedded como
   padrao de integracao Mongo (com Testcontainers como excecao justificada por ADR quando
   houver dependencia de versao/engine). Quer que eu rascunhe?
3. **Mapeamento enum interno <-> JSON:** o contrato externo em portugues minusculo
   (`pendente`) vs enum Java maiusculo (`PENDENTE`) e um padrao que vai se repetir. Sugestao:
   diretriz curta em § API Contracts sobre serializacao de enums. Quer que eu rascunhe?
