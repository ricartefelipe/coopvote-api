# Contrato da API

Base path: `/api/v1`

Datas usam ISO 8601 em UTC. Corpos usam UTF-8 e `application/json`. A especificação executável está
disponível em `/api-docs` e a interface Swagger em `/swagger-ui.html`.

O fluxo próprio do aplicativo mobile também faz parte desta API e está detalhado em
[`MOBILE.md`](MOBILE.md).

## Cadastrar pauta

```http
POST /api/v1/agendas
Content-Type: application/json

{
  "title": "Construção de uma nova sede",
  "description": "Deliberação sobre a proposta do conselho."
}
```

Resposta `201 Created`:

```json
{
  "id": "f5bcf2b0-b24d-4e83-9e26-87b9a1a5264a",
  "title": "Construção de uma nova sede",
  "description": "Deliberação sobre a proposta do conselho.",
  "createdAt": "2026-08-28T10:00:00Z"
}
```

O cabeçalho `Location` aponta para `GET /api/v1/agendas/{agendaId}`.

## Abrir sessão

```http
POST /api/v1/agendas/{agendaId}/voting-sessions
Content-Type: application/json

{
  "durationMinutes": 5
}
```

O corpo pode ser omitido; nesse caso a duração é de um minuto. A resposta é `201 Created`:

```json
{
  "id": "b74014f7-52db-41d2-ad4c-6a84c8f5803b",
  "agendaId": "f5bcf2b0-b24d-4e83-9e26-87b9a1a5264a",
  "openedAt": "2026-08-28T10:00:00Z",
  "closesAt": "2026-08-28T10:05:00Z",
  "status": "OPEN"
}
```

## Votar

```http
POST /api/v1/agendas/{agendaId}/votes
Content-Type: application/json

{
  "associateId": "associate-123",
  "vote": "Sim"
}
```

`vote` aceita exclusivamente `Sim` ou `Não`. A resposta `201 Created` confirma o registro.

Quando `associateId` contém 11 dígitos, a API trata o valor como CPF: valida os dígitos
verificadores e consulta o serviço de elegibilidade. Identificadores que não são CPF não passam por
essa consulta. Se o serviço externo não responder a tempo, o voto é aceito.

## Consultar apuração

```http
GET /api/v1/agendas/{agendaId}/result
```

```json
{
  "agendaId": "f5bcf2b0-b24d-4e83-9e26-87b9a1a5264a",
  "sessionId": "b74014f7-52db-41d2-ad4c-6a84c8f5803b",
  "sessionStatus": "CLOSED",
  "yesVotes": 7,
  "noVotes": 3,
  "totalVotes": 10,
  "result": "APPROVED"
}
```

Resultados possíveis: `IN_PROGRESS`, `APPROVED`, `REJECTED` e `TIED`.

## Erros

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "The associate has already voted on agenda '...'.",
  "instance": "/api/v1/agendas/.../votes",
  "code": "DUPLICATE_VOTE",
  "timestamp": "2026-08-28T10:00:10Z",
  "correlationId": "7e652469-10f8-4a7c-84b9-4de89af42289"
}
```

| Código | HTTP | Situação |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | campos ou parâmetros inválidos |
| `INVALID_CPF` | 400 ou 404 | CPF com dígitos inválidos ou não encontrado no serviço externo |
| `ASSOCIATE_UNABLE_TO_VOTE` | 403 | o serviço externo impediu o associado de votar |
| `INVALID_REQUEST_BODY` | 400 | JSON ausente ou malformado |
| `INVALID_PARAMETER` | 400 | identificador em formato inválido |
| `RESOURCE_NOT_FOUND` | 404 | rota inexistente |
| `AGENDA_NOT_FOUND` | 404 | pauta inexistente |
| `VOTING_SESSION_NOT_FOUND` | 404 | sessão ainda não aberta |
| `METHOD_NOT_ALLOWED` | 405 | método HTTP não suportado pelo recurso |
| `VOTING_SESSION_ALREADY_EXISTS` | 409 | tentativa de abrir outra sessão |
| `VOTING_SESSION_CLOSED` | 409 | voto no fechamento ou após ele |
| `DUPLICATE_VOTE` | 409 | associado já votou na pauta |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | `Content-Type` não suportado pelo recurso |
| `INTERNAL_ERROR` | 500 | falha inesperada sem exposição de detalhes internos |

O cliente pode enviar `X-Correlation-ID` com até 100 caracteres seguros. Valores ausentes ou
inválidos são substituídos por um UUID, sempre devolvido no mesmo cabeçalho.
