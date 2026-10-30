# Changelog

Todas as mudanças relevantes deste projeto serão documentadas neste arquivo. O versionamento segue
Semantic Versioning.

## [Unreleased]

## [0.5.0] - 2026-10-29

### Added

- login local do associado (`POST /api/v1/auth/login`) com token Bearer (HS256, issuer, audience,
  expiração) e papéis `VOTER`, `MANAGER` e `AUDITOR`; migration `V2` com a tabela `associate`;
- recibo do voto (`GET /api/v1/agendas/{id}/votes/me`);
- CI no GitHub Actions: `mvn verify` com PostgreSQL via Testcontainers, cobertura mínima, build e scan
  bloqueante da imagem e smoke da imagem;
- contrato OpenAPI `1.0.0` versionado em `docs/openapi.json`.

### Changed

- Spring Boot 3.5.16 → 4.0.9 (Spring Framework 7.0.10, Spring Security 7, Jackson 3, springdoc 3.0.3):
  a imagem final tinha CVEs críticos sem correção na linha 6.2 do Spring Framework
  (CVE-2026-47884/47890 em `spring-webmvc`), além de Tomcat, Jackson e driver PostgreSQL;
- o eleitor é o sujeito do token; `associateId` no corpo do voto deixa de existir e, se enviado, é
  ignorado;
- elegibilidade consultada fora da transação e fail-closed (`503 ELIGIBILITY_UNAVAILABLE`);
- o prazo da sessão é revalidado no instante da gravação; só votos dentro do período entram na
  apuração; parciais só para gestores e auditores;
- somente a violação de `uk_vote_session_associate` vira `409 DUPLICATE_VOTE`; outras falhas de
  integridade não são mascaradas;
- testes de integração migrados de H2 para PostgreSQL real; concorrência provada com 100 tentativas;
- `render.yaml` sem deploy automático a cada commit.

### Removed

- H2 dos testes.


## [0.4.1] - 2026-08-30

### Fixed

- fluxo mobile de voto em sessão encerrada passa a mostrar o resultado, sem erro em inglês;
- mensagens de negócio do voto e da elegibilidade em português.

## [0.4.0] - 2026-08-30

### Added

- virtual threads no servidor HTTP;
- verificação de elegibilidade por CPF, sem alterar o voto por identificador opaco;
- teste de apuração com dois mil votos persistidos.

## [0.3.2] - 2026-08-30

### Fixed

- respostas `404` e `405` para rotas inexistentes e métodos HTTP não suportados, sem convertê-los
  em falhas internas;
- leitura do corpo de erro no smoke test de publicação no Windows.

## [0.3.1] - 2026-08-29

### Fixed

- inicialização do container em plataformas que fornecem a URL de conexão do PostgreSQL;
- composição da URL pública usada nos callbacks do fluxo mobile.

## [0.3.0] - 2026-08-29

### Added

- pipeline do GitLab com build, testes, verificação de cobertura e artefatos;
- Blueprint do Render para aplicação Docker e PostgreSQL gerenciado;
- documentação de publicação e teste automatizado do ambiente publicado.

### Fixed

- resposta `415 UNSUPPORTED_MEDIA_TYPE` para tipos de conteúdo não aceitos pela API.

## [0.2.0] - 2026-08-28

### Added

- protocolo de telas mobile compatível com `FORMULARIO` e `SELECAO`;
- fluxo mobile completo para cadastrar pauta, abrir sessão, votar e consultar a apuração;
- geração de callbacks absolutos a partir de `CALLBACK_BASE_URL`;
- testes integrados do contrato e do fluxo server-driven.

## [0.1.0] - 2026-08-28

### Added

- cadastro e consulta de pautas;
- abertura temporizada de sessões;
- registro de votos com proteção contra duplicidade e concorrência;
- apuração com resultado final e parcial;
- persistência PostgreSQL versionada por Flyway;
- Problem Details, logs estruturados e correlação HTTP;
- documentação OpenAPI e execução com Docker Compose;
- testes unitários, integrados e concorrentes.
