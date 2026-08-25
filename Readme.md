# Checkpoint 04 — API REST com Spring Boot e Docker

API REST desenvolvida para o **Checkpoint 04 de Microservices and Web Engineering**.
O projeto disponibiliza operações CRUD para dois domínios:

- **Finanças**: gerenciamento de títulos financeiros;
- **Copa do Mundo**: gerenciamento de edições da competição.

Além da API construída nas etapas anteriores, o Checkpoint 04 adiciona profiles de execução, configuração por variáveis de ambiente, empacotamento com Docker e publicação da imagem no Docker Hub.

## Funcionalidades

- CRUD completo de Finanças e Copa do Mundo;
- IDs gerados automaticamente pelo banco;
- separação em Controller, DTO/Mapper, Service, Repository e Entity;
- persistência com Spring Data JPA e MySQL;
- documentação interativa com Swagger/OpenAPI;
- profiles `default` e `prd`;
- configuração do banco por variáveis de ambiente;
- imagem Docker multi-stage;
- mesma imagem executável nos dois profiles;
- imagem versionada e publicada no Docker Hub.

## Tecnologias

- Java 17 como versão de compilação do projeto
- Spring Boot 4.0.3
- Spring MVC
- Spring Data JPA e Hibernate
- MySQL
- SpringDoc OpenAPI
- Maven
- Docker
- Lombok
- ModelMapper
- Bean Validation

> O Dockerfile atual utiliza imagens Eclipse Temurin 21 para build e execução. O Maven compila o projeto com compatibilidade Java 17.

## Repositórios

