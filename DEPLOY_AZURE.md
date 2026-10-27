# Roteiro de publicação — Docker Hub, Azure SQL e Container Apps

Este roteiro traduz as anotações da aula para este projeto. Execute os comandos no **PowerShell**, a partir da raiz do repositório. Valores entre aspas com `SEU-...` precisam ser substituídos.

> Não coloque tokens ou senhas em commits, no README ou no arquivo de entrega.

## 1. Publicar a imagem pelo GitHub Actions

No Docker Hub:

1. crie um repositório **público** chamado `cp-microservice`;
2. em **Account settings → Personal access tokens**, crie um token com permissão de leitura e escrita;
3. copie o token: ele só será exibido uma vez.

No GitHub, abra `Settings → Secrets and variables → Actions` e crie:

- variável `DOCKER_USER` com seu usuário do Docker Hub;
- secret `DOCKER_TOKEN` com o token do Docker Hub.

Depois envie o código para a branch `main`. O workflow `.github/workflows/continuous_delivery.yml` executará os testes e publicará quatro tags, incluindo `latest` e uma tag com o SHA curto do commit.

Confirme em **GitHub → Actions** que a execução ficou verde e em **Docker Hub → Tags** que a imagem apareceu antes de seguir.

## 2. Preparar o Azure CLI

Instale o Azure CLI, abra um novo PowerShell e execute:

```powershell
az login
az account show --output table
az extension add --name containerapp --upgrade
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights
```

Defina nomes únicos. O nome do SQL Server precisa ser globalmente exclusivo e usar apenas letras minúsculas, números e hífens.

```powershell
$suffix = "SEU-RM-OU-INICIAIS"
$location = "canadacentral"
$resourceGroup = "rg-cp04-$suffix"
$sqlServer = "sql-cp04-$suffix"
$databaseName = "api"
$dbUser = "sqladmincp04"
$containerEnvironment = "env-cp04-$suffix"
$containerApp = "api-cp04-$suffix"
$dockerUser = "SEU-USUARIO-DOCKER-HUB"
$imageTag = "latest"
$image = "$dockerUser/cp-microservice:$imageTag"
$dbPassword = Read-Host "Crie uma senha forte para o Azure SQL"
```

Use uma senha forte, com maiúsculas, minúsculas, números e símbolo. A variável existe apenas nessa sessão do PowerShell.

## 3. Criar o grupo, o SQL Server e o banco

```powershell
az group create --name $resourceGroup --location $location

az sql server create `
  --name $sqlServer `
  --resource-group $resourceGroup `
  --location $location `
  --admin-user $dbUser `
  --admin-password $dbPassword

az sql db create `
  --name $databaseName `
  --server $sqlServer `
  --resource-group $resourceGroup `
  --edition GeneralPurpose `
  --family Gen5 `
  --capacity 2 `
  --compute-model Serverless `
  --use-free-limit true `
  --free-limit-exhaustion-behavior AutoPause
```

O parâmetro `--use-free-limit true` é importante: sem ele, a Azure pode criar um banco serverless faturável. Se sua assinatura não oferecer a modalidade gratuita, pare e revise o resumo de custos no portal antes de continuar.

## 4. Liberar as conexões necessárias

A regra `0.0.0.0` permite conexões de serviços Azure, incluindo o Container App:

```powershell
az sql server firewall-rule create `
  --resource-group $resourceGroup `
  --server $sqlServer `
  --name AllowAzureServices `
  --start-ip-address 0.0.0.0 `
  --end-ip-address 0.0.0.0
```

Para conectar pelo DBeaver em seu computador, descubra seu IPv4 público e informe-o quando solicitado:

```powershell
$clientIp = Read-Host "Seu IPv4 publico atual"

az sql server firewall-rule create `
  --resource-group $resourceGroup `
  --server $sqlServer `
  --name AllowMyComputer `
  --start-ip-address $clientIp `
  --end-ip-address $clientIp
```

Não use uma faixa aberta de `0.0.0.0` até `255.255.255.255`. A regra do seu computador pode ser removida depois dos testes.

## 5. Criar as tabelas no Azure SQL

Obtenha o host:

```powershell
$sqlHost = az sql server show `
  --name $sqlServer `
  --resource-group $resourceGroup `
  --query fullyQualifiedDomainName `
  --output tsv

$sqlHost
```

No DBeaver, crie uma conexão SQL Server com:

| Campo | Valor |
|---|---|
| Host | valor exibido em `$sqlHost` |
| Porta | `1433` |
| Database | `api` |
| Usuário | `sqladmincp04` |
| Senha | a senha criada no passo 2 |
| Encrypt | `true` |
| Trust server certificate | `false` |

