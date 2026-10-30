# CoopVote API

API REST para cadastrar pautas, abrir sessões temporizadas, receber os votos dos associados e
publicar a apuração. O projeto prioriza um desenho simples, regras de negócio explícitas e
execução reproduzível.

## Ambiente publicado

- API: `https://coopvote-api.onrender.com`
- Swagger UI: `https://coopvote-api.onrender.com/swagger-ui.html`
- contrato OpenAPI JSON: `https://coopvote-api.onrender.com/api-docs`
- entrada do aplicativo: `POST https://coopvote-api.onrender.com/api/v1/mobile`

O ambiente usa um plano gratuito e pode levar cerca de dois minutos para responder ao primeiro
acesso após um período de inatividade.

## Funcionalidades

- cadastro e consulta de pautas;
- abertura de uma única sessão por pauta, com duração configurável e padrão de um minuto;
- login do associado com credencial local e token Bearer (issuer, audience, expiração); papéis
  `VOTER`, `MANAGER` e `AUDITOR`;
- votos `Sim` ou `Não` do associado autenticado, limitados a um voto por associado em cada pauta
  (constraint no banco) e recibo consultável;
- encerramento calculado pelo relógio, sem processo agendado;
- apuração agregada pelo banco, adequada a grandes volumes;
- virtual threads no processamento HTTP;
- verificação de elegibilidade pelo CPF cadastrado, fora da transação e fail-closed;
- parciais visíveis só a gestores e auditores; resultado final determinístico;
- mensagens `FORMULARIO` e `SELECAO` para o fluxo server-driven do aplicativo mobile;
- callbacks mobile absolutos com domínio alterável por configuração;
- respostas de erro no padrão Problem Details;
- API versionada em `/api/v1`;
- migrations, logs estruturados, health check e documentação OpenAPI.

## Tecnologias

- Java 21;
- Spring Boot 4.0 (Spring Framework 7, Spring Security 7, Jackson 3);
- PostgreSQL 17;
- Flyway;
- Maven;
- Spring Security (resource server JWT);
- JUnit 5, Mockito, MockMvc, Testcontainers (PostgreSQL) e JaCoCo;
- springdoc-openapi.

## Executando com Docker

Pré-requisito: Docker com o plugin Compose.

```bash
docker compose up --build
```

A API estará disponível em `http://localhost:8080`. O volume `postgres_data` preserva os dados
quando os contêineres são reiniciados. O Compose local define `AUTH_LOCAL_SEED=true`, que cria os
associados sintéticos `gestora` (gestora e eleitora), `auditora`, `associado-1` e `associado-2`, todos
com a senha `coopvote-local`. Esse seed é exclusivo da jornada local.

Verificações rápidas:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api-docs
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"associateId":"gestora","password":"coopvote-local"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -X POST http://localhost:8080/api/v1/mobile -H "Authorization: Bearer $TOKEN"
```

Interfaces de documentação:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- contrato OpenAPI JSON: `http://localhost:8080/api-docs`

## Executando pela IDE

1. Inicie apenas o PostgreSQL com `docker compose up database -d`.
2. Execute `CoopVoteApplication` com uma JDK 21 ou superior.

Os valores padrão de conexão são os mesmos do arquivo `compose.yaml`. Para personalizar o
ambiente, copie `.env.example` para `.env` ou defina as variáveis diretamente no processo.

## Testes e build

Linux ou macOS:

```bash
./mvnw clean verify
```

Windows:

```powershell
.\mvnw.cmd clean verify
```

O relatório de cobertura é criado em `target/site/jacoco/index.html`.

Os testes de integração usam PostgreSQL real via Testcontainers (Docker obrigatório). No GitHub,
`.github/workflows/ci.yml` executa em push e pull request: `mvn clean verify` (testes, cobertura
mínima de 85% de linhas e 70% de branches, contagem de testes executados maior que zero), build da
imagem final, scan Trivy bloqueante para HIGH/CRITICAL e smoke da imagem contra PostgreSQL (login,
pauta, sessão, voto, duplicidade e acesso anônimo negado). Não há deploy na CI.

## Configuração

| Variável | Padrão | Finalidade |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/coopvote` | URL JDBC da aplicação |
| `DB_USERNAME` | `coopvote` | usuário do banco |
| `DB_PASSWORD` | `coopvote` | senha do banco |
| `DB_POOL_SIZE` | `20` | máximo de conexões no pool |
| `DB_POOL_MIN_IDLE` | `5` | mínimo de conexões ociosas |
| `CALLBACK_BASE_URL` | `http://localhost:8080` | domínio usado nas URLs de callback do cliente mobile |
| `ELIGIBILITY_ENABLED` | `true` | consulta o serviço externo quando o identificador é um CPF |
| `ELIGIBILITY_BASE_URL` | `https://user-info.herokuapp.com` | base do serviço de elegibilidade |
| `ELIGIBILITY_TIMEOUT` | `2s` | tempo máximo da consulta de elegibilidade |
| `PORT` | `8080` | porta HTTP da aplicação |
| `AUTH_JWT_SECRET` | *(obrigatório)* | segredo HMAC (≥ 32 bytes) que assina os tokens; sem ele a aplicação não sobe |
| `AUTH_JWT_ISSUER` / `AUTH_JWT_AUDIENCE` | `coopvote-api` | claims `iss` e `aud` validados em cada requisição |
| `AUTH_TOKEN_TTL` | `30m` | validade do token |
| `AUTH_LOCAL_SEED` | `false` | cria associados sintéticos; só para a jornada local |

Credenciais padrão existem apenas para desenvolvimento local. Ambientes compartilhados devem
fornecer segredos próprios.

## Documentação

- [Contrato e exemplos da API](docs/API.md)
- [Protocolo e fluxo de telas mobile](docs/MOBILE.md)
- [Arquitetura e decisões técnicas](docs/ARCHITECTURE.md)
- [Requisitos, premissas e lacunas catalogadas](docs/REQUIREMENTS.md)
- [Fluxo de desenvolvimento e commits](docs/DEVELOPMENT.md)
- [Publicação em nuvem e teste pós-publicação](docs/DEPLOYMENT.md)
- [Validação reproduzível da entrega](docs/VALIDATION.md)
- [Histórico de versões](CHANGELOG.md)
