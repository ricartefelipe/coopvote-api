# Contrato da API

Base path: `/api/v1`. Versão do contrato: `1.0.0` (campo `info.version` de `/api-docs`; cópia
versionada em [`openapi.json`](openapi.json)).

Datas usam ISO 8601 em UTC. Corpos usam UTF-8 e `application/json`. A especificação executável está
disponível em `/api-docs` e a interface Swagger em `/swagger-ui.html`.

O fluxo próprio do aplicativo mobile também faz parte desta API e está detalhado em
[`MOBILE.md`](MOBILE.md).

## Autenticação e papéis

Toda rota de negócio exige `Authorization: Bearer <token>`. O token é emitido pelo login com a
credencial local do associado e carrega `iss`, `aud`, `sub` (identificador do associado), `roles` e
`exp` (padrão 30 minutos). Sem token, token inválido, expirado ou com assinatura/issuer/audience
diferentes a resposta é `401`.

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "associateId": "associado-1", "password": "..." }
```

Resposta `200 OK` (`Cache-Control: no-store`):

```json
{ "tokenType": "Bearer", "accessToken": "<jwt>", "expiresAt": "2026-08-28T10:30:00Z", "roles": ["VOTER"] }
```

Credencial inválida, associado inexistente ou inativo recebem `401 INVALID_CREDENTIALS`, sem
distinguir a causa.

| Papel | Pode |
|---|---|
| `VOTER` | votar e consultar o próprio recibo; ver apuração final (parciais ocultas) |
| `MANAGER` | cadastrar pautas, abrir sessões e ver parciais; também vota se tiver `VOTER` |
| `AUDITOR` | ver parciais e resultado; não vota nem gere |

Operação sem o papel exigido responde `403 FORBIDDEN`.

## Cadastrar pauta (MANAGER)

```http
POST /api/v1/agendas
Authorization: Bearer <token>
Content-Type: application/json

{ "title": "Construção de uma nova sede", "description": "Deliberação sobre a proposta do conselho." }
```

Resposta `201 Created` com `id`, `title`, `description`, `createdAt` e o cabeçalho `Location`
apontando para `GET /api/v1/agendas/{agendaId}`.

## Abrir sessão (MANAGER)

```http
POST /api/v1/agendas/{agendaId}/voting-sessions
Authorization: Bearer <token>
Content-Type: application/json

{ "durationMinutes": 5 }
```

O corpo pode ser omitido; nesse caso a duração é de um minuto. Resposta `201 Created` com `id`,
`agendaId`, `openedAt`, `closesAt` e `status`. Uma pauta tem no máximo uma sessão (`409
VOTING_SESSION_ALREADY_EXISTS`).

## Votar (VOTER)

```http
POST /api/v1/agendas/{agendaId}/votes
Authorization: Bearer <token>
Content-Type: application/json

{ "vote": "Sim" }
```

O eleitor é o sujeito do token; qualquer `associateId` enviado no corpo é ignorado. A sessão
precisa estar aberta **no instante da gravação** (relógio do servidor): a elegibilidade externa é
consultada antes e fora da transação, e o prazo é revalidado logo antes de persistir. Resposta
`201 Created`:

```json
{ "id": "…", "agendaId": "…", "associateId": "associado-1", "vote": "Sim", "castAt": "2026-08-28T10:01:00Z" }
```

| Situação | Resposta |
|---|---|
| sessão inexistente | `404 VOTING_SESSION_NOT_FOUND` |
| sessão encerrada (inclusive se a elegibilidade respondeu depois do fechamento) | `409 VOTING_SESSION_CLOSED` |
| segundo voto do mesmo associado (constraint `uk_vote_session_associate`) | `409 DUPLICATE_VOTE` |
| associado inativo, CPF cadastrado inválido, não encontrado ou inabilitado | `403` (`ASSOCIATE_INACTIVE`, `INVALID_CPF`, `ASSOCIATE_NOT_FOUND`, `ASSOCIATE_UNABLE_TO_VOTE`) |
| serviço de elegibilidade sem resposta conclusiva | `503 ELIGIBILITY_UNAVAILABLE` — o voto **não** é registrado; tentar de novo |

## Recibo do voto (VOTER)

```http
GET /api/v1/agendas/{agendaId}/votes/me
Authorization: Bearer <token>
```

Devolve o voto do associado autenticado nesta pauta (`200`) ou `404 VOTE_NOT_FOUND`. Depois de um
timeout o cliente consulta o recibo antes de repetir o envio; repetir o envio nunca duplica o voto.

## Consultar apuração

```http
GET /api/v1/agendas/{agendaId}/result
Authorization: Bearer <token>
```

```json
{ "agendaId": "…", "sessionId": "…", "sessionStatus": "CLOSED", "yesVotes": 2, "noVotes": 1, "totalVotes": 3, "result": "APPROVED" }
```

Enquanto `sessionStatus` é `OPEN`, `result` é `IN_PROGRESS` e os totais só aparecem para `MANAGER`
e `AUDITOR`; para os demais os campos de contagem são omitidos. Após `closesAt` o resultado é
final e determinístico: só contam votos gravados dentro do período da sessão, e repetir a consulta
não altera o resultado. Maioria de `Sim` aprova, maioria de `Não` rejeita, igualdade empata.

## Erros

Erros seguem Problem Details (`application/problem+json`) com `status`, `title`, `detail`, `code`
estável e, em validação, `errors` por campo. Códigos: `AUTHENTICATION_REQUIRED`/`401`,
`INVALID_CREDENTIALS`, `FORBIDDEN`, `VALIDATION_ERROR`, `INVALID_REQUEST_BODY`, `AGENDA_NOT_FOUND`,
`VOTING_SESSION_NOT_FOUND`, `VOTING_SESSION_ALREADY_EXISTS`, `VOTING_SESSION_CLOSED`,
`DUPLICATE_VOTE`, `VOTE_NOT_FOUND`, `ASSOCIATE_INACTIVE`, `INVALID_CPF`, `ASSOCIATE_NOT_FOUND`,
`ASSOCIATE_UNABLE_TO_VOTE`, `ELIGIBILITY_UNAVAILABLE`. Falhas de banco que não sejam a violação
de voto único não são traduzidas como duplicidade: respondem `500` sem detalhes técnicos.
