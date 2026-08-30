# Changelog

Todas as mudanças relevantes deste projeto serão documentadas neste arquivo. O versionamento segue
Semantic Versioning.

## [Unreleased]

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
