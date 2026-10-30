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
| Testes | domínio, serviços, HTTP, persistência, concorrência e volume |
| Elegibilidade por CPF | consulta ao serviço externo quando o identificador é um CPF |
| Versionamento da API | prefixo `/api/v1` |
| Virtual threads | `spring.threads.virtual.enabled` |

## Premissas adotadas

O documento não define todos os comportamentos de borda. Para manter a API determinística foram
adotadas estas premissas:

1. Uma pauta possui título obrigatório e descrição opcional.
2. Cada pauta pode ter uma única sessão e não pode ser reaberta.
3. A duração é informada em minutos inteiros positivos.
4. O instante de fechamento não aceita novos votos; o que vale é o relógio do servidor no momento
   da gravação, revalidado depois da consulta de elegibilidade. Votos gravados fora do período não
   entram na apuração.
5. Durante a sessão o resultado é `IN_PROGRESS`; as contagens parciais só são exibidas a gestores e
   auditores. Eleitores veem os totais após o encerramento.
6. Após o fechamento, maioria de `Sim` aprova, maioria de `Não` rejeita e contagens iguais empatam.
7. Uma sessão sem votos termina empatada.
8. O eleitor é o associado autenticado (sujeito do token emitido no login local). O identificador
   é um texto opaco de até 100 caracteres; o CPF, quando existir, fica no cadastro e serve só à
   elegibilidade — nunca é credencial nem vem no corpo da requisição.
15. **Modalidade do voto: identificável e auditável, não secreto.** O voto fica vinculado ao
    associado no banco e no recibo; a API e o aplicativo não prometem anonimato. Voto secreto
    exigiria separar elegibilidade da cédula e uma análise de ameaças própria (P2).
16. Papéis: `VOTER` vota, `MANAGER` cadastra pautas e abre sessões, `AUDITOR` acompanha parciais.
17. Elegibilidade externa fail-closed: sem resposta conclusiva do provedor o voto não é aceito
    (`503`), e o eleitor tenta de novo; erro técnico nunca vira elegibilidade.
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
| Consulta de parcial e momento da apuração não são definidos | parcial só para gestor/auditor, resultado `IN_PROGRESS`; eleitores veem totais após o encerramento |
| Critério de aprovação e comportamento de empate não são definidos | adotada maioria simples e empate explícito |
| Campos que compõem uma pauta não são definidos | adotados título e descrição |
| Relação entre ID único e CPF do bônus não é definida | CPF é um atributo opcional do cadastro do associado; a elegibilidade só é consultada quando ele existe |
| Serviço externo informado pode estar indisponível e responde aleatoriamente | timeout curto, consulta fora da transação, testes com dublê e voto **recusado** (`503`) se o serviço falhar |
| Não há metas de latência, vazão ou volume para performance | apuração no banco e teste com 2.000 votos abaixo de 1s |
| Status HTTP e formato de erros não são especificados | adotadas semânticas REST e Problem Details |
