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
| `CORS_ALLOWED_ORIGINS`   | Nao         | `http://localhost:3000`                | Allowlist de origens CORS, separadas por virgula. **Nunca `*`** (PDR-0003). |

No profile `prod`, `MONGODB_URI` nao tem default e deve ser fornecida pelo ambiente.

## API

Base path: `/tarefas`. Sem autenticacao.

| Metodo | Path             | Descricao                                              |
|--------|------------------|--------------------------------------------------------|
| POST   | `/tarefas`       | Cria uma tarefa. Retorna 201 + header `Location`.      |
| GET    | `/tarefas`       | Lista paginada. `?page=` (default 0), `?size=` (default 20, max 100). Retorna 200 com envelope `PageResponse`. `page`/`size` invalidos -> 400. |
| GET    | `/tarefas/{id}`  | Retorna a tarefa pelo id. 200, ou 404 se inexistente.  |
| PUT    | `/tarefas/{id}`  | Substituicao completa da tarefa. 200, ou 404.          |
| DELETE | `/tarefas/{id}`  | Deleta a tarefa pelo id. 204, ou 404 se inexistente.   |

### Hardening de seguranca (spec 002)

- **Headers de seguranca:** toda resposta carrega `Strict-Transport-Security`,
  `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` e
  `Referrer-Policy: strict-origin-when-cross-origin` (PDR-0003). Sem CSP/Permissions-Policy
  (API sem frontend).
- **CORS:** allowlist explicita via `CORS_ALLOWED_ORIGINS` (nunca `*`).
- **Rate limiting:** endpoints de escrita (`POST`/`PUT`/`DELETE` de `/tarefas`) limitados a
  60 requisicoes por IP por 60s, por endpoint. Excedeu -> **429** (shape de erro padrao) +
  header `Retry-After`. Leitura nao e limitada (PDR-0001 / ADR-0003).
- **Auditoria:** cada mutacao bem-sucedida emite um log estruturado em INFO
  (`operacao`, `tarefaId`, `outcome`, `timestamp`), sem dados sensiveis (PDR-0002).
- **Sem autenticacao:** Spring Security entra apenas para headers + CORS; todos os
  endpoints permanecem anonimos (Non-Goal #1).

### Resposta paginada (`GET /tarefas`)

```json
{
  "content": [ { "id": "665a...", "titulo": "Comprar leite", "descricao": "Integral", "status": "pendente", "dataCriacao": "2026-06-01T12:00:00Z" } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

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

# Listar (paginado)
curl "http://localhost:8080/tarefas?page=0&size=20"

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

- ADRs: [`docs/adr/`](docs/adr/) — inclui ADR-0003 (Bucket4j) e ADR-0004 (`PageResponse`).
- PDRs: [`docs/pdr/`](docs/pdr/) — 0001 (rate limiting), 0002 (auditoria), 0003 (headers/CORS), 0004 (paginacao).
- Spec / plano / tasks:
  [`specs/001-crud-tarefas/`](specs/001-crud-tarefas/),
  [`specs/002-hardening-seguranca/`](specs/002-hardening-seguranca/)

> Dockerfile, docker-compose, `run-local.sh` e CI sao dominio do DevOps Engineer,
> fora do escopo desta feature (ver plano OQ-3).
