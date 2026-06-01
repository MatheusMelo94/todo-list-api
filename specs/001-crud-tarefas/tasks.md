# todo-list-api — crud-tarefas Tasks v1

- **Status:** Ready for /implement
- **Date:** 2026-06-01
- **Plan:** [crud-tarefas Plan v1](./plan.md)
- **Spec:** [crud-tarefas Spec v1](./spec.md)
- **Baseline:** `architecture-conventions.md` (binding)

## Como usar

- Tarefas ordenadas; executar **uma de cada vez** (§ Engineering Workflow — one task at a
  time). Setup primeiro (repo vazio), depois TDD por camada.
- Granularidade: cada tarefa cabe em **um ciclo red-green-refactor** (≈ 30–90 min).
- Tarefas de produto/codigo seguem TDD: o teste (Red) faz parte da tarefa; o codigo
  minimo (Green) a satisfaz. Tarefas de setup/infra nao tem ciclo de teste proprio.
- Cada commit cita a(s) secao(oes) de convencao no corpo (§ Engineering Workflow —
  Conventional Commits) e lista `Tests: N passed`.
- Branch: `feat/001-crud-tarefas` (§ Repository Hygiene).

---

## Fase 0 — Setup do projeto (repo vazio)

### T01 — Bootstrap do projeto Spring Boot + Maven wrapper
- **O que:** Gerar a estrutura Maven single-module com Spring Boot 4.x, Java 17 e o Maven
  wrapper (`mvnw`/`mvnw.cmd`), com a classe `@SpringBootApplication` em
  `com.matheusmelo.todolist`.
- **Camada:** build / config
- **Convencao:** § Stack Baseline (Java 17, Spring Boot 4.x, Maven wrapper).
- **Criterios de aceite:**
  - `./mvnw -v` funciona; `./mvnw compile` compila a app vazia.
  - Pacote raiz `com.matheusmelo.todolist` criado.
- **Depende de:** —

### T02 — Declarar dependencias (web, data-mongodb, validation, test, flapdoodle embedded)
- **O que:** Adicionar ao `pom.xml`: `spring-boot-starter-web`,
  `spring-boot-starter-data-mongodb`, `spring-boot-starter-validation`,
  `spring-boot-starter-test`, e `de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x`
  (test scope) para Mongo embedded nos testes de integracao (OQ-2 Resolved). **Nao** incluir
  Testcontainers nem `spring-boot-starter-security` (constituicao § Non-Goals 1).
- **Camada:** build
- **Convencao:** § Stack Baseline (Spring Data MongoDB, Jakarta Validation); plano §
  Estrategia de testes (flapdoodle embedded); plano § Convencoes de escopo reduzido (sem
  Security).
- **Criterios de aceite:**
  - `./mvnw dependency:resolve` resolve sem erro.
  - flapdoodle embedded presente em test scope; Testcontainers **ausente**.
  - Security **ausente** do classpath.
- **Depende de:** T01

### T03 — Configuracao MongoDB env-driven + profiles
- **O que:** Criar `application.yml` lendo a URI Mongo via `${MONGODB_URI:...}` e definir
  profiles `local`/`docker`/`prod`. Criar `.env.example` com placeholders comentados e
  `.gitignore` cobrindo `.env` e `target/`.
- **Camada:** config
- **Convencao:** § Configuration & Secrets (env via `${VAR:default}`, profiles,
  `.env.example`); § Repository Hygiene (`.gitignore`).
- **Criterios de aceite:**
  - App sobe lendo `MONGODB_URI` do ambiente; default `local` funciona contra Mongo local.
  - `.env` ignorado pelo git; `.env.example` versionado.
- **Depende de:** T02

### T04 — Base de teste de integracao com Mongo embedded (flapdoodle)
- **O que:** Criar uma classe-base de teste (`@SpringBootTest` + `MockMvc`) usando o Mongo
  embedded auto-configurado pelo flapdoodle (OQ-2 Resolved). **Sem** Testcontainers, **sem**
  `@DynamicPropertySource`/URI dinamica, **sem** Docker. Garantir limpeza da colecao
  `tarefas` entre testes (ex.: `@BeforeEach` com `MongoTemplate.dropCollection`).
- **Camada:** test infra
- **Convencao:** § Engineering Workflow (suite via `./mvnw test`); plano § Estrategia de
  testes (Integration — flapdoodle embedded, sem Docker).
- **Criterios de aceite:**
  - Um teste-sentinela vazio sobe o contexto com o Mongo embedded e passa.
  - `./mvnw test` roda a suite **sem Docker daemon** disponivel.
  - Estado limpo entre testes (sem vazamento de dados entre casos).
- **Depende de:** T03

---

## Fase 1 — Modelo de dominio

