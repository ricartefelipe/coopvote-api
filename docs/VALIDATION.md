# Validação da entrega

Este documento reúne as verificações reproduzíveis da API e do protocolo de telas. Os comandos
podem ser executados no Windows a partir da raiz do repositório.

## Suíte automatizada

```powershell
.\mvnw.cmd clean verify
```

A versão validada executa 36 testes de domínio, serviços, HTTP, persistência em teste e
concorrência. O JaCoCo exige no mínimo 85% de linhas e 70% de branches; a validação atual atingiu
94,40% de linhas e 84,44% de branches.

## PostgreSQL e reinício

```powershell
docker compose up --build -d
```

Para validar persistência, cadastre uma pauta, abra uma sessão e registre um voto. Reinicie somente
o serviço da API com `docker compose restart api` e consulte novamente a pauta e a apuração. O
volume `postgres_data` deve permanecer intacto e os identificadores e totais devem ser os mesmos.

## Ambiente publicado

O smoke test REST valida saúde, OpenAPI, cadastro, abertura, voto, apuração e rejeição de voto
duplicado:

```powershell
.\scripts\smoke-test.ps1 -BaseUrl https://coopvote-api.onrender.com
```

O smoke test mobile percorre callbacks reais e valida as telas `SELECAO` e `FORMULARIO`, o domínio
configurado das URLs, a abertura da sessão, o voto e a tela de resultado:

```powershell
.\scripts\mobile-smoke-test.ps1 -BaseUrl https://coopvote-api.onrender.com
```

Nos dois casos, o resultado esperado contém `status = PASSED`.

## Verificações manuais

- Swagger UI: `https://coopvote-api.onrender.com/swagger-ui.html`;
- OpenAPI: `https://coopvote-api.onrender.com/api-docs`;
- saúde: `https://coopvote-api.onrender.com/actuator/health`;
- logs: linhas JSON com nível, logger, mensagem e `correlationId` nos eventos de requisição;
- métodos não suportados: Problem Details com HTTP 405 e código `METHOD_NOT_ALLOWED`;
- rotas inexistentes: Problem Details com HTTP 404 e código `RESOURCE_NOT_FOUND`.

O plano gratuito pode suspender o serviço após inatividade. Nesse caso, aguarde a primeira resposta
antes de repetir a validação.
