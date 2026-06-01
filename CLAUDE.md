# CLAUDE.md — todo-list-api

Contexto para desenvolvimento assistido por IA neste repositorio.

## O que e

API REST de CRUD de tarefas (`/tarefas`), sem autenticacao, sobre uma colecao
MongoDB global. Projeto de referencia do fluxo spec-driven.

## Stack e versoes

- Java 17, Spring Boot **3.5.x** (Web, Data MongoDB, Validation), Maven (`./mvnw`).
- MongoDB. Testes de integracao usam MongoDB embedded (flapdoodle), **sem Docker**.
- **Importante:** o projeto fixa Boot 3.5.x em vez do baseline 4.x do time, por
  compatibilidade com o flapdoodle. Ver `docs/adr/0002`. Nao reabrir essa decisao.

## Layout (per architecture-conventions.md § Package Layout)

```
com.matheusmelo.todolist/
  controller/   TarefaController (REST, thin)
  service/      TarefaService (logica de negocio)
  repository/   TarefaRepository (Spring Data MongoDB)
  dto/          TarefaCreateRequest, TarefaUpdateRequest, TarefaResponse, ErrorResponse (records)
  mapper/       TarefaMapper (entidade <-> DTO)
  model/        Tarefa (@Document), StatusTarefa (enum)
  exception/    ResourceNotFoundException, GlobalExceptionHandler (@RestControllerAdvice)
```

## Convencoes-chave deste codigo

- **Camadas:** controller thin (so delega, recebe/retorna DTO, nunca entidade);
  service orquestra; repository thin; mapper em todo boundary entidade<->DTO.
- **DTOs:** records imutaveis. `TarefaCreateRequest.status` opcional (default
  `PENDENTE` no service); `TarefaUpdateRequest.status` obrigatorio (`@NotNull`).
- **PUT = substituicao completa:** `descricao` ausente -> `null`; `id` e
  `dataCriacao` preservados.
- **Validacao:** Jakarta declarativa (titulo `@NotBlank` <=200, descricao <=2000).
- **Erros:** `@RestControllerAdvice` global; corpo padrao
  `{timestamp,status,error,message,path}`; sem vazar internals.
- **Contrato JSON do status:** minuscula (`pendente`/`concluida`).
- **Rota e campos em portugues** (`/tarefas`, `titulo`, `descricao`...).
- `id`/`dataCriacao` gerados pelo servidor, nunca aceitos como entrada.

## Workflow de desenvolvimento (per § Engineering Workflow)

- TDD estrito: red -> green -> refactor -> suite verde -> commit convencional.
- Uma tarefa por vez (ver `specs/001-crud-tarefas/tasks.md`).
- Commits citam a secao de convencao (e ADR quando relevante) no corpo.

## Comandos

```bash
source ~/tools/env.sh          # se JDK/Maven nao estiverem no PATH
./mvnw test                    # suite completa (unit + integracao, sem Docker)
./mvnw test -Dtest=ClassName   # uma classe
./mvnw spring-boot:run         # subir app (requer MONGODB_URI / Mongo rodando)
```

## Referencias

- Convencoes do time: `architecture-conventions.md` (fonte de verdade).
- Spec / plano / tasks: `specs/001-crud-tarefas/`.
- Decisoes: `docs/adr/` (0001 contrato de erro; 0002 pin Boot 3.5.x).
