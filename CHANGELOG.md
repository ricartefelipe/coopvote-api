# Changelog

Todas as mudanças relevantes deste projeto serão documentadas neste arquivo. O versionamento segue
Semantic Versioning.

## [Unreleased]

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