### T05 — enum `StatusTarefa` com contrato JSON em minuscula
- **O que (Red):** Teste de (de)serializacao: `PENDENTE`/`CONCLUIDA` <-> `"pendente"`/
  `"concluida"`; valor desconhecido falha. **(Green):** criar `model/StatusTarefa` com a
  estrategia de serializacao em minuscula.
- **Camada:** model
- **Convencao:** § Package Layout (enums em `model/`); § API Contracts (contrato externo).
- **Criterios de aceite:**
  - Serializa `PENDENTE` -> `"pendente"`, `CONCLUIDA` -> `"concluida"`.
  - Deserializa `"pendente"`/`"concluida"`; valor invalido nao mapeia para enum valido.
- **Depende de:** T01

### T06 — Documento `Tarefa` (`@Document`, colecao `tarefas`)
- **O que (Red):** Teste mapeando os campos. **(Green):** criar `model/Tarefa` com `@Id
  String id`, `titulo`, `descricao`, `StatusTarefa status`, `Instant dataCriacao`,
  colecao `tarefas`.
- **Camada:** model
- **Convencao:** § Package Layout (entidades em `model/`); plano § Modelo de dados.
- **Criterios de aceite:**
  - Campos e tipos conforme plano; colecao `tarefas`.
  - `dataCriacao` do tipo `Instant`.
- **Depende de:** T05

---

## Fase 2 — Persistencia

### T07 — `TarefaRepository` (Spring Data MongoDB)
- **O que (Red):** Teste de integracao (base T04) salvando e recuperando uma `Tarefa`.
  **(Green):** criar `repository/TarefaRepository extends MongoRepository<Tarefa, String>`.
- **Camada:** repository
- **Convencao:** § Layer Rules (repository thin Spring Data).
- **Criterios de aceite:**
  - `save` + `findById` round-trip funciona contra o container.
  - Sem query customizada (CRUD padrao basta).
- **Depende de:** T06, T04

---

## Fase 3 — DTOs e mapper

### T08 — DTOs `TarefaCreateRequest`, `TarefaUpdateRequest`, `TarefaResponse` (records) + validacao Bean
- **O que (Red):** Testes de Bean Validation: em **ambos** os request DTOs (`@NotBlank`
  titulo, `@Size(200)` titulo, `@Size(2000)` descricao); e em `TarefaUpdateRequest`,
  `@NotNull` em `status` (PUT sem `status` -> violacao). **(Green):** criar os records em
  `dto/`: `TarefaCreateRequest` (`status` opcional), `TarefaUpdateRequest` (`status`
  `@NotNull`), `TarefaResponse` (OQ-1 Resolved — opcao (b), `status` obrigatorio no PUT).
- **Camada:** dto
- **Convencao:** § Layer Rules (DTOs records imutaveis); § API Contracts (validacao
  declarativa Jakarta).
- **Criterios de aceite:**
  - Em ambos os request DTOs: `titulo` em branco -> violacao `@NotBlank`; `titulo` > 200 ->
    violacao `@Size`; `descricao` > 2000 -> violacao `@Size`.
  - `TarefaUpdateRequest` com `status=null` -> violacao `@NotNull` (PUT sem `status` sera
    barrado em 400, AC4.8).
  - `TarefaCreateRequest` aceita `status=null` (default aplicado no service).
  - Nenhum DTO expoe `id`/`dataCriacao` como entrada do cliente.
- **Depende de:** T05

### T09 — `ErrorResponse` (corpo de erro padrao da convencao)
- **O que (Red):** Teste de shape: record com `timestamp`, `status`, `error`, `message`,
  `path` (nomes em ingles). **(Green):** criar `dto/ErrorResponse`.
- **Camada:** dto
- **Convencao:** § API Contracts (shape de erro padrao). ADR-0001 (desvio enxuto) REJEITADO
  — usar o shape completo.
- **Criterios de aceite:**
  - 5 campos com os nomes exatos da convencao, em ingles.
  - Sem campo dedicado de "campo que falhou" (vai na `message`).
- **Depende de:** T01

### T10 — `TarefaMapper` (entidade <-> DTO, ambos os sentidos)
- **O que (Red):** Teste unitario das conversoes: `TarefaCreateRequest` -> `Tarefa`
  (criacao), aplicacao de `TarefaUpdateRequest` sobre uma `Tarefa` existente (update), e
  `Tarefa` -> `TarefaResponse`. **(Green):** criar `mapper/TarefaMapper`.
