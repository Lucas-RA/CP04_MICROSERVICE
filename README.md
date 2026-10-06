# Checkpoint 2 — API REST com Spring Boot e SQL Server

API REST desenvolvida para o **Check Point 2 do 2º semestre de 2026** da disciplina Microservices and Web Engineering.

O projeto realiza operações CRUD em dois domínios:

- **Finanças**: gerenciamento de títulos financeiros;
- **Copa do Mundo**: gerenciamento de edições da competição.

Os dados são persistidos em um SQL Server local executado com Docker.

## Tecnologias

- Java 17
- Spring Boot 4.0.3
- Spring MVC
- Spring Data JPA e Hibernate
- SQL Server
- Maven Wrapper
- Docker
- Swagger/OpenAPI
- Lombok
- ModelMapper
- Bean Validation

## Repositório

- GitHub: [Lucas-RA/CP04_MICROSERVICE](https://github.com/Lucas-RA/CP04_MICROSERVICE)

## Arquitetura

```text
Cliente HTTP
    ↓
Controller
    ↓
DTO ↔ Mapper
    ↓
Service
    ↓
Repository
    ↓
Entity JPA ↔ SQL Server
```

A aplicação possui CRUD completo, IDs gerados pelo banco e separação entre contrato HTTP, regra de negócio e persistência.

## Pré-requisitos

- Java 17 ou superior;
- Docker instalado e em execução;
- portas `1433` e `8080` disponíveis;
- DBeaver opcional para inspecionar o banco.

> A senha `1q2w3e4R@` é usada somente no ambiente local deste trabalho. Não reutilize essa credencial em ambientes reais.

## 1. Iniciar o SQL Server

No PowerShell, execute:

```powershell
docker run -d `
  --name sqlserver `
  --rm `
  -e "MSSQL_SA_PASSWORD=1q2w3e4R@" `
  -e "ACCEPT_EULA=Y" `
  -p 1433:1433 `
  mcr.microsoft.com/mssql/server:latest
```

Confirme que o contêiner está em execução:

```powershell
docker ps
```

Acompanhe os logs até o SQL Server informar que está pronto para conexões:

```powershell
docker logs -f sqlserver
```

Use `Ctrl+C` para sair dos logs. O contêiner continuará em execução.

Referência do comando: [acnaweb/database](https://github.com/acnaweb/database#sql-server).

## 2. Criar o banco `api`

A imagem do SQL Server cria o banco `master`, mas não cria automaticamente o banco `api`. Crie-o uma vez antes de iniciar a aplicação.

Pelo próprio contêiner:

```powershell
docker exec sqlserver /opt/mssql-tools18/bin/sqlcmd `
  -S localhost -U sa -P "1q2w3e4R@" -C `
  -Q "IF DB_ID('api') IS NULL CREATE DATABASE api"
```

Se a imagem utilizada ainda possuir o caminho antigo do `sqlcmd`, troque `/opt/mssql-tools18/bin/sqlcmd` por `/opt/mssql-tools/bin/sqlcmd`.

Também é possível criar o banco no DBeaver. Conecte primeiro ao `master` e execute:

```sql
IF DB_ID('api') IS NULL
    CREATE DATABASE api;
```

## 3. Testar a conexão no DBeaver

Crie uma conexão do tipo **SQL Server** com os seguintes dados:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `1433` |
| Database inicial | `master` |
| Usuário | `sa` |
| Senha | `1q2w3e4R@` |

Depois de criar o banco, altere o campo Database para `api` ou abra uma nova conexão apontando para ele.

Se o driver solicitar opções SSL no ambiente local, habilite `Trust server certificate` ou desabilite a criptografia somente para esse teste local.

## 4. Profiles e conexão da aplicação

As propriedades comuns ficam em `src/main/resources/application.properties`.

### Profile `dev`

O arquivo `application-dev.properties` usa valores locais padrão:

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_SERVER_URL` | `localhost` | Host do SQL Server |
| `DB_SERVER_PORT` | `1433` | Porta do SQL Server |
| `DB_NAME` | `api` | Nome do banco |
| `DB_USER` | `sa` | Usuário do banco |
| `DB_PWD` | `1q2w3e4R@` | Senha local temporária |
| `DB_ENCRYPT` | `false` | Criptografia da conexão local |
| `DB_TRUST_SERVER_CERTIFICATE` | `true` | Confia no certificado local |

Nesse profile, `spring.jpa.hibernate.ddl-auto=update` permite ao Hibernate criar ou atualizar as tabelas.

### Profile `prd`

O arquivo `application-prd.properties` também usa SQL Server, mas não possui credenciais padrão e define `spring.jpa.hibernate.ddl-auto=none`.

Portanto, em `prd`:

- as variáveis `DB_SERVER_URL`, `DB_SERVER_PORT`, `DB_NAME`, `DB_USER` e `DB_PWD` são obrigatórias;
- o banco e as tabelas devem existir antes da inicialização;
- a aplicação não altera a estrutura do banco.

O nome `prd` é válido: o Spring identifica o profile pelo sufixo de `application-prd.properties`.

## 5. Executar a aplicação com o profile `dev`

Na raiz do projeto, execute no PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Como o profile possui valores locais padrão, não é necessário informar variáveis para o cenário descrito acima.

Para sobrescrever uma configuração, defina a variável antes de iniciar a aplicação. Exemplo:

```powershell
$env:DB_PWD = "outra-senha"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

A inicialização bem-sucedida apresenta `Started Application` no console.

## 6. Confirmar a criação das tabelas

No DBeaver, atualize o banco `api` e verifique as tabelas `financas` e `futebois`.

Também é possível consultar:

```sql
USE api;

SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE';
```

## Swagger/OpenAPI

Com a aplicação em execução:

- Swagger UI: [http://localhost:8080/](http://localhost:8080/)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Endpoints

### Finanças — `/financas`

| Método | Rota | Resposta esperada | Descrição |
|---|---|---|---|
| `POST` | `/financas` | `201 Created` | Cadastra uma finança |
| `GET` | `/financas` | `200 OK` | Lista todas as finanças |
| `GET` | `/financas/{id}` | `200 OK` ou `404 Not Found` | Busca uma finança |
| `PUT` | `/financas/{id}` | `200 OK` ou `404 Not Found` | Atualiza uma finança |
| `DELETE` | `/financas/{id}` | `204 No Content` ou `404 Not Found` | Remove uma finança |

Body de exemplo para `POST` e `PUT`:

```json
{
  "emissor": "Tesouro Nacional",
  "taxa": 12.5,
  "risco": "baixo",
  "vencimento": "2030-01-01",
  "quantidade": 10
}
```

### Copa do Mundo — `/copa`

| Método | Rota | Resposta esperada | Descrição |
|---|---|---|---|
| `POST` | `/copa` | `201 Created` | Cadastra uma edição da Copa |
| `GET` | `/copa` | `200 OK` | Lista todas as edições |
| `GET` | `/copa/{id}` | `200 OK` ou `404 Not Found` | Busca uma edição |
| `PUT` | `/copa/{id}` | `200 OK` ou `404 Not Found` | Atualiza uma edição |
| `DELETE` | `/copa/{id}` | `204 No Content` ou `404 Not Found` | Remove uma edição |

Body de exemplo para `POST` e `PUT`:

```json
{
  "ano": 2022,
  "capeao": "Argentina",
  "vice": "França",
  "sede": "Catar",
  "melhorJogador": "Lionel Messi"
}
```

## Teste rápido do CRUD no PowerShell

Com a API em execução, crie um registro:

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
  -Uri "http://localhost:8080/financas" `
  -ContentType "application/json" `
  -Body $body

$criada
```

Consulte, atualize e exclua o registro:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/financas/$($criada.id)"

$atualizacao = @{
  emissor = "Tesouro Nacional"
  taxa = 13.0
  risco = "baixo"
  vencimento = "2031-01-01"
  quantidade = 12
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Put `
  -Uri "http://localhost:8080/financas/$($criada.id)" `
  -ContentType "application/json" `
  -Body $atualizacao

Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/financas/$($criada.id)"
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/financas"
```

Essas chamadas comprovam inserção, consulta, alteração e exclusão por meio da API e do SQL Server.

## Build e testes

Compilar sem iniciar a aplicação:

```powershell
.\mvnw.cmd compile
```

Executar os testes com o SQL Server e o banco `api` disponíveis:

```powershell
.\mvnw.cmd test "-Dspring.profiles.active=dev"
```

## Encerrar o ambiente

Pare a aplicação com `Ctrl+C` e depois execute:

```powershell
docker stop sqlserver
```

Como o contêiner foi iniciado com `--rm`, ele será removido automaticamente. Os dados também serão perdidos; na próxima execução, será necessário recriar o banco `api`.

## Solução de problemas

### `docker` não é reconhecido

Instale e inicie o Docker Desktop e abra um novo PowerShell para atualizar o PATH.

### A aplicação não conecta ao SQL Server

- confirme que `sqlserver` aparece em `docker ps`;
- aguarde o SQL Server ficar pronto nos logs;
- confirme se a porta `1433` está livre;
- confirme que o banco `api` foi criado;
- verifique se a senha do contêiner é a mesma de `DB_PWD`.

### `Cannot open database "api" requested by the login`

O SQL Server está acessível, mas o banco ainda não existe. Execute a etapa **Criar o banco `api`**.

### A porta `8080` ou `1433` já está em uso

Identifique o processo ou contêiner que ocupa a porta e encerre-o, ou altere o mapeamento/configuração antes de iniciar o ambiente.