- GitHub: [Lucas-RA/CP03_MICROSERVICE](https://github.com/Lucas-RA/CP03_MICROSERVICE)
- Docker Hub: [270506lu/cp04_microservice](https://hub.docker.com/r/270506lu/cp04_microservice)
- Imagem utilizada na entrega: `270506lu/cp04_microservice:1.0.0`

## Arquitetura

O projeto separa o contrato HTTP da persistência:

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
Entity JPA ↔ MySQL
```

- **Controller**: recebe as requisições e devolve as respostas HTTP;
- **DTO**: define os dados de entrada e saída da API sem expor diretamente as entidades;
- **Mapper**: converte DTOs em entidades e entidades em DTOs com ModelMapper;
- **Service**: centraliza o fluxo de negócio e as operações CRUD;
- **Repository**: acessa o banco com Spring Data JPA;
- **Entity**: representa as tabelas persistidas no MySQL.

Os DTOs de criação não recebem `id`. Nas entidades, o identificador é gerado automaticamente:

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

## Profiles

### `default`

O profile padrão é destinado ao desenvolvimento local:

- aceita valores padrão para a conexão;
- inclui `createDatabaseIfNotExist=true` na URL JDBC;
- usa `spring.jpa.hibernate.ddl-auto=update`;
- permite ao Hibernate criar ou atualizar as tabelas;
- exibe SQL no console com `spring.jpa.show-sql=true`.

Configuração: `src/main/resources/application.properties`.

### `prd`

O profile de produção exige configuração explícita:

- todas as variáveis `DB_*` são obrigatórias;
- não contém `createDatabaseIfNotExist=true`;
- usa `spring.jpa.hibernate.ddl-auto=none`;
- não cria nem altera tabelas;
- desabilita a exibição de SQL com `spring.jpa.show-sql=false`.

O schema e as tabelas precisam existir antes da inicialização da aplicação.

Configuração: `src/main/resources/application-prd.properties`.

## Variáveis de ambiente

| Variável | Obrigatória | Descrição | Exemplo local |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Sim | Profile ativo | `default` ou `prd` |
| `DB_SERVER_URL` | Sim em `prd` | Endereço do MySQL | `host.docker.internal` |
| `DB_SERVER_PORT` | Sim em `prd` | Porta do MySQL | `3306` |
| `DB_SCHEMA` | Sim em `prd` | Nome do schema | `study` |
| `DB_USER` | Sim em `prd` | Usuário do banco | `root` |
| `DB_PWD` | Sim em `prd` | Senha do banco | `root_pwd` |

As credenciais deste README são somente exemplos locais. Não utilize credenciais reais no repositório.

Quando a API executa em um container, `localhost` aponta para o próprio container. No Docker Desktop, `host.docker.internal` permite acessar o MySQL publicado na máquina host.

## Pré-requisitos

Para executar a imagem publicada:

- Docker instalado e em execução;
- porta `8080` disponível para a API;
- porta `3306` disponível para o MySQL.

Não é necessário instalar Java ou Maven para executar a imagem do Docker Hub.

## Execução completa com Docker

Os exemplos abaixo usam Git Bash, Linux ou macOS. No Git Bash, execute os comandos a partir da raiz do projeto.

### 1. Iniciar o MySQL

```bash
docker run -d --name cp04-mysql --rm \
  -e MYSQL_ROOT_PASSWORD=root_pwd \
  -e MYSQL_ROOT_HOST=% \
  -p 3306:3306 \
  mysql:8.0
```

Confirme que o container iniciou:

```bash
docker ps
```

Acompanhe os logs até aparecer `ready for connections`:

```bash
docker logs -f cp04-mysql
```

Use `Ctrl+C` para sair dos logs. O MySQL continuará em execução.

### 2. Baixar a imagem publicada

```bash
docker pull 270506lu/cp04_microservice:1.0.0
```

Confirme a presença da imagem:

```bash
docker images 270506lu/cp04_microservice
```

### 3. Executar com o profile `default`

```bash
docker run --rm --name cp04-api-default \
  -p 8080:8080 \
  -e DB_SERVER_URL=host.docker.internal \
  -e DB_SERVER_PORT=3306 \
  -e DB_SCHEMA=study \
  -e DB_USER=root \
  -e DB_PWD=root_pwd \
  -e SPRING_PROFILES_ACTIVE=default \
  270506lu/cp04_microservice:1.0.0
```

O profile `default` deve criar o schema `study`, preparar as tabelas e apresentar `Started Application` nos logs.

Depois da validação, encerre a API com `Ctrl+C`. Como foi usado `--rm`, o container será removido automaticamente.

### 4. Validar que `prd` não cria o banco

Use propositalmente um schema inexistente:

```bash
docker run --rm --name cp04-api-prd-error \
  -p 8080:8080 \
  -e DB_SERVER_URL=host.docker.internal \
  -e DB_SERVER_PORT=3306 \
  -e DB_SCHEMA=study_prd_inexistente \
  -e DB_USER=root \
  -e DB_PWD=root_pwd \
  -e SPRING_PROFILES_ACTIVE=prd \
  270506lu/cp04_microservice:1.0.0
```

O resultado esperado é uma falha semelhante a:

```text
Unknown database 'study_prd_inexistente'
```

Esse comportamento confirma que o profile `prd` não cria o banco automaticamente.

### 5. Executar `prd` com estrutura existente

Use o schema `study`, criado anteriormente pelo profile `default`:

```bash
docker run --rm --name cp04-api-prd \
  -p 8080:8080 \
  -e DB_SERVER_URL=host.docker.internal \
  -e DB_SERVER_PORT=3306 \
  -e DB_SCHEMA=study \
  -e DB_USER=root \
  -e DB_PWD=root_pwd \
  -e SPRING_PROFILES_ACTIVE=prd \
  270506lu/cp04_microservice:1.0.0
```

Com o schema e as tabelas existentes, o log deve apresentar `Started Application`.

## Swagger/OpenAPI

Com a aplicação em execução, acesse:

- Swagger UI: [http://localhost:8080/](http://localhost:8080/)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

O Swagger permite consultar e testar todos os endpoints diretamente pelo navegador.

## Endpoints

### Finanças — `/financas`

| Método | Rota | Resposta esperada | Descrição |
|---|---|---|---|
| `POST` | `/financas` | `201 Created` | Cadastra uma finança |
| `GET` | `/financas` | `200 OK` | Lista todas as finanças |
| `GET` | `/financas/{id}` | `200 OK` ou `404 Not Found` | Busca uma finança |
| `PUT` | `/financas/{id}` | `200 OK` ou `404 Not Found` | Atualiza uma finança |
| `DELETE` | `/financas/{id}` | `204 No Content` ou `404 Not Found` | Remove uma finança |

Exemplo de body para `POST` e `PUT`:

```json
{
  "emissor": "Tesouro Nacional",
  "taxa": 12.5,
  "risco": "baixo",
  "vencimento": "2030-01-01",
  "quantidade": 10
}
```

O campo `id` não deve ser enviado no `POST`; ele é gerado automaticamente.

### Copa do Mundo — `/copa`

| Método | Rota | Resposta esperada | Descrição |
|---|---|---|---|
| `POST` | `/copa` | `201 Created` | Cadastra uma edição da Copa |
| `GET` | `/copa` | `200 OK` | Lista todas as edições |
| `GET` | `/copa/{id}` | `200 OK` ou `404 Not Found` | Busca uma edição |
| `PUT` | `/copa/{id}` | `200 OK` ou `404 Not Found` | Atualiza uma edição |
| `DELETE` | `/copa/{id}` | `204 No Content` ou `404 Not Found` | Remove uma edição |

Exemplo de body para `POST` e `PUT`:

```json
{
  "ano": 2022,
  "capeao": "Argentina",
  "vice": "França",
  "sede": "Catar",
  "melhorJogador": "Lionel Messi"
}
```

O campo `id` não deve ser enviado no `POST`; ele é gerado automaticamente.

## Build e publicação da imagem

Para construir a imagem com o mesmo nome e versão publicados:

```bash
docker build -t 270506lu/cp04_microservice:1.0.0 .
```

Para publicar a versão no Docker Hub depois do login:

```bash
docker push 270506lu/cp04_microservice:1.0.0
```

Opcionalmente, publique também a tag `latest`:

```bash
docker tag 270506lu/cp04_microservice:1.0.0 \
  270506lu/cp04_microservice:latest

docker push 270506lu/cp04_microservice:latest
```

## Execução local com Maven

Para trabalhar diretamente com o código-fonte, tenha Java, Maven e MySQL disponíveis. No Git Bash:

```bash
DB_SERVER_URL=localhost \
DB_SERVER_PORT=3306 \
DB_SCHEMA=study \
DB_USER=root \
DB_PWD=root_pwd \
SPRING_PROFILES_ACTIVE=default \
mvn spring-boot:run
```

## Estrutura do projeto

```text
src/
├── main/
│   ├── java/br/com/fiap/cp01_api01/
│   │   ├── controller/
│   │   │   ├── FinancasController.java
│   │   │   └── FutebolController.java
│   │   ├── dto/
│   │   │   ├── FinancaCreateRequest.java
│   │   │   ├── FinancaUpdateRequest.java
│   │   │   ├── FinancaResponse.java
│   │   │   ├── FinancaMapper.java
│   │   │   ├── FutebolCreateRequest.java
│   │   │   ├── FutebolUpdateRequest.java
│   │   │   ├── FutebolResponse.java
│   │   │   └── FutebolMapper.java
│   │   ├── model/
│   │   │   ├── Financa.java
│   │   │   └── Futebol.java
│   │   ├── repository/
│   │   │   ├── FinancaRepository.java
│   │   │   └── FutebolRepository.java
│   │   ├── service/
│   │   │   ├── FinancaService.java
│   │   │   └── FutebolService.java
│   │   └── Application.java
│   └── resources/
│       ├── application.properties
│       └── application-prd.properties
└── test/
    └── java/br/com/fiap/cp01_api01/ApplicationTests.java
```

Na raiz também estão:

- `Dockerfile`: build multi-stage e imagem de execução;
- `.dockerignore`: exclusões do contexto de build;
- `pom.xml`: dependências e configuração Maven.

## Comandos de diagnóstico

Listar containers em execução:

```bash
docker ps
```

Acompanhar logs:

```bash
docker logs -f cp04-api-default
```

Parar a API ou o MySQL:

```bash
docker stop cp04-api-default
docker stop cp04-mysql
```

## Solução de problemas

### A API não conecta ao MySQL

- confirme que `cp04-mysql` aparece em `docker ps`;
- aguarde o log `ready for connections` antes de iniciar a API;
- confirme o mapeamento `3306:3306`;
- use `host.docker.internal` quando a API estiver no Docker Desktop;
- use `localhost` quando a aplicação estiver executando diretamente pelo Maven.

### A porta 8080 ou 3306 já está em uso

Identifique os containers ativos:

```bash
docker ps
```

Pare o container conflitante ou altere a porta publicada.

### O profile `prd` não inicia

Confirme que:

- todas as variáveis `DB_*` foram fornecidas;
- o MySQL está acessível;
- o schema existe;
- as tabelas já foram criadas.

O profile `prd` não cria nem altera a estrutura do banco.

### `denied: requested access to the resource is denied`

Faça login na conta correta e confirme o nome completo da imagem:

```bash
docker login -u 270506lu
docker push 270506lu/cp04_microservice:1.0.0
```

