# Publicação em nuvem

O ambiente de avaliação usa Render com uma aplicação Docker e um PostgreSQL gerenciado. Os dois
recursos são declarados em `render.yaml`, inclusive região, health check e variáveis obtidas do
banco sem expor credenciais no repositório.

## Criar o ambiente

1. Integre a release validada na branch `main`.
2. Entre no Render e conecte a conta do GitLab.
3. Autorize o acesso ao projeto privado `ricarte-group/coopvote-api`.
4. No Dashboard, selecione **New > Blueprint** e escolha esse repositório.
5. Confirme `render.yaml` como Blueprint e crie os recursos gratuitos declarados.
6. Aguarde o deploy concluir e o health check ficar saudável.

O serviço acompanha somente a branch `main`. Assim, features e integrações em `develop` não são
publicadas antes de comporem uma release.

## Endereços de verificação

Ambiente publicado para avaliação: `https://coopvote-api.onrender.com`.

- saúde: `https://coopvote-api.onrender.com/actuator/health`;
- Swagger UI: `https://coopvote-api.onrender.com/swagger-ui.html`;
- OpenAPI JSON: `https://coopvote-api.onrender.com/api-docs`;
- entrada do aplicativo: `POST https://coopvote-api.onrender.com/api/v1/mobile`.

O domínio usado nos callbacks mobile é derivado automaticamente do endereço público do serviço.

## Teste pós-publicação

No PowerShell, execute:

```powershell
.\scripts\smoke-test.ps1 -BaseUrl https://coopvote-api.onrender.com
```

O teste cria uma pauta identificada como smoke test, abre uma sessão, registra um voto, confere a
apuração e valida a rejeição de voto duplicado. O resultado esperado contém `status = PASSED`.

## Limitações do ambiente gratuito

O serviço pode suspender após um período sem acessos, causando demora na primeira requisição. O
PostgreSQL gratuito expira 30 dias após a criação e não possui backup. Essas limitações são
aceitáveis somente para a avaliação; uma operação contínua deve usar planos persistentes.
