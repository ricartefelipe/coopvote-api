# CoopVote API

API REST para cadastrar pautas, abrir sessões temporizadas, receber os votos dos associados e
publicar a apuração. O projeto prioriza um desenho simples, regras de negócio explícitas e
execução reproduzível.

## Funcionalidades

- cadastro e consulta de pautas;
- abertura de uma única sessão por pauta, com duração configurável e padrão de um minuto;
- votos `Sim` ou `Não`, limitados a um voto por associado em cada pauta;
- encerramento calculado pelo relógio, sem processo agendado;
- apuração agregada pelo banco, adequada a grandes volumes;
- respostas de erro no padrão Problem Details;
- API versionada em `/api/v1`;
- migrations, logs estruturados, health check e documentação OpenAPI.

## Tecnologias

- Java 21;
- Spring Boot 3.5;
- PostgreSQL 17;
- Flyway;
- Maven;
- JUnit 5, Mockito, MockMvc, H2 e JaCoCo;
- springdoc-openapi.

## Executando com Docker

Pré-requisito: Docker com o plugin Compose.

```bash
docker compose up --build
```

A API estará disponível em `http://localhost:8080`. O volume `postgres_data` preserva os dados
quando os contêineres são reiniciados.

Verificações rápidas:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api-docs
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

## Configuração

| Variável | Padrão | Finalidade |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/coopvote` | URL JDBC da aplicação |
| `DB_USERNAME` | `coopvote` | usuário do banco |
| `DB_PASSWORD` | `coopvote` | senha do banco |
| `DB_POOL_SIZE` | `20` | máximo de conexões no pool |
| `DB_POOL_MIN_IDLE` | `5` | mínimo de conexões ociosas |
| `CALLBACK_BASE_URL` | `http://localhost:8080` | domínio reservado para callbacks do cliente mobile |

Credenciais padrão existem apenas para desenvolvimento local. Ambientes compartilhados devem
fornecer segredos próprios.

## Documentação

- [Contrato e exemplos da API](docs/API.md)
- [Arquitetura e decisões técnicas](docs/ARCHITECTURE.md)
- [Requisitos, premissas e lacunas catalogadas](docs/REQUIREMENTS.md)
- [Fluxo de desenvolvimento e commits](docs/DEVELOPMENT.md)
- [Histórico de versões](CHANGELOG.md)

