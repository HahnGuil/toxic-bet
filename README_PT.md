# Toxic Bet API

## Versao em Ingles

Para a versao principal em ingles deste README, acesse [`README.md`](./README.md).

---

## Visao Geral

**Toxic Bet API** e o backend principal de uma aplicacao de bolao para a Copa do Mundo de 2026. Ele gerencia usuarios da aplicacao, campeonatos, times, partidas, apostas, odds, boloes e calculo de pontuacao.

O servico foi construido com **Java 21** e **Spring Boot 3.4.11**, usando uma stack reativa com **WebFlux**, **R2DBC/PostgreSQL**, **Flyway**, validacao de JWT por JWKs de um Auth-Server externo e eventos Kafka para sincronizacao de usuarios OAuth.

---

## Tecnologias

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.11-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=flat&logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-integrated-231F20?style=flat&logo=apachekafka&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?style=flat&logo=junit5&logoColor=white)

- **Java 21** conforme definido no `pom.xml`.
- **Spring Boot 3.4.11** como base da aplicacao.
- **Spring WebFlux** para endpoints REST reativos e SSE.
- **Spring Security + OAuth2 Resource Server** para protecao com JWT bearer.
- **Spring Data R2DBC** para acesso reativo ao PostgreSQL.
- **Flyway** para migracoes de banco.
- **Apache Kafka** para eventos `sync-application`.
- **Caffeine Cache** para caches configurados.
- **Springdoc OpenAPI** para documentacao Swagger/OpenAPI.
- **JUnit 5 + Mockito + Reactor Test** para testes.

---

## Funcionalidades

### Usuarios
- Verificacao se um usuario ja existe por e-mail.
- Cadastro de usuario da aplicacao apos autenticacao no Auth-Server externo.
- Consulta de usuarios por ID ou e-mail.
- Publicacao de eventos Kafka para sincronizacao de usuarios OAuth quando necessario.

### Partidas
- Cadastro de partidas com validacao de papel `ADMIN`.
- Validacao de datas futuras.
- Bloqueio de conflito de agenda entre times em uma janela de 3 horas.
- Abertura de partidas para apostas.
- Fechamento das apostas de uma partida.
- Encerramento de partidas com placar final.
- Atualizacao automatica de partidas para `IN_PROGRESS`.
- Stream de listas de partidas por Server-Sent Events.

### Apostas
- Registro de uma aposta por usuario por partida.
- Bloqueio de apostas em partidas que nao estao abertas.
- Processamento sequencial por partida para reduzir disputa de lock.
- Calculo dinamico de odds e pontos do usuario.
- Recalculo de pontuacao quando partidas sao encerradas.

### Boloes
- Criacao de boloes.
- Busca de bolao por codigo unico.
- Entrada em bolao compartilhado.
- Listagem de usuarios do bolao ordenados por pontuacao.

### Integracoes
- Validacao de JWT usando JWK set do Auth-Server.
- Publicacao de eventos Kafka no topico `sync-application`.
- Documentacao OpenAPI a partir de `src/main/resources/static/swagger.yml`.

---

## Arquitetura

O projeto segue uma estrutura por camadas:

- `src/main/java/br/com/hahn/toxicbet/application/`
  - `controller/` - controladores REST.
  - `service/` - regras de negocio.
  - `mapper/` - mapeamento entre entidades e DTOs.
- `src/main/java/br/com/hahn/toxicbet/domain/`
  - `model/` - entidades e objetos de dominio.
  - `repository/` - repositorios reativos.
  - `exception/` - excecoes de negocio.
- `src/main/java/br/com/hahn/toxicbet/infrastructure/`
  - `config/` - configuracoes tecnicas.
  - `security/` - seguranca JWT.
  - `service/` - integracoes tecnicas.
  - `scheduling/` - tarefas agendadas.
- `src/main/resources/`
  - `application-local.yml`
  - `application-docker.yml`
  - `application-aws.yml`
  - `db/migration/`
  - `static/swagger.yml`

---

## Referencias da API

### URLs de Execucao

