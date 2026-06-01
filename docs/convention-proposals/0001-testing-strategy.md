# Proposta de Convencao 0001: Nova secao § Testing Strategy

- **Status:** Proposed — aguardando aprovacao do usuario (Matheus Melo)
- **Date:** 2026-06-01
- **Autor:** Backend Architect
- **Baseline afetado:** `architecture-conventions.md` — adiciona uma nova secao
  `§ Testing Strategy` em Cross-Cutting, complementando `§ Engineering Workflow`
  (que ja codifica TDD mas nao niveis de teste, infraestrutura nem nomenclatura).

---

## Motivacao (gap observado)

O baseline codifica a disciplina de **como** se escreve teste (`§ Engineering Workflow`
— Red/Green/Refactor, "ambos unit e integration tests devem passar") mas nao codifica:

1. **Niveis de teste** — o que e um teste unitario vs. um teste de integracao, e como
   cada um decide se sobe ou nao contexto Spring / Mongo.
2. **Mecanismo de Mongo para testes de integracao** — o projeto `todo-list-api` adotou
   **Mongo embedded via flapdoodle** (sem Docker) como padrao, e essa escolha tem uma
   amarra de compatibilidade com a versao do Spring Boot que precisa ficar registrada
   para nao ser redescoberta em cada projeto.
3. **Nomenclatura `*Test` / `*IT`** — o projeto separou suites por sufixo de classe, e
   esse padrao deve se repetir.

Padroes ja praticados em `todo-list-api` que sustentam esta proposta:
- Sufixos consistentes: `TarefaServiceTest`, `TarefaMapperTest` (unit) vs.
  `TarefaRepositoryIT`, `TarefaControllerPostGetIT`, `ContextLoadsIT`,
  `GlobalExceptionHandlerIT` (integration).
- Base unica `AbstractMongoIntegrationTest` que sobe `@SpringBootTest` com Mongo
  embedded flapdoodle e limpa estado via `mongoTemplate.getDb().drop()` em `@BeforeEach`
  — zero Docker, zero URI dinamica.
- `pom.xml` usa o artefato `de.flapdoodle.embed.mongo.spring3x`, atrelado a Spring Boot
  3.5.x (ver ADR-0002).

---

## Texto atual no baseline

Hoje nao existe secao `§ Testing Strategy`. O unico tratamento de teste vive em
`§ Engineering Workflow → Test-Driven Development (TDD)`:

> **Suite check:** Run the full test suite (`./mvnw test`). Both unit and integration
> tests must pass.

Nao ha definicao de o que distingue unit de integration, nem de infraestrutura de
banco para integration, nem de nomenclatura de classes de teste.

---

## Texto proposto (diff em prosa)

**Adicionar** uma nova secao em Cross-Cutting, posicionada logo apos
`§ Engineering Workflow`, com o seguinte conteudo:

> ### Testing Strategy
>
> Complementa `§ Engineering Workflow` (TDD). Define niveis, infraestrutura e
> nomenclatura. A disciplina Red/Green/Refactor permanece inalterada.
>
> **Niveis de teste:**
> - **Unit (`*Test`):** exercita uma unica classe/colaboracao em isolamento. NAO sobe
>   contexto Spring nem banco. Service testado com repositorio mockado; mapper, DTO,
>   validacao e enums testados como POJOs. Rapido, deterministico, sem I/O.
> - **Integration (`*IT`):** exercita a integracao real entre camadas com contexto
>   Spring (`@SpringBootTest`) e MongoDB real embedded. Cobre repositorios (queries
>   reais), controllers ponta-a-ponta (via `MockMvc`), autoconfiguracao e o
>   `@RestControllerAdvice`.
>
> **Nomenclatura (obrigatoria):**
> - Classes de teste unitario terminam em `Test` (ex.: `TarefaServiceTest`).
> - Classes de teste de integracao terminam em `IT` (ex.: `TarefaRepositoryIT`).
> - O sufixo permite ao build separar suites (ex.: Surefire para `*Test`, Failsafe
>   para `*IT`) sem configuracao por classe.
>
> **Infraestrutura de integracao — Mongo embedded (padrao):**
> - Testes de integracao usam **MongoDB embedded via flapdoodle**
>   (`de.flapdoodle.embed.mongo.spring3x`), nao Docker. Criterio decisivo: a suite
>   (`./mvnw test`) roda em qualquer ambiente sem dependencia de Docker, mantendo
>   fidelidade contra um Mongo real (sem mocks de repositorio).
> - Padroniza-se uma classe base abstrata (ex.: `AbstractMongoIntegrationTest`) que
>   sobe o contexto e garante estado limpo entre testes (drop da base no `@BeforeEach`).
>
> **Compatibilidade Spring Boot ↔ flapdoodle (cuidado conhecido):**
> - O flapdoodle Mongo embedded e sensivel a major do Spring Boot. Spring Boot 4.x
>   relocou `MongoProperties`, quebrando a autoconfiguracao do flapdoodle, e (em
>   2026-06-01) **nao existe build de flapdoodle compativel com Boot 4**. O artefato
>   `...mongo.spring3x` exige a linha **Boot 3.x**.
> - Consequencia para o baseline (`§ Stack Baseline` = Boot 4.x): um projeto que adote
>   flapdoodle embedded precisa fixar Boot 3.x e registrar a divergencia via ADR. Ver o
>   precedente em `todo-list-api/docs/adr/0002-fixar-spring-boot-3-5-x-para-compatibilidade-com-flapdoodle.md`.
>   Reavaliar quando o flapdoodle publicar build para Boot 4.
>
> **Excecao — Testcontainers (via ADR):**
> - Testcontainers e a excecao, nao o padrao. Justifica-se via ADR quando: (a) o
>   ambiente ja garante Docker e abre-se mao do criterio "sem Docker"; (b) precisa-se
>   testar contra a major de Boot do baseline (4.x) sem o gargalo de compatibilidade do
>   flapdoodle; ou (c) precisa-se de uma versao/feature de MongoDB que o embedded nao
>   reproduz. Default permanece flapdoodle embedded.

---

## Justificativa

- **Codifica um padrao ja repetido**, evitando que cada projeto reinvente a separacao
  unit/integration e a escolha de infraestrutura de banco de teste.
- **Previne a redescoberta dolorosa** da incompatibilidade Boot 4 ↔ flapdoodle: hoje so
  esta capturada num ADR de projeto (ADR-0002); promove-la ao baseline transforma a
  armadilha em diretriz de stack.
- **Mantem a regra "sem desvio silencioso":** Testcontainers continua possivel, mas
  como excecao gated por ADR, alinhado a `§ Stack Baseline` ("Don't introduce new core
  technologies without an ADR").
- **A nomenclatura `*Test`/`*IT`** habilita separacao de suites no CI (Surefire vs.
  Failsafe) sem anotacao por classe.

## Escopo da mudanca

- Apenas adiciona uma secao; nao altera `§ Engineering Workflow`, `§ Stack Baseline`
  nem nenhuma secao existente.
- Decisao de aceitar/editar/recusar e exclusiva do usuario. Esta proposta NAO altera o
  baseline.

## Referencias

- `architecture-conventions.md § Engineering Workflow`, `§ Stack Baseline`
- `todo-list-api/docs/adr/0002-fixar-spring-boot-3-5-x-para-compatibilidade-com-flapdoodle.md`
- `todo-list-api/src/test/java/.../AbstractMongoIntegrationTest.java`
- `todo-list-api/pom.xml` (artefato `de.flapdoodle.embed.mongo.spring3x`)