- **Camada:** mapper
- **Convencao:** § Layer Rules (mapper em todo boundary entidade<->DTO, nunca bypass).
- **Criterios de aceite:**
  - `toEntity(create)` mapeia titulo/descricao/status; **nao** define `id`/`dataCriacao`.
  - update aplica titulo/descricao/status do `TarefaUpdateRequest` **preservando**
    `id`/`dataCriacao` da entidade existente.
  - `toResponse` mapeia todos os campos incluindo `id`/`dataCriacao`.
- **Depende de:** T06, T08

---

## Fase 4 — Excecoes e handler global

### T11 — `ResourceNotFoundException` (excecao de dominio)
- **O que (Red):** Teste verificando que o service a lanca para id inexistente (parcial;
  completado em T13/T15). **(Green):** criar `exception/ResourceNotFoundException`
  (runtime).
- **Camada:** exception
- **Convencao:** § Error Handling (tipos de excecao por dominio).
- **Criterios de aceite:**
  - Excecao runtime com mensagem de dominio (ex.: "Tarefa nao encontrada").
- **Depende de:** T01

### T12 — `GlobalExceptionHandler` (`@RestControllerAdvice`)
- **O que (Red):** Teste de integracao (via endpoint stub ou ja sobre T13) que assere o
  **corpo de erro completo** para 400 (`MethodArgumentNotValidException` +
  `HttpMessageNotReadableException`) e 404 (`ResourceNotFoundException`). **(Green):**
  criar `exception/GlobalExceptionHandler` mapeando as 3 excecoes para `ErrorResponse` com
  `timestamp`/`status`/`error`/`message`/`path`; `message` da validacao identifica o campo;
  loga sem vazar internals.
- **Camada:** exception
- **Convencao:** § Error Handling (handler global, sem try/catch de controle, sem vazamento
  de internals, log); § API Contracts (shape de erro).
- **Criterios de aceite:**
  - 400 retorna os 5 campos; `message` indica o campo (ex.: `titulo nao pode ser vazio`,
    `titulo excede 200 caracteres`, `status invalido`).
  - 404 retorna os 5 campos com `status=404`, `path` = URI requisitada.
  - Nenhuma resposta de erro contem stack trace, nome de DB ou path de classe.
- **Depende de:** T09, T11

---

## Fase 5 — Service (logica de negocio)

### T13 — `TarefaService.criar` + `listar` + `verPorId`
- **O que (Red):** Testes unitarios (repo mockado): `criar` gera `dataCriacao` e default
  `status=pendente` quando nulo; `listar` retorna todos (e `[]` vazio); `verPorId` retorna
  ou lanca `ResourceNotFoundException`. **(Green):** implementar em `service/TarefaService`.
- **Camada:** service
- **Convencao:** § Layer Rules (service orquestra logica); plano § HTTP->Camadas (1,2,3).
- **Criterios de aceite:**
  - `criar`: `dataCriacao = Instant.now()`, `status` default `PENDENTE` se request nulo
    (AC1.1, AC1.4).
  - `verPorId` inexistente -> `ResourceNotFoundException` (AC3.2).
  - `listar` sem dados -> lista vazia (AC2.2).
- **Depende de:** T07, T10, T11

### T14 — `TarefaService.atualizar` (semantica PUT — substituicao completa)
- **O que (Red):** Testes unitarios: id inexistente -> `ResourceNotFoundException`;
  `descricao` ausente -> `null`; `id` e `dataCriacao` preservados do registro original;
  `status` do `TarefaUpdateRequest` aplicado na entidade. **(Green):** implementar
  `atualizar(id, TarefaUpdateRequest)`.
- **Camada:** service
- **Convencao:** plano § Semantica PUT; § Layer Rules.
- **Criterios de aceite:**
  - `id` inexistente -> `ResourceNotFoundException` (AC4.2), nada alterado.
  - `descricao` ausente no request -> entidade salva com `descricao=null` (AC4.7).
  - `id` e `dataCriacao` inalterados apos update (AC4.1).
  - `status` do request aplicado na entidade salva. O service recebe `TarefaUpdateRequest`
    com `status` ja garantido nao-nulo pela validacao (`@NotNull`, OQ-1 Resolved) — nao ha
    default nem caminho de `status=null` no PUT.
- **Depende de:** T13

### T15 — `TarefaService.deletar`
- **O que (Red):** Testes unitarios: id existente -> delecao; id inexistente ->
  `ResourceNotFoundException`. **(Green):** implementar `deletar`.
- **Camada:** service
- **Convencao:** § Layer Rules; plano § HTTP->Camadas (5).
- **Criterios de aceite:**
  - id existente -> `deleteById` chamado (AC5.1).
  - id inexistente -> `ResourceNotFoundException` (AC5.2).
- **Depende de:** T13

---

## Fase 6 — Controller (REST, thin)