Abra e execute todo o arquivo `database/sqlserver-schema.sql`. Confirme que as tabelas `dbo.financas` e `dbo.futebois` foram criadas. O profile de produção usa `ddl-auto=validate`, então o Container App não inicia se esta etapa for omitida.

## 6. Criar o ambiente do Container Apps

```powershell
az containerapp env create `
  --name $containerEnvironment `
  --resource-group $resourceGroup `
  --location $location
```

## 7. Implantar a API

O repositório no Docker Hub precisa estar público. A senha do banco será criada como secret do Container Apps e referenciada pela variável `DB_PWD`.

```powershell
az containerapp create `
  --name $containerApp `
  --resource-group $resourceGroup `
  --environment $containerEnvironment `
  --image $image `
  --ingress external `
  --target-port 8080 `
  --min-replicas 1 `
  --max-replicas 1 `
  --cpu 0.5 `
  --memory 1.0Gi `
  --secrets "db-pwd=$dbPassword" `
  --env-vars `
    "SPRING_PROFILES_ACTIVE=prd" `
    "DB_SERVER_URL=$sqlHost" `
    "DB_SERVER_PORT=1433" `
    "DB_SCHEMA=$databaseName" `
    "DB_USER=$dbUser" `
    "DB_PWD=secretref:db-pwd" `
    "DB_ENCRYPT=true" `
    "DB_TRUST_SERVER_CERTIFICATE=false" `
    "DB_DDL_AUTO=validate" `
    "CORS_ALLOWED_ORIGIN_PATTERNS=https://*.azurecontainerapps.io"
```

`--min-replicas 1` mantém uma instância ativa para a apresentação, mas pode consumir a franquia. Depois da avaliação, você pode usar `az containerapp update --min-replicas 0` ou remover o grupo de recursos.

## 8. Obter a URL e validar

```powershell
$fqdn = az containerapp show `
  --name $containerApp `
  --resource-group $resourceGroup `
  --query properties.configuration.ingress.fqdn `
  --output tsv

$baseUrl = "https://$fqdn"
$baseUrl

Invoke-WebRequest "$baseUrl/" -UseBasicParsing
Invoke-RestMethod "$baseUrl/v3/api-docs"
Invoke-RestMethod "$baseUrl/api/v1/financas"
Invoke-RestMethod "$baseUrl/api/v1/copa"
```

Teste persistência real com o CRUD de Finanças:

```powershell
$body = @{
  emissor = "Tesouro Nacional"
  taxa = 12.5
  risco = "baixo"
  vencimento = "2030-01-01"
  quantidade = 10
} | ConvertTo-Json

$criada = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/v1/financas" `
  -ContentType "application/json" `
  -Body $body

$criada
Invoke-RestMethod "$baseUrl/api/v1/financas/$($criada.id)"
```

No DBeaver, execute `SELECT * FROM dbo.financas;` e confirme que o registro está no Azure SQL. Depois, faça ao menos um `PUT` e um `DELETE` pelo Swagger para comprovar o CRUD completo.

Se a aplicação não iniciar, consulte os logs:

```powershell
az containerapp logs show `
  --name $containerApp `
  --resource-group $resourceGroup `
  --follow
```

## 9. Atualizar a imagem sem reutilizar `latest`

Para uma nova versão, prefira a tag do SHA curto exibida no Docker Hub/GitHub Actions:

```powershell
$imageTag = "SHA-CURTO-DA-NOVA-VERSAO"
$image = "$dockerUser/cp-microservice:$imageTag"

az containerapp update `
  --name $containerApp `
  --resource-group $resourceGroup `
  --image $image
```

Isso cria uma revisão rastreável e evita que o Azure reutilize uma imagem antiga em cache.

## 10. Finalizar a entrega

Preencha `outputs/ENTREGA_CP3.txt` com:

1. URL do GitHub;
2. URL pública da imagem no Docker Hub;
3. URL pública do Container App;
4. URL do Swagger (`URL_DO_CONTAINER_APP/`);
5. nome e RM de cada integrante.

Abra todas as URLs em uma janela anônima antes de enviar. Capture também as evidências de Actions verde, tags do Docker Hub, Swagger, operações CRUD e dados no Azure SQL.

## 11. Limpeza após a correção

Quando a prova tiver sido corrigida e você tiver certeza de que os recursos não são mais necessários, o grupo inteiro pode ser removido pelo portal ou pelo comando abaixo:

```powershell
az group delete --name $resourceGroup --yes --no-wait
```

Esse comando exclui o Container App, o ambiente e o banco, portanto não o execute antes da correção.

Referências oficiais: [Azure SQL gratuito](https://learn.microsoft.com/azure/azure-sql/database/free-tier), [variáveis e secrets no Container Apps](https://learn.microsoft.com/azure/container-apps/environment-variables) e [ingress do Container Apps](https://learn.microsoft.com/azure/container-apps/ingress-how-to).
