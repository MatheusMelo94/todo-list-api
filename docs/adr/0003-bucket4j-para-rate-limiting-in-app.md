# ADR-0003: Bucket4j (in-memory) como biblioteca de rate limiting in-app

- **Status:** Accepted (2026-06-01)
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo

> **ACCEPTED (2026-06-01) por Matheus Melo.** Bucket4j (in-memory) aprovado como a
> biblioteca de rate limiting in-app. As tarefas T-RL-* da feature
> `002-hardening-seguranca` estão liberadas para implementação.

## Context

PDR-0001 (Accepted) e a Constitution v2 (§ Constraints) já decidiram **incluir rate
limiting in-app** nos endpoints de escrita (`POST`/`PUT`/`DELETE` de `/tarefas`),
remediando F-0002, na variante BUILD in-app (sem edge/WAF — roda em localhost). A spec
`002-hardening-seguranca` fixa os parâmetros: **60 requisições por IP por janela fixa de
60s, por endpoint de escrita** (OQ-3, default adotado: contadores separados por endpoint),
respondendo **429** com o contrato de erro padrão e header `Retry-After` (OQ-1, default
adotado: incluir).

O que PDR-0001 deixou em aberto é **como** construir o limitador. `architecture-conventions.md
§ Stack Baseline` exige: *"Don't introduce new core technologies without an ADR."* Bucket4j
é uma biblioteca nova fora do baseline declarado — sua adoção é, portanto, um desvio que
exige esta ADR antes de entrar no `pom.xml`. A alternativa (filtro próprio) não introduz
dependência, mas reimplementa lógica de token-bucket sensível a concorrência.

## Decision

Adotar **Bucket4j (core, armazenamento in-memory por instância)** como biblioteca de rate
limiting in-app, aplicada via um `OncePerRequestFilter` (ou `HandlerInterceptor`) restrito
aos endpoints de escrita de `/tarefas`.

- **Algoritmo:** token-bucket com janela equivalente a 60 tokens / 60s por chave.
- **Chave do bucket:** `IP de origem + método/endpoint de escrita` (contadores separados
  por endpoint — OQ-3 default da spec).
- **Armazenamento:** `ConcurrentHashMap<String, Bucket>` in-memory (thread-safe; estado por
  instância — aceitável em localhost/Prototype, per PDR-0001 § Consequences).
- **Resposta ao exceder:** HTTP 429 no shape `ErrorResponse` (timestamp, status=429, error,
  message, path) montado de forma consistente com o `GlobalExceptionHandler`, mais header
  `Retry-After` (segundos até o reset).
- **Escopo:** somente endpoints de escrita; leitura (`GET`) não é limitada (PDR-0001).

A dependência fica fixada no `pom.xml` na linha compatível com Spring Boot 3.5.x (ADR-0002).

## Consequences

- **Positive:** lógica de token-bucket testada e thread-safe pronta (não reinventamos
  concorrência); API expressiva para janela/quota; troca futura para backend distribuído
  (ex.: Redis/Hazelcast) é suportada pela própria lib quando houver deploy. Fecha F-0002.
- **Negative:** adiciona uma dependência fora do `§ Stack Baseline` (a manter/atualizar);
  estado in-memory não é distribuído (cada instância tem seu contador) — irrelevante em
  instância única local, revisit no deploy. Buckets in-memory crescem com o nº de IPs
  distintos (mitigável com expiração/eviction; baixo risco em localhost).
- **Neutral:** quando houver edge/deploy, parte do limite migra para o edge (revisit
  trigger de F-0002); o limite in-app vira defesa secundária.

## Alternatives Considered

- **Filtro próprio com contador in-memory (sem dependência).** Rejeitado como default:
  reimplementa token-bucket concorrente (janela, reset, thread-safety) — superfície de bug
  sem ganho sobre uma lib madura. Permanece como **fallback caso esta ADR seja rejeitada**.
- **Resilience4j RateLimiter.** Rejeitado: orientado a proteção de chamadas
  (cliente/bulkhead), menos ergonômico para rate limit por-chave (por IP+endpoint) em filtro
  HTTP; também é dependência nova sem vantagem sobre Bucket4j para este caso.
- **Spring Cloud Gateway / filtro de gateway.** Rejeitado: introduz um componente de
  arquitetura (gateway) desproporcional para uma app single-module em localhost.

## References

- Finding: `docs/reviews/security-findings-001.md` § Finding 0002
- PDR: `docs/pdr/0001-rate-limiting-in-app.md`
- Constituição: `memory/constitution.md` (v2) § Constraints, § Success Criteria #4
- Convenção: `architecture-conventions.md § Stack Baseline` (gate de tecnologia nova),
  `security-conventions.md § Rate Limiting`, `§ Error Responses`
- ADR relacionada: `docs/adr/0002-...` (pin Spring Boot 3.5.x — condiciona a versão da lib)
- Spec/Plan: `specs/002-hardening-seguranca/spec.md`, `specs/002-hardening-seguranca/plan.md`