### T16 — `TarefaController` — `POST` e `GET` (lista + por id)
- **O que (Red):** Testes de integracao (MockMvc): `POST` valido -> 201 com corpo + header
  `Location`; `POST` invalido -> 400 (corpo de erro padrao); `GET /tarefas` -> 200 (array
  e `[]`); `GET /tarefas/{id}` existente -> 200, inexistente -> 404. **(Green):** criar
  `controller/TarefaController` com os 3 endpoints, `@Valid TarefaCreateRequest` no body do
  POST, retornando DTOs.
- **Camada:** controller
- **Convencao:** § Layer Rules (controller thin, recebe/retorna DTO, nunca entidade); §
  API Contracts (`@Valid`).
- **Criterios de aceite:**
  - AC1.1, AC1.2, AC1.3, AC1.4, AC1.5, AC1.6, AC1.7, AC2.1, AC2.2, AC3.1, AC3.2.
  - Controller sem logica de negocio (so delega ao service).
- **Depende de:** T12, T13

### T17 — `TarefaController` — `PUT` e `DELETE`
- **O que (Red):** Testes de integracao: `PUT` valido -> 200 (`id`/`dataCriacao` intactos);
  `PUT descricao` ausente -> 200 com `descricao=null`; **`PUT` sem `status` -> 400 (corpo de
  erro padrao, AC4.8)**; `PUT` invalido (titulo/limites/`status` invalido) -> 400; `PUT`
  inexistente -> 404; `DELETE` existente -> 204; `DELETE` + `GET` -> 404; `DELETE`
  inexistente -> 404. **(Green):** adicionar `PUT` (`@Valid TarefaUpdateRequest`) e `DELETE`
  ao controller.
- **Camada:** controller
- **Convencao:** § Layer Rules; § API Contracts.
- **Criterios de aceite:**
  - AC4.1–AC4.8, AC5.1, AC5.2, AC5.3.
  - `PUT` sem `status` -> 400 com corpo de erro padrao (AC4.8, OQ-1).
  - `DELETE` retorna 204 sem corpo.
- **Depende de:** T16, T14, T15

---

## Fase 7 — Fechamento

### T18 — Verificacao da suite + cobertura por endpoint
- **O que:** Rodar `./mvnw test` completo e confirmar **>= 1 teste feliz + >= 1 de erro por
  endpoint** (>= 10 total) — constituicao § Success Criteria criterio 2. Ajustar lacunas.
- **Camada:** test
- **Convencao:** § Engineering Workflow (suite verde); constituicao § Success Criteria 2.
- **Criterios de aceite:**
  - `./mvnw test` 100% verde.
  - Matriz feliz+erro por endpoint cumprida (plano § Estrategia de testes).
- **Depende de:** T17

### T19 — README minimo + CLAUDE.md
- **O que:** Criar `README.md` (proposito, stack, setup local, tabela de env vars, tabela
  de API com metodo+path+descricao, cURLs de exemplo) e `CLAUDE.md`. _(Dockerfile,
  docker-compose, run-local.sh e CI sao dominio do DevOps Engineer — fora deste escopo;
  ver plano OQ-3.)_
- **Camada:** docs
- **Convencao:** § Documentation.
- **Criterios de aceite:**
  - README com as 5 rotas e cURLs; tabela de env vars (`MONGODB_URI`).
  - Dados de exemplo sao placeholders sinteticos.
- **Depende de:** T18

---

## Open Questions herdadas do plano — Resolved (2026-06-01)

Decididas por Matheus em 2026-06-01. Sem desvio de convencao; sem ADR. Ja incorporadas
nas tarefas acima.

- **OQ-1 — RESOLVED: `status` OBRIGATORIO no PUT (`@NotNull`).** Dois request DTOs
  (`TarefaCreateRequest` opcional / `TarefaUpdateRequest` `@NotNull`). PUT sem `status` ->
  400 (AC4.8). Refletido em T08 (DTOs), T10 (mapper), T14 (service), T17 (controller +
  teste 400).
- **OQ-2 — RESOLVED: Mongo embedded (flapdoodle), sem Docker.** Refletido em T02
  (dependencia flapdoodle, sem Testcontainers) e T04 (base de teste embedded; suite roda
  sem Docker daemon).
- **OQ-3 — RESOLVED: manter MINIMO.** Confirmado: este `/tasks` **nao** contem nenhuma
  tarefa de Dockerfile/docker-compose/run-local.sh/CI. O minimo para rodar esta coberto
  por T01–T03 (build, `application.yml`, `.env.example`, `.gitignore`) e T19 (README minimo
  + `CLAUDE.md`). Artefatos DevOps ficam para o DevOps Engineer numa etapa futura
  (§ Multi-target Deployment). Nenhuma tarefa removida (nao havia tarefa de Docker/CI).
