# Validação da entrega

Verificações reproduzíveis da API e do protocolo de telas, executadas a partir da raiz do
repositório (Linux/macOS; no Windows use `mvnw.cmd`). Docker é obrigatório: os testes de integração
sobem PostgreSQL 17 via Testcontainers.

## Suíte automatizada

```bash
./mvnw clean verify
```

Execução de 29/10/2026 (versão 0.5.0, Spring Boot 4.0.9): **63 testes, 0 falhas, 0 erros, 0
ignorados**; JaCoCo com 89,04% de linhas e 73,50% de branches (mínimos de 85% e 70%). Cobre domínio, serviços, HTTP com
MockMvc, autenticação (login, token forjado, papéis), persistência, elegibilidade, fronteira
temporal (1 s antes, exatamente no fechamento e 1 s depois; elegibilidade que responde após o
fechamento), provedor indisponível (503 sem voto), 100 tentativas concorrentes do mesmo eleitor
no PostgreSQL (um voto) e apuração de 2.000 votos abaixo de 1 s.

A CI (`.github/workflows/ci.yml`) executa o mesmo `verify`, exige contagem de testes maior que
zero, constrói a imagem final, bloqueia HIGH/CRITICAL no scan Trivy e faz um smoke da imagem com
PostgreSQL.

## Imagem final

Trivy 0.69.3 (base de 29/10/2026) sobre a imagem construída pelo `Dockerfile`
(`sha256:99dcaa821bf4…`, Alpine 3.24.3): **0 HIGH/CRITICAL**. Com Spring Boot 3.5.16 a mesma
imagem tinha 12 achados (Tomcat 10.1.55, `spring-webmvc` 6.2.19 sem correção na linha 6.2,
Jackson 2.21.4 e driver PostgreSQL 42.7.11), motivo da migração para o Boot 4.

## Jornada contra a imagem final, reinício e restore

Executada em 29/10/2026 num projeto Compose isolado (`coopvote-v5`, portas próprias) com a
imagem acima: login errado 401; eleitor cadastrando pauta 403; sessão aberta pela gestora 201;
voto de `associado-1` com `associateId` de outro associado no corpo registrado para `associado-1`;
segundo voto 409 `DUPLICATE_VOTE`; auditora votando 403; sem token 401; recibo com `castAt`;
parcial oculta ao eleitor e visível à auditora; telas mobile sem "Cadastrar nova pauta" para o
eleitor; reinício só da API preservando recibo e token; voto após o encerramento
`VOTING_SESSION_CLOSED`; resultado final `yes=1 no=1 TIED`. Dump (`pg_dump -Fc`) restaurado num
PostgreSQL 17 vazio e lido pela imagem: login, mesmo resultado final, recibo e 409 num novo voto;
`flyway_schema_history` com 2 migrations.

```bash
docker compose up --build -d --wait
```

Com o seed local (`AUTH_LOCAL_SEED=true`), faça login como `gestora`, cadastre uma pauta, abra uma
sessão, vote como `associado-1` e `associado-2`, consulte o recibo e a apuração. Reinicie só a API
com `docker compose restart api` e repita as consultas: os identificadores e totais permanecem. Para
o restore, gere `pg_dump -Fc` do serviço `database`, restaure num PostgreSQL vazio e suba a imagem
apontando para ele: login, recibo, resultado final e `409` num novo voto devem ser idênticos.

## Ambiente publicado

O serviço do Render não publica automaticamente (`autoDeployTrigger: off`). Os scripts
`scripts/smoke-test.ps1` e `scripts/mobile-smoke-test.ps1` precisam de token: obtenha-o com
`POST /api/v1/auth/login` antes de executá-los contra um ambiente onde o deploy foi feito pelo
proprietário.
