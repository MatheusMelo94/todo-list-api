# Proposta de Convencao 0002: § ADR Template — expandir enumeracao de status e padronizar a data

- **Status:** Proposed — aguardando aprovacao do usuario (Matheus Melo)
- **Date:** 2026-06-01
- **Autor:** Backend Architect
- **Baseline afetado:** `architecture-conventions.md § ADR Template` — campo
  **Status** e campo **Date**.

---

## Motivacao (gap observado)

A enumeracao de status no template atual nao preve **`Rejected`**, mas o projeto
`todo-list-api` precisou dele: o ADR-0001 (contrato de erro simplificado) foi proposto
e depois **rejeitado** pelo usuario, e ficou como registro historico. O autor teve de
inventar `Rejected (2026-06-01)` fora do enum sancionado.

Alem disso, surgiu uma inconsistencia de **onde a data aparece**:
- ADR-0001: `- **Status:** Rejected (2026-06-01)` e tambem `- **Date:** 2026-06-01`
- ADR-0002: `- **Status:** Accepted (2026-06-01)` e tambem `- **Date:** 2026-06-01`

A data acabou duplicada (inline no Status e no campo Date), porque o template define um
campo `Date` mas nao diz qual data ele representa nem se a transicao de status tambem
deve ser datada. Falta padronizacao.

---

## Texto atual no baseline

Em `§ ADR Template`, dentro do bloco de template:

> ```text
> # ADR-[NNNN]: [Decision title]
>
> - **Status:** Proposed | Accepted | Superseded by ADR-[NNNN]
> - **Date:** YYYY-MM-DD
> - **Deciders:** [name(s) — typically the user]
> ```

E, ao final da secao:

> ADRs are immutable once *Accepted* — replace via *Superseded* rather than edit.

---

## Texto proposto (diff em prosa)

**1. Expandir a enumeracao de Status** para incluir `Rejected`:

> - **Status:** Proposed | Accepted | Rejected | Superseded by ADR-[NNNN]

**2. Padronizar onde a data aparece.** Substituir a linha `- **Date:** YYYY-MM-DD` e
acrescentar uma nota de uso logo abaixo do bloco de template:

> - **Status:** Proposed | Accepted | Rejected | Superseded by ADR-[NNNN] _(YYYY-MM-DD)_
> - **Date:** YYYY-MM-DD _(data da proposta)_
> - **Deciders:** [name(s) — typically the user]
>
> **Convencao de datas:**
> - `Date` registra a data da **proposta** (quando o ADR foi escrito), e nao muda.
> - A **data da transicao terminal** (aceite, rejeicao ou superseding) acompanha o
>   campo `Status`, entre parenteses: `Accepted (2026-06-01)`, `Rejected (2026-06-01)`,
>   `Superseded by ADR-0009 (2026-08-12)`. Para status `Proposed`, a data entre
>   parenteses e omitida (ainda nao houve decisao).

**3. Ampliar a regra de imutabilidade** para cobrir `Rejected`:

> ADRs are immutable once *Accepted* **or *Rejected*** — replace via *Superseded*
> rather than edit. Um ADR *Rejected* permanece no repositorio como registro historico
> da proposta recusada; nao se apaga nem se reaproveita o numero.

---

## Justificativa

- **Reflete a pratica real:** `Rejected` ja foi usado (ADR-0001) e e um desfecho
  legitimo do Deviation Gate — o usuario pode recusar uma divergencia, e isso precisa
  de status sancionado em vez de texto ad-hoc.
- **Elimina a duplicacao de data:** hoje a mesma data aparece em `Status` e em `Date`
  sem regra. Separar "data da proposta" (campo `Date`, estavel) de "data da transicao"
  (inline no `Status`) da rastreabilidade da decisao sem ambiguidade.
- **Preserva a imutabilidade:** estender a regra a `Rejected` mantem o ADR como trilha
  de auditoria — a proposta recusada continua visivel, com seu racional, em vez de
  desaparecer.
- **Coerente com o ciclo `Proposed` → `Accepted`/`Rejected` → (eventualmente)
  `Superseded`.**

## Escopo da mudanca

- Altera apenas o campo `Status`, o campo `Date` e a regra de imutabilidade em
  `§ ADR Template`. Nao toca no corpo do template (Context/Decision/Consequences/etc.).
- Decisao de aceitar/editar/recusar e exclusiva do usuario. Esta proposta NAO altera o
  baseline. (Nota: ADR-0001 e ADR-0002 ja foram emitidos com a forma proposta; aprovar
  esta proposta apenas legitima retroativamente esse padrao.)

## Referencias

- `architecture-conventions.md § ADR Template`
- `todo-list-api/docs/adr/0001-contrato-de-erro-simplificado.md` (uso de `Rejected`)
- `todo-list-api/docs/adr/0002-fixar-spring-boot-3-5-x-para-compatibilidade-com-flapdoodle.md`
  (uso de `Accepted (data)`)