- API local: [`http://localhost:10000`](http://localhost:10000)
- API Docker: [`http://localhost:20000`](http://localhost:20000)
- API de producao: [`https://api.toxicbet.com.br`](https://api.toxicbet.com.br)

### OpenAPI e Swagger

- Swagger UI local: [`http://localhost:10000/swagger-ui.html`](http://localhost:10000/swagger-ui.html)
- Swagger UI local alternativo: [`http://localhost:10000/swagger-ui/index.html`](http://localhost:10000/swagger-ui/index.html)
- Swagger UI Docker: [`http://localhost:20000/swagger-ui.html`](http://localhost:20000/swagger-ui.html)
- OpenAPI JSON local: [`http://localhost:10000/v3/api-docs`](http://localhost:10000/v3/api-docs)
- OpenAPI JSON Docker: [`http://localhost:20000/v3/api-docs`](http://localhost:20000/v3/api-docs)
- Fonte OpenAPI estatico: [`src/main/resources/static/swagger.yml`](src/main/resources/static/swagger.yml)

### APIs Relacionadas

- Auth-Server Docker: [`http://localhost:2300/auth-server`](http://localhost:2300/auth-server)
- Auth-Server local: [`http://localhost:2310/auth-server`](http://localhost:2310/auth-server)
- Auth-Server producao: [`https://auth.toxicbet.com.br/auth-server`](https://auth.toxicbet.com.br/auth-server)
- JWK set do Auth-Server: [`https://auth.toxicbet.com.br/auth-server/public-key/jwks`](https://auth.toxicbet.com.br/auth-server/public-key/jwks)
- Frontend local: [`http://localhost:4200`](http://localhost:4200)

### Principais Endpoints Documentados

- **Users**
  - `GET /users/existsByEmail/`
  - `POST /users`
  - `GET /users`
- **Match**
  - `POST /match`
  - `GET /match` (SSE)
  - `GET /match/find-open` (SSE)
  - `GET /match/in-progress` (SSE)
  - `PATCH /match/end-bet`
  - `PATCH /match`
  - `PATCH /match/open`
- **Bet**
  - `POST /bet`
  - `GET /bet/results`
  - `GET /bet/results/open`
- **Betting Pool**
  - `POST /bettingPool`
  - `GET /bettingPool/{bettingPoolKey}`
  - `PATCH /bettingPool/{bettingPoolKey}`
  - `GET /bettingPool/getUsers`

---

## Variaveis de Ambiente

O servico suporta tres perfis de execucao:

- `local` - aplicacao roda no host e usa PostgreSQL local.
- `docker` - aplicacao e PostgreSQL rodam em containers.
- `aws` - aplicacao roda em container e consome dependencias externas na AWS.

Use `.env.example` como base.

| Variavel | Descricao |
|---|---|
| `POSTGRES_LOCAL_HOST` | Host do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_PORT` | Porta do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_USER` | Usuario do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_PASSWORD` | Senha do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_DB` | Banco do PostgreSQL do perfil `local` |
| `POSTGRES_DOCKER_HOST` | Host do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_PORT` | Porta do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_USER` | Usuario do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_PASSWORD` | Senha do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_DB` | Banco do PostgreSQL do perfil `docker` |
| `POSTGRES_AWS_HOST` | Host do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_PORT` | Porta do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_USER` | Usuario do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_PASSWORD` | Senha do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_DB` | Banco do PostgreSQL do perfil `aws` |
| `KAFKA_BOOTSTRAP_SERVERS` | Bootstrap servers do Kafka para Docker |
| `KAFKA_AWS_BOOTSTRAP_SERVERS` | Bootstrap servers do Kafka para AWS |
| `AUTH_SERVER_JWK_SET_URI` | Endpoint JWK do Auth-Server usado para validar JWT |
| `AUTH_SERVER_BASE_URL` | URL base do Auth-Server usada em integracoes |
| `SHARED_SERVICES_NETWORK` | Rede Docker compartilhada entre servicos |

Exemplo:

```dotenv
POSTGRES_DOCKER_HOST=postgres
POSTGRES_DOCKER_PORT=5432
POSTGRES_DOCKER_USER=postgres
POSTGRES_DOCKER_PASSWORD=postgres
POSTGRES_DOCKER_DB=toxic-bet
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
AUTH_SERVER_JWK_SET_URI=http://ms-auth-server:2300/auth-server/public-key/jwks
AUTH_SERVER_BASE_URL=http://ms-auth-server:2300
SHARED_SERVICES_NETWORK=shared-services
```

---

## Docker

O repositorio possui Docker Compose dedicado por perfil:

- `compose.local.yaml` - sobe apenas PostgreSQL para apoio ao perfil local.
- `compose.docker.yaml` - sobe API + PostgreSQL para o perfil `docker`.
- `compose.aws.yaml` - sobe apenas a API com perfil `aws`, assumindo infraestrutura externa.

Kafka e Auth-Server nao sao gerenciados por este repositorio. No perfil `docker`, eles devem estar acessiveis pela rede Docker compartilhada.

### 1. Criar `.env`

```bash
cp .env.example .env
```

### 2. Criar a rede compartilhada

```bash
docker network create shared-services
```

Se sua rede tiver outro nome, atualize `SHARED_SERVICES_NETWORK` no `.env`.

### 3. Subir o perfil desejado

```bash
docker compose -f compose.local.yaml up -d
docker compose -f compose.docker.yaml up -d --build
docker compose -f compose.aws.yaml up -d --build
```

### 4. Verificar containers

```bash
docker compose -f compose.local.yaml ps
docker compose -f compose.docker.yaml ps
docker compose -f compose.aws.yaml ps
```

### 5. Parar servicos

```bash
docker compose -f compose.local.yaml down
docker compose -f compose.docker.yaml down
docker compose -f compose.aws.yaml down
```

---

## Rodando Localmente

### Pre-requisitos

- Java 21
- Docker, opcional para PostgreSQL
- Kafka acessivel pelo `KAFKA_BOOTSTRAP_SERVERS` configurado
- Auth-Server acessivel pelo `AUTH_SERVER_JWK_SET_URI` configurado

### 1. Subir PostgreSQL local, se necessario

```bash
docker compose -f compose.local.yaml up -d
```

### 2. Exportar variaveis

```bash
export POSTGRES_LOCAL_HOST="localhost"
export POSTGRES_LOCAL_PORT="5435"
export POSTGRES_LOCAL_USER="postgres"
export POSTGRES_LOCAL_PASSWORD="postgres"
export POSTGRES_LOCAL_DB="toxic-bet-local"
export AUTH_SERVER_JWK_SET_URI="http://localhost:2310/auth-server/public-key/jwks"
export AUTH_SERVER_BASE_URL="http://localhost:2310"
```

### 3. Rodar a aplicacao

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O perfil local expoe a API na porta `10000`.

---

## Seguranca

A API e protegida com JWT bearer tokens.

### Caminhos Publicos

- `/actuator/**`
- `/public/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

### Caminhos Protegidos

Todos os demais endpoints exigem um JWT valido emitido por um servidor compativel com o `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` configurado.

Operacoes de gerenciamento de partidas exigem papel `ADMIN`.

---

## Banco de Dados e Migracoes

As migracoes ficam em `src/main/resources/db/migration/`.

Migracoes atuais:

- `V1__create_initial_tables.sql`
- `V2__create_role_colum_users.sql`

Estruturas principais:

- `users`
- `teams`
- `championship`
- `championship_teams`
- `match`
- `bet`
- `betting_pool`

Regras relevantes:

- `users.email` e unico.
- `bet` possui restricao unica para `(user_id, match_id)`.
- `users.role` suporta `USER` e `ADMIN`.
- `match` armazena odds, totais de aposta e status da partida.
- Um trigger de banco impede insercao de aposta apos o inicio da partida.

---

## Rotinas Agendadas

`ApplicationScheduler` abre todas as partidas `NOT_STARTED` do dia a meia-noite e atualiza partidas abertas para `IN_PROGRESS` quando o horario de inicio ja passou.

---

## Testes

Execute a suite com:

```bash
./mvnw test
```

A suite atual cobre:

- `UserService`
- `TeamService`
- `ChampionshipService`
- `OddsService`
- `BetProcessorServiceLoadTest`

---

## Arquivos Importantes

- `pom.xml` - dependencias, plugins e configuracoes de build.
- `compose.local.yaml` - PostgreSQL para o perfil local.
- `compose.docker.yaml` - API + PostgreSQL para o perfil Docker.
- `compose.aws.yaml` - API para o perfil AWS.
- `.env.example` - variaveis de ambiente base.
- `src/main/resources/application-local.yml` - perfil local.
- `src/main/resources/application-docker.yml` - perfil Docker.
- `src/main/resources/application-aws.yml` - perfil AWS.
- `src/main/resources/db/migration/` - migracoes Flyway.
- `src/main/resources/static/swagger.yml` - contrato OpenAPI.

---

## Licenca

Nao foi identificado um arquivo de licenca neste repositorio.
