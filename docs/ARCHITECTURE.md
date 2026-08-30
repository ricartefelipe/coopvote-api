# Arquitetura

## Visão geral

A aplicação é um monólito modular organizado por funcionalidade. Essa escolha mantém implantação,
transações e diagnóstico simples, sem impedir que as responsabilidades do domínio sejam localizadas.

```text
HTTP / JSON
    |
    +-- agenda.api ------ AgendaService ------ AgendaRepository
    |
    +-- voting.api ----- VotingService ------- VotingSessionRepository
    |                          |               VoteRepository
    |                          |
    |                    regras temporais
    |
    +-- mobile.api ----- MobileScreenFactory
    |
    +-- eligibility ---- UserInfoClient
                               |
                         AgendaService + VotingService

PostgreSQL + Flyway
```

Os módulos atuais são:

- `agenda`: cadastro, consulta e persistência das pautas;
- `voting`: sessão, voto, unicidade e apuração;
- `mobile`: adaptação do domínio para mensagens `FORMULARIO` e `SELECAO`;
- `eligibility`: consulta opcional de CPF ao serviço externo, sem alterar o voto por ID opaco;
- `shared`: configuração transversal, correlação HTTP e tratamento de erros.

Controllers limitam-se ao contrato HTTP. Services delimitam casos de uso e transações. Entidades
protegem invariantes locais. Repositórios concentram o acesso aos dados. Não foram adicionadas
interfaces intermediárias sem uma segunda implementação concreta.

A camada `mobile` não replica regras de negócio. Seus controllers chamam os mesmos services usados
pela API de domínio e o `MobileScreenFactory` limita-se a montar o protocolo de apresentação. As
ações são URLs absolutas de callback construídas a partir de `CALLBACK_BASE_URL`.

## Modelo de dados

```text
agenda 1 --- 0..1 voting_session 1 --- 0..N vote
```

- `voting_session.agenda_id` é único: uma pauta não pode ser reaberta nem ter sessões sobrepostas;
- `(voting_session_id, associate_id)` é único: o banco garante a regra mesmo sob concorrência;
- `(voting_session_id, vote_option)` é indexado para favorecer a apuração;
- datas são persistidas como instantes com fuso e interpretadas em UTC.

## Decisões principais

### Encerramento calculado

Uma sessão está aberta no intervalo `openedAt <= instante < closesAt`. Não existe tarefa agendada
para alterar seu estado. Assim, reinícios não perdem o encerramento e múltiplas instâncias não
precisam coordenar timers.

### Concorrência dos votos

O caso de uso faz uma consulta antecipada para fornecer erro legível. A restrição única no banco é
a autoridade final e evita dois votos quando requisições concorrentes passam juntas pela consulta.

### Apuração no banco

`VoteRepository` agrupa por opção e retorna somente as contagens. O consumo de memória da API não
cresce com a quantidade de votos. A suíte inclui um teste com dois mil votos persistidos.

### Virtual threads

O servidor HTTP usa virtual threads do Java 21. A decisão favorece o cenário de muitos associados
votando ao mesmo tempo, sem mudar o modelo transacional nem introduzir filas.

### Elegibilidade por CPF

Quando o `associateId` contém 11 dígitos, a API valida o CPF e consulta
`GET {baseUrl}/users/{cpf}`. Identificadores que não são CPF seguem o fluxo original. Se o serviço
externo estiver indisponível ou exceder o timeout, o voto prossegue e o evento é registrado em
`WARN`, para a dependência externa não impedir a assembleia.

### Persistência e migrations

PostgreSQL é o banco da aplicação. Flyway versiona o esquema e o Hibernate apenas o valida. H2 em
modo de compatibilidade PostgreSQL é restrito aos testes automatizados.

### Erros

Erros seguem `application/problem+json`, com código estável, instante, caminho, detalhes de campos
e identificador de correlação. Exceções inesperadas não expõem stack trace nem detalhes internos ao
cliente.

### Observabilidade

Os logs de produção são JSON no formato Logstash. Eventos de criação e abertura ficam em `INFO`;
votos aceitos ficam em `DEBUG` para não produzir um log por voto em cenários de volume. O cabeçalho
`X-Correlation-ID` é propagado ou gerado pela API, e o mesmo valor integra os logs e respostas de
erro. O Actuator expõe `health`, `info` e `metrics`.

## Decisões deliberadamente adiadas

- autenticação e autorização, abstraídas pelo enunciado;
- cache de resultados, desnecessário sem uma meta de carga demonstrada;
- mensageria, pois o fluxo atual é síncrono e transacional;
- separação em microsserviços, sem fronteira operacional que justifique o custo;
- paginação de pautas no protocolo mobile, pois o anexo não define paginação e o escopo esperado é
  reduzido.
