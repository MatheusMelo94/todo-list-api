# ADR-0002: Fixar Spring Boot 3.5.x em vez de 4.x para compatibilidade com flapdoodle

- **Status:** Accepted (2026-06-01)
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo

## Context

A `architecture-conventions.md § Stack Baseline` fixa **Spring Boot 4.x** como baseline
de backend do time. Em paralelo, a decisao de teste deste projeto (OQ-2, resolvida em
`/clarify`) escolheu **Mongo embedded via flapdoodle** para os testes de integracao,
com criterio decisivo explicito: rodar a suite **sem dependencia de Docker**.

Durante a implementacao (T04), o Backend Engineer encontrou uma incompatibilidade
tecnica real entre essas duas decisoes ja firmadas:

1. **Spring Boot 4.x relocou `MongoProperties` de pacote.** A integracao do flapdoodle
   com o autoconfigure do Spring Data MongoDB depende da localizacao anterior dessa
   classe; com Boot 4 ela quebra.
2. **Nao existe build do flapdoodle compativel com Boot 4 (em 2026-06-01).** Os
   artefatos publicados do Mongo embedded so tem builds compativeis com a linha **Boot
   3.x**. Nao ha artefato flapdoodle para Boot 4 disponivel hoje.

O resultado e que `§ Stack Baseline` (Boot 4.x) e OQ-2 (flapdoodle embedded, sem Docker)
sao **mutuamente incompativeis** neste projeto. A decisao precisa ser tomada agora porque
a versao do Spring Boot e definida no `pom.xml` e condiciona todo o codigo de
autoconfiguracao, os testes de integracao (T04 em diante) e a forma da suite.

## Decision

Fixar **Spring Boot 3.5.x** (ultima linha 3.x) neste projeto `todo-list-api`,
preservando o criterio decisivo de OQ-2 (testes de integracao sem dependencia de Docker,
via flapdoodle embedded).

Esta decisao **supersede o pin "4.x" da `architecture-conventions.md § Stack Baseline`
apenas no escopo deste projeto**. O baseline do time permanece Boot 4.x para os demais
projetos; nao ha alteracao na convencao global por este ADR.

Especificamente:
- A versao de Spring Boot do projeto e fixada na linha 3.5.x (ultima 3.x disponivel) no
  `pom.xml` (o pin de versao e tarefa do Backend Engineer; este ADR nao altera codigo).
- O flapdoodle (Mongo embedded) permanece como mecanismo de teste de integracao,
  conforme OQ-2.

## Consequences

- **Positive:**
  - O flapdoodle funciona, mantendo a suite de integracao com **zero dependencia de
    Docker**, exatamente o criterio decisivo de OQ-2.
  - Fidelidade de teste preservada: os testes rodam contra um MongoDB real embedded, sem
    mocks de repositorio nem stubs do driver.
  - `MongoProperties` permanece na localizacao esperada pela autoconfiguracao, eliminando
    a incompatibilidade encontrada em T04.
- **Negative / Risco:**
  - O projeto fica **uma major atras** do baseline do time (`§ Stack Baseline` = Boot
    4.x), acumulando uma diferenca a ser reconciliada no futuro.
  - Recursos e correcoes exclusivos da linha 4.x nao estao disponiveis neste projeto.
  - **Revisitar quando o flapdoodle publicar build compativel com Boot 4.** Nesse momento,
    este ADR deve ser candidato a *Superseded* por um novo ADR que realinhe o projeto ao
    baseline 4.x do time.
- **Neutral:**
  - O desvio fica restrito a versao do framework; as demais convencoes de backend
    (layout de pacotes, camadas, error handling, auth) permanecem integralmente em vigor.
  - Caso de teste apenas: a constituicao trata este repositorio como exercicio de fluxo,
    o que limita o impacto operacional de ficar uma major atras.

## Alternatives Considered

- **Testcontainers + Spring Boot 4.x:** rejeitado. Resolveria a versao do framework e
  rodaria contra um MongoDB real, mas **exige Docker** no ambiente de teste, contrariando
  diretamente o criterio decisivo de OQ-2 (suite sem dependencia de Docker).
- **Shim Pre-Convention (camada de compatibilidade para reexpor `MongoProperties` na
  localizacao antiga sob Boot 4):** rejeitado. Fragil, acoplado a internals do
  autoconfigure, sem sancao da convencao e com alto risco de quebrar em patches da linha
  4.x. Aumentaria o custo de manutencao sem ganho sobre simplesmente fixar 3.5.x.
- **Aguardar build do flapdoodle para Boot 4:** rejeitado. **Inexistente em 2026-06-01.**
  Bloquearia T04 e toda a suite de integracao por tempo indeterminado. Fica, no entanto,
  registrado como gatilho de revisao futura (ver § Consequences — possivel superseding).

## References

- `architecture-conventions.md § Stack Baseline` (baseline Boot 4.x — superseded apenas
  no escopo deste projeto)
- OQ-2 (decisao de Mongo embedded via flapdoodle, sem Docker, resolvida em `/clarify`)
- T04 (tarefa de implementacao em que a incompatibilidade foi descoberta)
- `docs/adr/0001-contrato-de-erro-simplificado.md` (precedente de ADR com escopo
  restrito a este projeto)
