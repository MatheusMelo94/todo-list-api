# Proposta de Convencao 0003: § API Contracts — versionamento de rota e mapeamento enum↔JSON

- **Status:** Proposed — aguardando aprovacao do usuario (Matheus Melo)
- **Date:** 2026-06-01
- **Autor:** Backend Architect
- **Baseline afetado:** `architecture-conventions.md` —
  `§ Backend (Spring Boot) Conventions → API Contracts`.

---

## Motivacao (gap observado)

Dois pontos surgiram em `todo-list-api` que o `§ API Contracts` atual nao resolve de
forma acionavel:

**(a) Versionamento de rota.** O baseline diz "versioned at the route level **when
breaking changes occur**". Na pratica, `todo-list-api` montou os endpoints em
`@RequestMapping("/tarefas")` — **sem prefixo de versao**. Isso e coerente com a regra
atual (nenhum breaking change ainda), mas significa que o primeiro breaking change vai
exigir mover todas as rotas para `/v1/` retroativamente, ou conviver com a confusao de
`/tarefas` (v1 implicito) ao lado de `/v2/tarefas`. Falta uma diretriz de greenfield.

**(b) Mapeamento enum Java ↔ contrato JSON.** A nomenclatura do projeto e em portugues
e o contrato externo e minusculo. O enum `StatusTarefa` resolveu isso com `@JsonValue`:

```java
public enum StatusTarefa {
    PENDENTE,
    CONCLUIDA;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
```

Ou seja: constante Java `PENDENTE` (UPPER_SNAKE_CASE, idiomatica) ↔ token JSON
`"pendente"` (minusculo, contrato externo). Esse padrao vai se repetir em qualquer
enum exposto no contrato; hoje cada projeto decide ad-hoc (anotacao por valor, conversor
custom, string crua, etc.).

---

## Texto atual no baseline

Em `§ API Contracts`:

> - DTOs are versioned at the route level when breaking changes occur (`/v1/`, `/v2/`)
> - Validation is declarative via Jakarta annotations (`@NotNull`, `@Email`, `@Size`, etc.)
> - Custom validators when domain rules don't fit standard annotations
> - Error responses use a standard shape (timestamp, status, error code, message, path)

Nao ha diretriz sobre prefixo de versao em greenfield, nem sobre mapeamento enum↔JSON.

---

## Texto proposto (diff em prosa)

**(a) Substituir** a primeira bala por uma versao que fixa o prefixo desde o dia zero:

> - **Rotas carregam prefixo de versao desde o greenfield** (`/v1/...`). Projetos novos
>   ja nascem em `/v1/` — nao se espera o primeiro breaking change para introduzir
>   versao. Breaking changes futuros sobem para `/v2/` etc., permitindo coexistencia
>   `/v1` + `/v2` sem rota "sem versao" ambigua. (Recomendacao: definir o prefixo uma
>   vez via `server.servlet.context-path` ou um `@RequestMapping` base, nao repetir
>   `/v1` em cada controller.)

**(b) Acrescentar** uma bala nova ao final da secao, sobre mapeamento enum↔JSON:

> - **Mapeamento enum Java ↔ JSON:** constantes de enum seguem a convencao Java
>   (`UPPER_SNAKE_CASE`, ex.: `PENDENTE`); o token exposto no contrato JSON e definido
>   explicitamente, nunca herdado por acidente de `name()`. Padrao:
>   - **Serializacao:** `@JsonValue` num metodo que produz o token externo
>     (ex.: `name().toLowerCase()` para contrato minusculo).
>   - **Desserializacao:** quando o token externo difere de `name()`, parear com
>     `@JsonCreator` (ou um `Converter`) que aceita o token externo e e tolerante a
>     caixa, retornando 400 (nao 500) para valor desconhecido.
>   - O mapeamento (constante ↔ token) fica documentado num Javadoc curto no proprio
>     enum, citando que o contrato externo e parte da API publica.

> _Nota: o uso de `@JsonValue` sem o `@JsonCreator` correspondente serializa correto mas
> falha ao desserializar tokens que nao batem com `name()`. Sempre que o token externo
> divergir da constante (ex.: minusculo), os dois lados sao obrigatorios._

---

## Justificativa

- **(a) `/v1/` em greenfield** elimina a migracao retroativa dolorosa e a ambiguidade
  "rota sem versao = v1 implicito". O custo e zero no dia zero (um prefixo) e o ganho e
  evolutividade limpa. Trade-off honesto: para um projeto-exercicio descartavel como
  `todo-list-api`, o prefixo pode parecer cerimonia; por isso a diretriz vale como
  **default de baseline**, do qual um projeto pode divergir explicitamente (ADR) quando
  o ciclo de vida nao justificar versao.
- **(b) Mapeamento enum↔JSON** codifica um padrao que reaparece em todo enum exposto e
  previne o bug silencioso de `@JsonValue` sem `@JsonCreator` (serializa, mas quebra a
  desserializacao). Tambem desacopla a nomenclatura idiomatica Java (em portugues,
  maiusculo) do contrato externo, sem string crua nem espalhamento de conversores.

## Escopo da mudanca

- Altera/acrescenta apenas balas em `§ API Contracts`. Nao toca em error handling, auth
  nem em outras secoes.
- **Ponto de decisao do usuario (item a):** confirmar se a regra greenfield deve ser
  **obrigatoria** ("todo projeto novo nasce em `/v1/`") ou **default recomendado**
  (divergencia permitida via ADR para exercicios/descartaveis). A redacao acima assume
  default recomendado; ajustavel conforme sua preferencia.
- Decisao de aceitar/editar/recusar e exclusiva do usuario. Esta proposta NAO altera o
  baseline.

## Referencias

- `architecture-conventions.md § API Contracts`
- `todo-list-api/src/main/java/.../controller/TarefaController.java`
  (`@RequestMapping("/tarefas")` — sem prefixo de versao)
- `todo-list-api/src/main/java/.../model/StatusTarefa.java`
  (`@JsonValue` + `name().toLowerCase()`)
