# Requisitos, premissas e lacunas

## Requisitos atendidos pelo backend

| Requisito | Implementação |
|---|---|
| Cadastrar pauta | `POST /api/v1/agendas` |
| Abrir sessão com duração informada | `POST /api/v1/agendas/{id}/voting-sessions` |
| Usar um minuto por padrão | corpo ausente ou `durationMinutes` ausente |
| Receber somente `Sim` ou `Não` | validação do enum no JSON e restrição no banco |
| Um voto por associado e pauta | validação no serviço e chave única no banco |
| Persistir após reinício | PostgreSQL em volume Docker e migrations Flyway |
| Contabilizar e informar resultado | `GET /api/v1/agendas/{id}/result` |
| API REST em JSON | recursos versionados em `/api/v1` |
| Mensagens de tela para o aplicativo | fluxo POST em `/api/v1/mobile` |
| Tela `FORMULARIO` | cadastro, abertura, votação e resultado |
| Tela `SELECAO` | navegação entre pautas e ações disponíveis |
| Campos do anexo | `TEXTO`, `INPUT_TEXTO` e `INPUT_NUMERO` |
| POST definido por botões e itens | cada ação contém `url` absoluta e `body` quando necessário |
| Domínio dos callbacks configurável | variável `CALLBACK_BASE_URL` |
| Erros e exceções | Problem Details e códigos de negócio |
| Documentação da API | Swagger UI e OpenAPI JSON |
| Logs | JSON estruturado e correlação por requisição |
| Testes | domínio, serviços, HTTP, persistência e concorrência |

## Premissas adotadas

O documento não define todos os comportamentos de borda. Para manter a API determinística foram
adotadas estas premissas:

1. Uma pauta possui título obrigatório e descrição opcional.
2. Cada pauta pode ter uma única sessão e não pode ser reaberta.
3. A duração é informada em minutos inteiros positivos.
4. O instante de fechamento não aceita novos votos.
5. A contagem pode ser consultada durante a sessão, mas o resultado é `IN_PROGRESS`.
6. Após o fechamento, maioria de `Sim` aprova, maioria de `Não` rejeita e contagens iguais empatam.
7. Uma sessão sem votos termina empatada.
8. O identificador do associado é um texto opaco de até 100 caracteres; não é presumido como CPF.
9. IDs de recursos são UUIDs gerados pelo servidor.
10. Horários do contrato são instantes ISO 8601 em UTC.
11. Não foi criado endpoint de domínio para atualização, exclusão ou listagem de pautas porque não
    são solicitados.
12. A listagem existente no fluxo mobile é apenas navegação e não amplia a API de domínio.
13. O ponto de entrada do cliente é `POST /api/v1/mobile`, ausente no documento.
14. Os callbacks mobile bem-sucedidos retornam sempre a próxima tela com HTTP 200.

## Lacunas catalogadas no material recebido

Estas lacunas não impedem a API de domínio, mas exigem confirmação antes da integração final com o
aplicativo:

| Lacuna | Impacto |
|---|---|
| O documento não define o ponto de entrada do aplicativo | adotado `POST /api/v1/mobile` |
| O documento define os tipos de tela, mas não as telas do fluxo de votação | criado um fluxo mínimo para cadastro, abertura, voto e apuração |
| O comportamento do aplicativo diante de respostas de erro não é definido | mantido Problem Details com semântica HTTP e códigos estáveis |
| Unidade e limite máximo da duração não são definidos | adotados minutos positivos, sem teto funcional arbitrário |
| Reabertura e múltiplas sessões não são definidas | adotada uma sessão por pauta |
| Consulta de parcial e momento da apuração não são definidos | parcial liberada com resultado `IN_PROGRESS` |
| Critério de aprovação e comportamento de empate não são definidos | adotada maioria simples e empate explícito |
| Campos que compõem uma pauta não são definidos | adotados título e descrição |
| Relação entre ID único e CPF do bônus não é definida | o núcleo usa ID opaco; CPF não foi acoplado ao voto |
| Respostas completas do serviço externo não constam no texto | o bônus de elegibilidade não foi implementado nesta versão |
| Serviço externo informado pode estar indisponível e responde aleatoriamente | faltam política de indisponibilidade e estratégia determinística de testes |
| Não há metas de latência, vazão ou volume para performance | estrutura otimizada, mas sem critério objetivo de aceite de carga |
| Status HTTP e formato de erros não são especificados | adotadas semânticas REST e Problem Details |
