# todo-list-api

API REST de CRUD de tarefas — referencia do fluxo spec-driven (Java 17 /
Spring Boot 3.5.x / MongoDB). Permite criar, listar, ver, atualizar e deletar
tarefas sobre uma unica colecao global, sem autenticacao.

> Nota de stack: este projeto fixa **Spring Boot 3.5.x** (e nao 4.x) por
> compatibilidade com o MongoDB embedded (flapdoodle) usado nos testes de
> integracao sem Docker. Decisao registrada em
> [`docs/adr/0002`](docs/adr/0002-fixar-spring-boot-3-5-x-para-compatibilidade-com-flapdoodle.md).

## Stack

- Java 17
- Spring Boot 3.5.x (Web, Data MongoDB, Validation)
- MongoDB
- Maven (wrapper `./mvnw`)
- Testes de integracao: MongoDB embedded via flapdoodle (sem Docker)

## Setup local

Pre-requisitos: JDK 17 e um MongoDB acessivel (default `localhost:27017`).

```bash
# 1. Configurar variaveis de ambiente
cp .env.example .env
# ajuste MONGODB_URI / SPRING_PROFILES_ACTIVE conforme seu ambiente

# 2. Rodar a suite de testes (nao requer Docker nem Mongo local — usa embedded)
./mvnw test

# 3. Subir a aplicacao (requer um MongoDB rodando)
export MONGODB_URI=mongodb://localhost:27017/todolist
./mvnw spring-boot:run
```

A aplicacao sobe por padrao em `http://localhost:8080`.

## Variaveis de ambiente

| Variavel                 | Obrigatoria | Default (profile `local`)              | Descricao                                   |
|--------------------------|-------------|----------------------------------------|---------------------------------------------|
| `MONGODB_URI`            | Sim (prod)  | `mongodb://localhost:27017/todolist`   | URI de conexao MongoDB.                     |
| `SPRING_PROFILES_ACTIVE` | Nao         | `local`                                | Profile ativo: `local` \| `docker` \| `prod`. |

No profile `prod`, `MONGODB_URI` nao tem default e deve ser fornecida pelo ambiente.

## API

Base path: `/tarefas`. Sem autenticacao.

| Metodo | Path             | Descricao                                              |
|--------|------------------|--------------------------------------------------------|
| POST   | `/tarefas`       | Cria uma tarefa. Retorna 201 + header `Location`.      |
| GET    | `/tarefas`       | Lista todas as tarefas. Retorna 200 (array, ou `[]`).  |
| GET    | `/tarefas/{id}`  | Retorna a tarefa pelo id. 200, ou 404 se inexistente.  |
| PUT    | `/tarefas/{id}`  | Substituicao completa da tarefa. 200, ou 404.          |
| DELETE | `/tarefas/{id}`  | Deleta a tarefa pelo id. 204, ou 404 se inexistente.   |

### Modelo

```json
{
  "id": "665a...",
  "titulo": "Comprar leite",
  "descricao": "Integral",
  "status": "pendente",
  "dataCriacao": "2026-06-01T12:00:00Z"
}
```

- `titulo`: obrigatorio, ate 200 caracteres.
- `descricao`: opcional, ate 2000 caracteres.
- `status`: `pendente` ou `concluida`. Opcional no POST (default `pendente`);
  **obrigatorio no PUT**.
- `id` e `dataCriacao`: gerados pelo servidor, nunca aceitos como entrada.

### Corpo de erro (padrao)

```json
{
  "timestamp": "2026-06-01T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Tarefa nao encontrada",
  "path": "/tarefas/665a..."
}
```

## Exemplos (cURL)

Dados de exemplo abaixo sao placeholders sinteticos.

```bash
# Criar
curl -i -X POST http://localhost:8080/tarefas \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Comprar leite","descricao":"Integral"}'

# Listar
curl http://localhost:8080/tarefas

# Ver por id
curl http://localhost:8080/tarefas/SEU_ID_AQUI

# Atualizar (PUT — substituicao completa; status obrigatorio)
curl -i -X PUT http://localhost:8080/tarefas/SEU_ID_AQUI \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Comprar leite e pao","descricao":"Integral","status":"concluida"}'

# Deletar
curl -i -X DELETE http://localhost:8080/tarefas/SEU_ID_AQUI
```

## Documentacao

- ADRs: [`docs/adr/`](docs/adr/)
- Spec / plano / tasks: [`specs/001-crud-tarefas/`](specs/001-crud-tarefas/)

> Dockerfile, docker-compose, `run-local.sh` e CI sao dominio do DevOps Engineer,
> fora do escopo desta feature (ver plano OQ-3).
