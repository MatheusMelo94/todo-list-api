# PDR-0002: Logging estruturado + auditoria de mutações in-app entra no escopo (remedia F-0006)

- **Status:** Accepted
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo (autoridade única de produto)

## Context

A finding **F-0006** (`docs/reviews/security-findings-001.md` — LOW, OWASP A09 Security
Logging and Monitoring Failures, ASVS V7) registra a ausência de logging de segurança e
de trilha de auditoria das mutações (create/update/delete): o único logging hoje é
`log.warn` no `GlobalExceptionHandler`. Em 2026-06-01 Matheus Melo **recusou aceitar**
esse risco.

A constituição v1 tratava observabilidade como hardening fora do v1 (§ Constraints).
Incluí-lo é expansão de escopo e exige este PDR antes do spec, per § Constraints v1.

## Decision

**Incluir logging estruturado + auditoria de mutações in-app (Spring / SLF4J+Logback)**
como remediação de F-0006. Cada mutação bem-sucedida (`create`/`update`/`delete`) emite
um evento de auditoria estruturado contendo no mínimo: timestamp, tipo de operação,
`id` da tarefa afetada e resultado — **sem dados sensíveis** e sem corpo completo da
requisição. Nível de log adequado (auditoria de operações em `INFO`; falhas em `WARN`/
`ERROR`). Correlação por request ID quando viável. Campos e formato exatos definidos na
spec `002-hardening-seguranca`, ancorados em `architecture-conventions.md § Logging` e
`security-conventions.md § Logging & Monitoring`.

Escolha **BUILD in-app** (logging estruturado via stack Spring padrão), não vendor de
observabilidade, motivada pela restrição de ambiente (sem deploy; localhost).

## Consequences

- **Positive:** remedia F-0006; cria trilha mínima para investigar abuso/incidentes;
  exercita o padrão de auditoria estruturada no projeto de referência.
- **Negative:** logs locais não persistem em plataforma gerenciada (sem retenção/alerta
  centralizado); valor operacional limitado sem agregador. Esforço de garantir que
  nenhum dado sensível vaze nos logs.
- **Neutral:** quando houver deploy/produção, encaminhar para plataforma de observabilidade
  gerenciada (Sentry/APM) é um passo futuro (BUY) — fora deste PDR.

## Alternatives Considered

- **Vendor de observabilidade (Sentry/APM) — BUY.** Rejeitado agora: depende de deploy e
  conta de vendor; inviável/desnecessário em localhost. As convenções preveem isso "when
  traffic warrants" — não é o caso. Revisit na transição para produção.
- **Não fazer nada / aceitar o risco.** Rejeitado: usuário recusou aceitar F-0006.
- **Auditar também leituras (`GET`).** Não adotado: o objetivo é trilha de **mutações**
  (integridade/repúdio). Auditar leituras geraria ruído sem ganho na posture atual.

## Constitution Impact

**Sim — exige emenda.** Revisa a § Constraints da v1 (hardening fora do v1). Formalizado
na **Constitution v2** (`memory/constitution.md`), § Constraints e § Success Criteria
critério 4. A v2 supersede a v1.

## References

- Finding: `docs/reviews/security-findings-001.md` § Finding 0006
- Constituição: `memory/constitution.md` (v2) § Constraints, § Success Criteria #4
- Convenção: `architecture-conventions.md § Logging`, `security-conventions.md § Logging & Monitoring`
- Spec: `specs/002-hardening-seguranca/spec.md`
- PDRs relacionados: 0001, 0003, 0004
