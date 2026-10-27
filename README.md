# Check Point 3 — API REST em Azure

API REST de **Finanças** e **Copa do Mundo**, preparada para o Check Point 3 de Microservices and Web Engineering. O projeto oferece CRUD completo, persistência em SQL Server, documentação OpenAPI, imagem Docker, Continuous Delivery para o Docker Hub e configuração para Azure Container Apps.

## Repositório e entrega

- GitHub: [Lucas-RA/CP04_MICROSERVICE](https://github.com/Lucas-RA/CP04_MICROSERVICE)
- Docker Hub: [270506lu/cp-microservice](https://hub.docker.com/r/270506lu/cp-microservice)
- Azure: [API no Azure Container Apps](https://api-cp04-270506lu.grayground-9a5a744a.canadacentral.azurecontainerapps.io/)
- Swagger: [Swagger UI](https://api-cp04-270506lu.grayground-9a5a744a.canadacentral.azurecontainerapps.io/swagger-ui/index.html)

O roteiro completo para publicar está em [DEPLOY_AZURE.md](DEPLOY_AZURE.md). O arquivo final exigido pela prova está em [ENTREGA_CP3.txt](ENTREGA_CP3.txt).

## Tecnologias

- Java 17 e Spring Boot 4.0.3
- Spring MVC, Spring Data JPA, Hibernate e Bean Validation
- SQL Server em desenvolvimento e Azure SQL em produção
- Swagger/OpenAPI com Springdoc
- Maven Wrapper, Docker e GitHub Actions
- H2 apenas nos testes automatizados

## Arquitetura

```text
Cliente HTTP → Controller → DTO/Mapper → Service → Repository → SQL Server
```

As entidades usam IDs `BIGINT IDENTITY`, e os contratos HTTP usam DTOs separados para entrada e saída.

## Endpoints

Swagger UI local: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)<br>
OpenAPI JSON local: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

| Domínio | Operações | Rota-base |
|---|---|---|
| Finanças | `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` | `/api/v1/financas` |
| Copa do Mundo | `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` | `/api/v1/copa` |

Exemplo de Finanças:

```json
{
  "emissor": "Tesouro Nacional",
  "taxa": 12.5,
  "risco": "baixo",
  "vencimento": "2030-01-01",
  "quantidade": 10
}
```

Exemplo de Copa do Mundo:

```json
{
  "ano": 2022,
  "capeao": "Argentina",
  "vice": "França",
  "sede": "Catar",
  "melhorJogador": "Lionel Messi"
}
```

## Executar testes e gerar o pacote

Os testes usam um banco H2 isolado; não exigem SQL Server ou Azure.

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

O pacote executável é gerado em `target/app.jar`.

## Executar localmente com SQL Server

1. Copie `.env.example` para `.env` apenas como referência local. O Spring não carrega esse arquivo automaticamente.
2. Defina uma senha forte no PowerShell, sem salvá-la no repositório.
3. Inicie o SQL Server, crie o banco `api` e execute a aplicação.

```powershell
$env:DB_PWD = Read-Host "Senha local do SQL Server"

docker run -d --name cp04-sqlserver --rm `
  -e "ACCEPT_EULA=Y" `
  -e "MSSQL_SA_PASSWORD=$env:DB_PWD" `
  -p 1433:1433 `
  mcr.microsoft.com/mssql/server:2022-latest

docker exec cp04-sqlserver /opt/mssql-tools18/bin/sqlcmd `
  -S localhost -U sa -P $env:DB_PWD -C `
  -Q "IF DB_ID('api') IS NULL CREATE DATABASE api"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

No profile `dev`, o Hibernate cria ou atualiza as tabelas. Se o caminho do `sqlcmd` não existir na imagem usada, tente `/opt/mssql-tools/bin/sqlcmd`.

Variáveis disponíveis:

| Variável | Desenvolvimento | Produção |
|---|---|---|
| `DB_SERVER_URL` | padrão `localhost` | obrigatória |
| `DB_SERVER_PORT` | padrão `1433` | padrão `1433` |
| `DB_SCHEMA` | padrão `api` | obrigatória; é o nome do banco |
| `DB_USER` | padrão `sa` | obrigatória |
| `DB_PWD` | obrigatória | obrigatória e armazenada como secret no Azure |
| `SPRING_PROFILES_ACTIVE` | `dev` | `prd` |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | padrão `http://localhost:*` | definir para as origens permitidas |

O profile `prd` usa conexão criptografada e valida o schema, sem criar tabelas. Execute [database/sqlserver-schema.sql](database/sqlserver-schema.sql) no Azure SQL antes de iniciar a aplicação.

## Imagem Docker

```powershell
docker build -t cp-microservice:local .
```

Para executar a imagem, o SQL Server informado nas variáveis precisa estar acessível a partir do contêiner:

```powershell
docker run --rm -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=prd `
  -e DB_SERVER_URL=<host-sql> `
  -e DB_SERVER_PORT=1433 `
  -e DB_SCHEMA=api `
  -e DB_USER=<usuario> `
  -e DB_PWD=<senha> `
  cp-microservice:local
```

## Continuous Delivery

O workflow [continuous_delivery.yml](.github/workflows/continuous_delivery.yml) roda em cada `push` na `main`:

1. executa os testes;
2. gera tags por número da execução e commit;
3. constrói a imagem;
4. publica no Docker Hub com tags imutáveis e `latest`.

Antes do primeiro `push`, configure no repositório GitHub:

- variável de Actions `DOCKER_USER`: seu usuário do Docker Hub;
- secret de Actions `DOCKER_TOKEN`: token do Docker Hub com permissão de escrita;
- repositório público no Docker Hub chamado `cp-microservice`.

Nenhuma senha de banco ou token deve ser versionado.

## Verificação rápida da API

```powershell
$baseUrl = "http://localhost:8080"
$body = @{
  emissor = "Tesouro Nacional"
  taxa = 12.5
  risco = "baixo"
  vencimento = "2030-01-01"
  quantidade = 10
} | ConvertTo-Json

$criada = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/api/v1/financas" `
  -ContentType "application/json" `
  -Body $body

Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/financas/$($criada.id)"
Invoke-RestMethod -Method Delete -Uri "$baseUrl/api/v1/financas/$($criada.id)"
```

## Encerrar o ambiente local

```powershell
docker stop cp04-sqlserver
Remove-Item Env:DB_PWD
```

O contêiner local usa `--rm`; portanto, seus dados são descartados ao pará-lo.
