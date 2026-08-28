# Desenvolvimento

## Git Flow

- `main`: versões estáveis;
- `develop`: integração da próxima versão;
- `feature/*`: uma mudança funcional pequena;
- `release/*`: estabilização, versão e changelog;
- `hotfix/*`: correção urgente originada na `main`.

Features são integradas em `develop` com merge explícito. Uma release validada é integrada em
`main` e `develop`, recebe tag semântica e preserva seu registro no `CHANGELOG.md`.

## Commits

As mensagens seguem Conventional Commits, no imperativo e com uma responsabilidade por commit:

```text
feat: add voting result endpoint
fix: enforce unique vote under concurrency
test: cover session closing boundary
docs: describe local execution
chore: update build configuration
```

## Verificações antes de integrar

```bash
./mvnw clean verify
git diff --check
```

A suíte cobre regras puras do domínio, services isolados, fluxo HTTP integrado, migrations e
concorrência. Mudanças em regra de negócio devem incluir teste unitário; mudanças no contrato ou na
persistência devem incluir teste de integração.

## Convenções

- Java 21 e UTF-8;
- injeção por construtor;
- entidades sem setters públicos;
- transações delimitadas nos services;
- DTOs separados das entidades persistidas;
- UTC para regras e persistência temporal;
- migrations somente incrementais após uma versão publicada;
- dados pessoais não devem aparecer em logs;
- novas dependências exigem um caso de uso concreto.

