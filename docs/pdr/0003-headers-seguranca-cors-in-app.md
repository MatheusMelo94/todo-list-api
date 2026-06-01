# PDR-0003: Headers de segurança + CORS allowlist via Spring Security in-app entra no escopo (remedia F-0007)

- **Status:** Accepted
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo (autoridade única de produto)

## Context

A finding **F-0007** (`docs/reviews/security-findings-001.md` — LOW, OWASP A05 Security
Misconfiguration, WSTG-CONF-07, ASVS V14) registra que não há headers de segurança
(HSTS, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, etc.) nem política de
CORS explícita; e que o projeto não tem `spring-boot-starter-security`, então nem os
defaults de hardening do Spring Security estão presentes. Em 2026-06-01 Matheus Melo
**recusou aceitar** esse risco.

A constituição v1 tratava hardening de borda como fora do v1 e listava frontend como
Non-Goal #6. Incluir headers/CORS in-app é expansão de escopo e exige este PDR.

## Decision

**Adicionar `spring-boot-starter-security` e configurar, in-app, headers de segurança +
CORS por allowlist explícita** como remediação de F-0007.
- **Headers:** via `HttpSecurity.headers(...)` — HSTS, X-Content-Type-Options (`nosniff`),
  X-Frame-Options (`DENY`), Referrer-Policy, e demais definidos na spec, ancorados em
  `security-conventions.md § Default Headers`.
- **CORS:** allowlist explícita de origens, **nunca `*`**, configurável via variável de
  ambiente, com métodos/headers explícitos, per `security-conventions.md § CORS Default`.

**Escopo do Spring Security limitado a headers + CORS. NÃO se introduz authentication nem
authorization** — Non-Goal #1 (auth) permanece intacto na Constitution v2. A configuração
deixa explicitamente todos os endpoints anônimos (`permitAll`), apenas adicionando os
headers e a política de CORS.

Escolha **BUILD in-app** (Spring Security), não edge, motivada pela restrição de ambiente
(sem reverse proxy/CDN; localhost).

## Consequences

- **Positive:** remedia F-0007; fecha clickjacking/MIME-sniffing e CORS aberto; traz os
  defaults de hardening do Spring Security; exercita o padrão in-app no projeto de
  referência. CORS por allowlist prepara consumo cross-origin mesmo sem frontend próprio.
- **Negative:** adiciona `spring-boot-starter-security` (dependência maior; precisa de
  config explícita `permitAll` para não quebrar o acesso anônimo existente). HSTS em
  localhost (HTTP) tem efeito prático nulo, mas mantém o header conforme convenção e
  ergonomia futura. Risco de configuração: um `permitAll` mal feito ou um filtro de auth
  acidental violaria o Non-Goal #1 — mitigado por teste que confirma acesso anônimo.
- **Neutral:** em deploy real, parte dos headers pode vir do edge; a config in-app vira
  defesa em profundidade.

## Alternatives Considered

- **Headers/CORS no edge (reverse proxy / Cloudflare) — BUY/edge.** Rejeitado: inviável
  sem infra de deploy (localhost). Preferência das convenções para public-facing; revisit
  na transição para deploy.
- **Filtro de headers manual sem Spring Security.** Rejeitado: reinventa o que
  `HttpSecurity.headers(...)` já entrega de forma testada e alinhada à convenção; o
  starter também é o caminho previsto pela arquitetura (`architecture-conventions.md`
  lista Spring Security no stack baseline).
- **Não fazer nada / aceitar o risco.** Rejeitado: usuário recusou aceitar F-0007.

## Constitution Impact

**Sim — exige emenda.** Revisa a § Constraints da v1 (hardening fora do v1). Formalizado
na **Constitution v2**, § Constraints e § Success Criteria critério 4. **Non-Goal #1
(auth) permanece intacto** — esta decisão usa Spring Security somente para headers/CORS,
sem authentication/authorization. A v2 supersede a v1.

## References

- Finding: `docs/reviews/security-findings-001.md` § Finding 0007
- Constituição: `memory/constitution.md` (v2) § Constraints, § Non-Goals #1 (intacto), § Success Criteria #4
- Convenção: `security-conventions.md § Default Headers`, `§ CORS Default`; `architecture-conventions.md § CORS`, `§ Stack Baseline`
- Spec: `specs/002-hardening-seguranca/spec.md`
- PDRs relacionados: 0001, 0002, 0004
