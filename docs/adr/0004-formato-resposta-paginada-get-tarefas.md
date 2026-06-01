# ADR-0004: Formato da resposta paginada do GET /tarefas (envelope DTO próprio)

- **Status:** Accepted (2026-06-01)
- **Date:** 2026-06-01
- **Deciders:** Matheus Melo

> **ACCEPTED (2026-06-01) por Matheus Melo.** Envelope `PageResponse<T>` (DTO próprio)
> aprovado como o formato da resposta paginada. As tarefas T-PG-* e T-REG-* da feature
> `002-hardening-seguranca` estão liberadas para implementação. Esta ADR também alimenta
> uma proposta de atualização de convenção (ver § Convention Drift no `plan.md`).

## Context

PDR-0004 (Accepted) e a Constitution v2 (§ Non-Goals #3, revertido) decidiram incluir
**paginação básica via `Pageable`** no `GET /tarefas` (remedia F-0003), com `page` default
0, `size` default 20, `size` máximo 100 e **metadados** de paginação. Isso muda o contrato
de `GET /tarefas` de **array puro** (spec 001, AC2.1) para **resposta paginada com
metadados** — a única quebra de contrato intencional (spec 002, AC5.1).

`architecture-conventions.md § API Contracts` define o shape de **erro** padrão, mas **não
define um shape de resposta paginada** — é uma lacuna de convenção. Há três formas usuais
de serializar a página, e a escolha tem consequência de contrato de longo prazo:

1. Serializar o `org.springframework.data.domain.Page` diretamente (default da plataforma).
2. `PagedModel` (Spring HATEOAS / Spring Data web).
3. Um **DTO envelope próprio** versionável, mapeado pelo `mapper` (camada já existente).

Decisão necessária agora porque o `TarefaController.listar()` e seus testes de integração
dependem do shape escolhido, e o shape é o contrato público que substitui o array.

## Decision

Adotar um **DTO envelope de página próprio** — `PageResponse<T>` (record imutável) — montado
na camada de service/mapper a partir do `Page<Tarefa>` retornado pelo repositório.

Shape proposto (campos mínimos exigidos pela spec AC2.1):

```json
{
  "content": [ /* itens TarefaResponse */ ],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

- `content`: lista de `TarefaResponse` (boundary entidade↔DTO mantido — § Layer Rules).
- `page`: número da página atual (0-based).
- `size`: tamanho efetivo da página (após aplicar o teto de 100 — AC2.3).
- `totalElements`, `totalPages`: metadados de navegação.

Justificativa: serializar `Page` diretamente é **explicitamente desencorajado pelo próprio
Spring** (shape instável entre versões; aviso de serialização) e vazaria o objeto de domínio
de persistência na borda da API, violando `§ Layer Rules` ("controllers… never expose
entities" / boundary via mapper). Um envelope próprio dá um contrato estável, versionável
por rota (`/v1/`, `/v2/` quando necessário — § API Contracts) e testável.

## Consequences

- **Positive:** contrato de listagem estável e explícito; sem acoplamento ao objeto `Page`
  do Spring Data na borda; metadados exatos da spec; reaproveita a camada `mapper` existente.
  Fecha F-0003 com teto de `size` aplicado no service.
- **Negative:** **quebra de contrato** vs spec 001 (array → objeto paginado) — exige
  atualizar os testes de integração de `GET /tarefas` da 001 (T-PG-* / T-REG-*). Adiciona um
  DTO e o mapeamento `Page → PageResponse`.
- **Neutral:** abre caminho para filtros/ordenação no futuro (novo PDR) reusando o mesmo
  envelope. Como não há frontend (Non-Goal #6), não há consumidor a coordenar além dos testes.

## Alternatives Considered

- **Serializar `Page<T>` diretamente.** Rejeitado: shape instável entre versões do Spring
  (warning oficial de serialização de `PageImpl`), vaza estrutura de persistência na borda e
  fere `§ Layer Rules`.
- **`PagedModel` / Spring HATEOAS.** Rejeitado: traz hipermídia (links HAL) desnecessária
  para uma API sem frontend (Non-Goal #6) e adiciona dependência/peso sem ganho aqui.
- **Manter array + header `X-Total-Count` (sem envelope).** Rejeitado: a spec AC2.1 exige
  metadados de paginação no corpo (página atual, size, total de elementos e de páginas);
  headers sozinhos não cobrem o critério de forma idiomática.

## References

- Finding: `docs/reviews/security-findings-001.md` § Finding 0003
- PDR: `docs/pdr/0004-paginacao-get-tarefas.md`
- Constituição: `memory/constitution.md` (v2) § Non-Goals #3 (revertido), § Success Criteria #4
- Convenção: `architecture-conventions.md § API Contracts` (lacuna: sem shape paginado),
  `§ Layer Rules` (não expor entidade na borda)
- Spec afetada: `specs/001-crud-tarefas/spec.md` (AC2.x — contrato de `GET /tarefas`)
- Spec/Plan: `specs/002-hardening-seguranca/spec.md`, `specs/002-hardening-seguranca/plan.md`
