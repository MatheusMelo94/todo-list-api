# todo-list-api Constitution v2

- **Status:** Accepted
- **Date:** 2026-06-01
- **Accepted:** 2026-06-01 por Matheus Melo
- **Deciders:** Matheus Melo (desenvolvedor solo, autoridade única de produto)
- **Supersedes:** Constitution v1 (2026-06-01) — ver § Histórico de Versões ao final.
- **Motivo da emenda:** Remediação de segurança solicitada por Matheus Melo (2026-06-01).
  O usuário recusou aceitar o risco das findings F-0002, F-0003, F-0006 e F-0007
  (`docs/reviews/security-findings-001.md`) e decidiu expandir o escopo do projeto para
  remediá-las **in-app** (sem infra de deploy / edge disponível — roda em localhost). A
  expansão segue o portão da própria constituição (§ Constraints v1: "qualquer expansão
  além do CRUD básico deve passar por um PDR antes de entrar em um spec") e está embasada
  nos PDRs `docs/pdr/0001`..`0004` (todos Accepted 2026-06-01).

> Per `product-conventions.md § Constitution Shape`: constituições são imutáveis após
> *Accepted*; emendas criam uma nova versão. Esta é a v2, que **supersede** a v1. O
> registro da v1 é preservado em § Histórico de Versões para rastreabilidade.

## Users

- **Usuário primário:** o próprio desenvolvedor solo (Matheus), atuando como autor e
  consumidor do projeto. Não há usuários finais externos.
- **Papel concreto:** desenvolvedor exercitando o stack Java / Spring Boot / MongoDB e
  o fluxo de desenvolvimento orientado a spec (spec -> plano -> implementacao).

> Nota (Spec Quality Bar): este projeto declara explicitamente nao ter usuarios finais
> reais. O "usuario" e o desenvolvedor. Isso e aceito porque o proposito declarado e
> aprendizado/exercicio de fluxo, nao atendimento de uma dor de mercado.

## Problem

O desenvolvedor precisa de uma base de teste limpa e de escopo controlado para exercitar
o fluxo completo de desenvolvimento orientado a spec (constituicao -> especificacao ->
plano -> implementacao -> testes) sobre o stack padrao do time. Sem um projeto de
referencia enxuto, o fluxo so e exercitado sobre projetos reais (com ruido de escopo e
risco de producao), o que dificulta validar e refinar o proprio processo.

Este projeto NAO resolve uma dor de mercado. Seu valor e instrumental: servir de
referencia de processo.

**Adendo v2:** o fluxo de referência agora inclui também o ciclo de **remediação de
segurança orientada a findings** — receber um relatório de findings, reabrir a
constituição via PDR, especificar e remediar. Exercitar esse ciclo in-app (sem depender
de edge/infra) faz parte do valor instrumental do projeto.

## Why Now / Why This Way

- **Por que agora:** projeto de teste, sem urgencia de negocio. Existe para validar o
  fluxo de desenvolvimento antes/junto de projetos reais.
- **Por que uma to-do list CRUD:** e o "hello world" canonico de APIs REST — dominio
  trivial e de escopo conhecido, o que mantem o foco no FLUXO e no stack, e nao na
  complexidade do dominio.
- **Por que este stack:** Java / Spring Boot / MongoDB e a convencao padrao do time;
  usar o stack padrao garante que o exercicio seja representativo dos projetos reais.

**Adendo v2 — por que expandir agora e por que in-app:**
- **Por que agora:** o usuário recusou aceitar o risco de 4 findings de segurança
  (F-0002/F-0003/F-0006/F-0007). Remediá-las agora, no projeto de referência, exercita
  o ciclo de remediação enquanto o escopo ainda é pequeno e controlado.
- **Por que in-app (Spring) e não edge/BUY:** não há infraestrutura de deploy disponível
  (roda apenas em localhost; sem Cloudflare/reverse proxy/WAF). As convenções
  (`security-conventions.md § Rate Limiting`, `§ Default Headers`) preferem edge para
  serviços public-facing, mas essa opção é inviável neste ambiente. Portanto a remediação
  é **in-app** como defesa em profundidade: rate limiting in-app, logging estruturado +
  auditoria in-app, Spring Security para headers + CORS allowlist, e paginação via
  `Pageable`. Embasado nos PDRs 0001–0004.

## Success Criteria (v2)

Mensuráveis e observáveis. Critérios 1–3 herdados da v1 (escopo CRUD); critério 4 é novo
(remediação de segurança).

1. **Endpoints:** os 5 endpoints CRUD (criar, listar, ver por id, atualizar, deletar)
   estao implementados e respondem conforme a especificacao da feature.
2. **Cobertura de testes:** cada um dos 5 endpoints possui no minimo 1 teste de caminho
   feliz e 1 teste de caminho de erro (total >= 10 testes), todos passando.
   _[Suposicao explicita — ver Assumptions; ajuste o numero minimo se desejar.]_
3. **Fluxo encadeado:** os 3 artefatos de processo existem e se referenciam em cadeia —
   constituicao (este arquivo), spec da feature CRUD e plano (`/plan`) — comprovando que
   o fluxo foi exercitado de ponta a ponta.
4. **Remediação de segurança (novo na v2):** as 4 findings recusadas pelo usuário
   (F-0002, F-0003, F-0006, F-0007) estão **remediadas in-app e cobertas por testes
   automatizados que passam**. Contável e verificável:
   - F-0002 (rate limiting): endpoints de escrita retornam **429** ao exceder o limite
     definido na spec — comprovado por teste.
   - F-0003 (paginação): `GET /tarefas` aceita `page`/`size`, aplica `size` máximo e
     retorna metadados de paginação — comprovado por teste.
   - F-0006 (logging/auditoria): cada mutação (create/update/delete) bem-sucedida emite
     um log de auditoria estruturado, sem dados sensíveis — comprovado por teste.
   - F-0007 (headers + CORS): respostas carregam os headers de segurança definidos na
     spec e o CORS opera por allowlist explícita (nunca `*`) — comprovado por teste.
   Critério satisfeito quando as 4 findings constam como **Resolved** em
   `docs/reviews/security-findings-001.md` (ou em uma re-revisão posterior).

## Non-Goals

Este projeto explicitamente NAO faz:

1. **Autenticacao / autorizacao** — sem login, tokens, papeis ou controle de acesso.
   _(Inalterado na v2. Spring Security entra na v2 APENAS para headers de segurança e
   CORS — sem authentication/authorization. Ver § Constraints e PDR-0003.)_
2. **Multiplos usuarios / multi-tenant** — uma unica colecao global de tarefas, sem
   isolamento por usuario. _(Inalterado na v2.)_
3. ~~**Paginacao avancada / filtros / ordenacao** — listar retorna todas as tarefas.~~
   **REMOVIDO na v2 (reverte o Non-Goal #3 da v1).** Paginação **básica** (`page`/`size`
   com `size` máximo e metadados) entra em escopo via PDR-0004 para remediar F-0003.
   **Permanecem fora de escopo:** filtros, busca textual e ordenação customizada — a
   listagem é paginada porém sem critérios de filtro/ordenação. Ver PDR-0004 e
   `specs/002-hardening-seguranca/spec.md § Out of Scope`.
4. **Categorias, tags ou agrupamento** de tarefas. _(Inalterado na v2.)_
5. **Data de vencimento, lembretes ou notificacoes.** _(Inalterado na v2.)_
6. **Frontend / interface grafica** — somente API REST. _(Inalterado na v2. O CORS
   allowlist da v2 é configurado mesmo sem frontend próprio: prepara consumo cross-origin
   e fecha a lacuna de CORS aberto — ver PDR-0003.)_

## Constraints

- **Stack:** Java / Spring Boot / MongoDB (convencao padrao do time). Nao negociavel.
- **Escopo:** manter o minimo viavel. Qualquer expansao alem do CRUD basico deve passar
  por um PDR antes de entrar em um spec. _(Inalterado — este foi exatamente o portão
  acionado para a expansão de hardening da v2: PDRs 0001–0004.)_
- **Hardening de segurança (revisado na v2):** ~~decisoes de hardening (rate limiting,
  observabilidade avancada) ficam fora do v1.~~ **Passam a estar EM ESCOPO, na variante
  in-app:**
  - **Rate limiting in-app** nos endpoints de escrita (PDR-0001, remedia F-0002).
  - **Logging estruturado + auditoria de mutações in-app** (PDR-0002, remedia F-0006).
  - **Headers de segurança + CORS allowlist via Spring Security in-app** (PDR-0003,
    remedia F-0007).
  - **Paginação básica via `Pageable`** no `GET /tarefas` (PDR-0004, remedia F-0003).
  Restrição de ambiente que motiva o "in-app": **não há infra de deploy / edge**
  (Cloudflare/reverse proxy/WAF indisponíveis; roda em localhost). A opção edge/BUY,
  preferida pelas convenções para serviços public-facing, fica registrada como rejeitada
  por inviabilidade ambiental (ver PDRs). Quando houver deploy público, reavaliar mover
  rate limiting e headers para o edge (revisit trigger nas findings permanece válido).
- **Sem producao real:** nao ha SLA, dados sensiveis ou usuarios externos. A remediação
  in-app da v2 é defesa em profundidade e exercício de fluxo, não resposta a uma exposição
  pública existente. _(Atualizado: a cláusula anterior usava este ponto para excluir o
  hardening; a v2 mantém o fato — sem produção real — mas decide remediar mesmo assim.)_
- **Limites de hardening (permanecem fora):** observabilidade avançada com vendor (Sentry,
  APM, tracing distribuído), segredos via vault, encryption at rest gerenciada e qualquer
  hardening que dependa de edge/infra **continuam fora do escopo** até existir deploy real.
  A v2 inclui apenas as 4 remediações in-app acima.

## Assumptions

Suposicoes explicitas adotadas na ausencia de definicao do usuario — ajuste se discordar:

- **A1 (cobertura de testes):** "cobertura de testes" foi traduzida em algo contavel como
  ">= 1 teste de caminho feliz + >= 1 teste de erro por endpoint". O usuario nao definiu
  uma meta de cobertura percentual; se desejar uma (ex.: 80% de linhas), substitua o
  criterio 2.
- **A2 (referencia limpa do fluxo):** "referencia limpa do fluxo" foi ancorada no proxy
  observavel "os 3 artefatos existem e se referenciam em cadeia", por ser qualitativa.
- **A3 (deciders):** assumido que o unico decisor de produto e Matheus Melo, conforme o
  contexto de desenvolvedor solo.
- **A4 (números de remediação — novo na v2):** os limites concretos da remediação (janela
  e quota de rate limit, `size` default/máximo de paginação, conjunto de headers) foram
  definidos na spec `002-hardening-seguranca` ancorados em `security-conventions.md`
  (§ Rate Limiting, § Default Headers, § CORS Default). São pontos de partida ajustáveis;
  alterá-los não exige nova constituição, apenas atualização da spec.

## Histórico de Versões

### v2 — 2026-06-01 (Accepted) — esta versão

Emenda de remediação de segurança. Mudanças relativas à v1:
- **§ Non-Goals #3:** removido — paginação básica entra em escopo (PDR-0004). Filtros,
  busca e ordenação permanecem fora.
- **§ Constraints:** cláusula de hardening revisada — rate limiting, logging/auditoria e
  headers/CORS (variante in-app) passam a estar EM escopo (PDRs 0001–0003).
- **§ Success Criteria:** adicionado critério 4 (4 findings remediadas e testadas).
- **Inalterado:** Users, Problem (núcleo), Non-Goals #1/#2/#4/#5/#6, stack, portão de PDR.

### v1 — 2026-06-01 (Superseded by v2)

Registro preservado da constituição original (escopo CRUD mínimo):
- **Status original:** Accepted (2026-06-01 por Matheus Melo).
- **Non-Goals (v1):** incluíam "#3 Paginacao avancada / filtros / ordenacao — listar
  retorna todas as tarefas".
- **Constraints (v1):** "Sem producao real: ... decisoes de hardening (rate limiting,
  observabilidade avancada) ficam fora do v1."
- **Success Criteria (v1):** continham apenas os critérios 1–3 (sem o critério de
  remediação de segurança).
- **Motivo da supersessão:** decisão de Matheus Melo (2026-06-01) de remediar as findings
  F-0002/F-0003/F-0006/F-0007 in-app, formalizada pelos PDRs 0001–0004.
