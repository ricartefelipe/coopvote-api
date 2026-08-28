# Protocolo de telas mobile

## Visão geral

O ponto de entrada é `POST /api/v1/mobile`. Todas as ações devolvidas nas telas também usam POST,
conforme o protocolo do anexo. A resposta de cada ação é a próxima tela que o aplicativo deve
montar.

O fluxo não possui regras de negócio próprias: cadastro, sessão, voto e apuração usam os mesmos
services da API de domínio. O valor de `CALLBACK_BASE_URL` define o esquema, host e porta de todas
as URLs entregues ao cliente.

## Tela SELECAO

Uma seleção contém título e itens acionáveis:

```json
{
  "tipo": "SELECAO",
  "titulo": "Pautas",
  "itens": [
    {
      "texto": "Cadastrar nova pauta",
      "url": "http://localhost:8080/api/v1/mobile/agendas/new"
    }
  ]
}
```

Cada item pode incluir `body`. Ao selecionar um item, o aplicativo envia esse corpo por POST para
a URL informada.

## Tela FORMULARIO

Um formulário contém componentes em `itens` e um ou dois botões:

```json
{
  "tipo": "FORMULARIO",
  "titulo": "Registrar voto",
  "itens": [
    {
      "tipo": "TEXTO",
      "texto": "Construção de uma nova sede"
    },
    {
      "tipo": "INPUT_TEXTO",
      "id": "associateId",
      "titulo": "Identificador do associado"
    }
  ],
  "botaoOk": {
    "texto": "Sim",
    "url": "http://localhost:8080/api/v1/mobile/agendas/{agendaId}/votes",
    "body": {
      "vote": "Sim"
    }
  },
  "botaoCancelar": {
    "texto": "Não",
    "url": "http://localhost:8080/api/v1/mobile/agendas/{agendaId}/votes",
    "body": {
      "vote": "Não"
    }
  }
}
```

Antes do POST, o cliente adiciona ao `body` os valores digitados, usando o `id` de cada campo como
chave. No exemplo, o corpo final contém `associateId` e `vote`.

Componentes usados pelo backend:

| Tipo | Finalidade |
|---|---|
| `TEXTO` | instruções e dados somente para leitura |
| `INPUT_TEXTO` | título, descrição e identificador do associado |
| `INPUT_NUMERO` | duração da sessão em minutos |

O anexo também descreve `INPUT_DATA`, mas o fluxo de votação não possui um dado de data a coletar.

## Fluxo de callbacks

| Callback | Próxima tela ou operação |
|---|---|
| `POST /api/v1/mobile` | lista de pautas e opção de cadastro |
| `POST /api/v1/mobile/agendas/new` | formulário de pauta |
| `POST /api/v1/mobile/agendas` | cadastra e mostra as ações da pauta |
| `POST /api/v1/mobile/agendas/{id}` | ações válidas para o estado corrente |
| `POST /api/v1/mobile/agendas/{id}/voting-sessions/new` | formulário de duração |
| `POST /api/v1/mobile/agendas/{id}/voting-sessions` | abre a sessão |
| `POST /api/v1/mobile/agendas/{id}/votes/new` | formulário com as opções Sim e Não |
| `POST /api/v1/mobile/agendas/{id}/votes` | registra o voto |
| `POST /api/v1/mobile/agendas/{id}/result` | tela de apuração |

A tela de ações é orientada pelo estado: antes da abertura oferece a criação da sessão; durante a
sessão oferece voto e apuração; após o fechamento oferece apenas a apuração.

## Erros

Falhas de validação, recursos inexistentes e conflitos mantêm o contrato Problem Details descrito
em [`API.md`](API.md). O documento de origem não define uma tela ou envelope próprio para erros;
por isso a resposta preserva códigos HTTP e códigos de negócio estáveis em vez de criar um formato
não especificado.
