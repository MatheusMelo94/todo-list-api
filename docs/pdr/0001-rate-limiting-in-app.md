# PDR-0001: Rate limiting in-app entra no escopo (remedia F-0002)

- **Status:** Accepted
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo (autoridade única de produto)

## Context

A finding **F-0002** (`docs/reviews/security-findings-001.md` — LOW, OWASP A04 Insecure
Design / API4 Unrestricted Resource Consumption) registra a ausência de rate limiting em
todos os endpoints: um cliente pode criar tarefas em loop, inflando a coleção. Em
2026-06-01 Matheus Melo **recusou aceitar** esse risco (registrado na finding como
pendência aberta a remediar antes de qualquer deploy público).

A constituição v1 colocava rate limiting **fora do v1** (§ Constraints: "decisoes de
hardening (rate limiting, observabilidade avancada) ficam fora do v1"). Incluí-lo é uma
expansão de escopo que, per a própria § Constraints v1 ("qualquer expansao alem do CRUD
basico deve passar por um PDR antes de entrar em um spec"), exige este PDR antes do spec.

## Decision

**Incluir rate limiting in-app (Spring) nos endpoints de escrita** (`POST`, `PUT`,
`DELETE` de `/tarefas`) como remediação de F-0002. Implementação como filtro/interceptor
Spring com contagem por janela de tempo por IP de origem; ao exceder, responder
**HTTP 429** com o contrato de erro padrão da convenção. Os limites concretos (janela,
quota) são definidos na spec `002-hardening-seguranca`, ancorados em
`security-conventions.md § Rate Limiting`.

Escolha **BUILD in-app**, não BUY/edge, motivada pela restrição de ambiente: não há
infraestrutura de deploy nem edge (Cloudflare/WAF) disponível — o projeto roda em
localhost.

## Consequences

- **Positive:** fecha o vetor de exaustão de recursos (F-0002); exercita o padrão de
  rate limiting in-app no projeto de referência; defesa em profundidade independente de
  infra.
- **Negative:** rate limit por IP in-app é menos robusto que no edge (estado em memória
  por instância; sem visão distribuída; IP pode ser compartilhado/NAT). Aceitável na
  posture Prototype/localhost. Adiciona uma dependência ou código de limitação a manter.
- **Neutral:** quando houver deploy público, parte do limite pode migrar para o edge
  (revisit trigger da finding permanece válido); o limite in-app vira defesa secundária.

## Alternatives Considered

- **Rate limiting no edge (Cloudflare / reverse proxy) — BUY/edge.** Rejeitado:
  inviável neste ambiente — não há infra de deploy nem edge disponível (localhost). É a
  preferência das convenções (`security-conventions.md § Rate Limiting`: "WAF/edge rate
  limiting is preferred") para serviços public-facing, mas não aplicável agora. Registrado
  como revisit na transição para deploy público.
- **Não fazer nada / aceitar o risco.** Rejeitado: o usuário recusou explicitamente
  aceitar o risco de F-0002 em 2026-06-01.
- **Rate limit também nos endpoints de leitura (`GET`).** Não adotado nesta decisão: o
  vetor de F-0002 é o crescimento ilimitado da coleção (escrita). Leitura paginada é
  tratada por PDR-0004. Pode ser revisitado se surgir abuso de leitura.

## Constitution Impact

**Sim — exige emenda.** Reverte parcialmente a § Constraints da v1 (hardening fora do v1).
Formalizado na **Constitution v2** (`memory/constitution.md`), § Constraints (hardening
in-app em escopo) e § Success Criteria critério 4. A v2 supersede a v1 (constituições são
imutáveis; emenda = nova versão, per `product-conventions.md § Constitution Shape`).

## References

- Finding: `docs/reviews/security-findings-001.md` § Finding 0002
- Constituição: `memory/constitution.md` (v2) § Constraints, § Success Criteria #4
- Convenção: `security-conventions.md § Rate Limiting`, `§ Error Responses`
- Spec: `specs/002-hardening-seguranca/spec.md`
- PDRs relacionados: 0002, 0003, 0004 (pacote de remediação de F-0002/F-0006/F-0007/F-0003)
