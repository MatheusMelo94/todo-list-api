# todo-list-api Constitution v1

- **Status:** Accepted
- **Date:** 2026-06-01
- **Accepted:** 2026-06-01 por Matheus Melo
- **Deciders:** Matheus Melo (desenvolvedor solo, autoridade única de produto)

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

## Why Now / Why This Way

- **Por que agora:** projeto de teste, sem urgencia de negocio. Existe para validar o
  fluxo de desenvolvimento antes/junto de projetos reais.
- **Por que uma to-do list CRUD:** e o "hello world" canonico de APIs REST — dominio
  trivial e de escopo conhecido, o que mantem o foco no FLUXO e no stack, e nao na
  complexidade do dominio.
- **Por que este stack:** Java / Spring Boot / MongoDB e a convencao padrao do time;
  usar o stack padrao garante que o exercicio seja representativo dos projetos reais.

## Success Criteria (v1)

1. **Endpoints:** os 5 endpoints CRUD (criar, listar, ver por id, atualizar, deletar)
   estao implementados e respondem conforme a especificacao da feature.
2. **Cobertura de testes:** cada um dos 5 endpoints possui no minimo 1 teste de caminho
   feliz e 1 teste de caminho de erro (total >= 10 testes), todos passando.
   _[Suposicao explicita — ver Assumptions; ajuste o numero minimo se desejar.]_
3. **Fluxo encadeado:** os 3 artefatos de processo existem e se referenciam em cadeia —
   constituicao (este arquivo), spec da feature CRUD e plano (`/plan`) — comprovando que
   o fluxo foi exercitado de ponta a ponta.

## Non-Goals

Este projeto explicitamente NAO faz:

1. **Autenticacao / autorizacao** — sem login, tokens, papeis ou controle de acesso.
2. **Multiplos usuarios / multi-tenant** — uma unica colecao global de tarefas, sem
   isolamento por usuario.
3. **Paginacao avancada / filtros / ordenacao** — listar retorna todas as tarefas.
4. **Categorias, tags ou agrupamento** de tarefas.
5. **Data de vencimento, lembretes ou notificacoes.**
6. **Frontend / interface grafica** — somente API REST.

## Constraints

- **Stack:** Java / Spring Boot / MongoDB (convencao padrao do time). Nao negociavel.
- **Escopo:** manter o minimo viavel. Qualquer expansao alem do CRUD basico deve passar
  por um PDR antes de entrar em um spec.
- **Sem producao real:** nao ha SLA, dados sensiveis ou usuarios externos; decisoes de
  hardening (rate limiting, observabilidade avancada) ficam fora do v1.

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
