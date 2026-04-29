# ⚽ Toxic-bet

## 📖 Visão geral

**Toxic-bet** é o backend de uma aplicação de bolão para a **Copa do Mundo de 2026**. O projeto permite cadastrar usuários autenticados por um servidor externo, organizar partidas, abrir e encerrar apostas, calcular odds dinamicamente e gerenciar bolões entre amigos.

A aplicação foi construída com **Java 21** e **Spring Boot 3**, usando programação **reativa com WebFlux**, persistência reativa com **R2DBC/PostgreSQL**, migrações com **Flyway** e segurança baseada em **JWT** validado via JWKs de um servidor de autenticação externo.

Além disso, o sistema publica eventos em **Kafka** para sincronização de usuários OAuth e disponibiliza stream reativa de partidas via **Server-Sent Events (SSE)**.

---

## 🛠️ Tecnologias

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.11-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=flat&logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-integrated-231F20?style=flat&logo=apachekafka&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?style=flat&logo=junit5&logoColor=white)

- **Java 21** — versão definida no `pom.xml`
- **Spring Boot 3.4.11** — base da aplicação
- **Spring WebFlux** — endpoints reativos e SSE
- **Spring Security + OAuth2 Resource Server** — proteção por JWT
- **Spring Data R2DBC** — acesso reativo ao PostgreSQL
- **Flyway** — versionamento e migração do banco
- **PostgreSQL 16** — banco principal
- **Apache Kafka** — envio do evento `sync-application`
- **Caffeine Cache** — cache configurado nos perfis atuais
- **Springdoc OpenAPI** — documentação e UI Swagger
- **JUnit 5 + Mockito + Reactor Test** — testes unitários e de carga leve

---

## ✨ Funcionalidades

### 👤 Usuários
- Verificação se um usuário já está cadastrado por e-mail
- Cadastro de usuário após autenticação prévia em servidor externo
- Consulta de usuário por ID ou e-mail
- Sincronização de usuários OAuth via Kafka quando necessário

### 🏟️ Partidas
- Cadastro de partidas por usuário com papel **ADMIN**
- Validação de horário futuro para partidas
- Validação para impedir conflito de horário entre equipes em janela de 3 horas
- Abertura manual de partidas para apostas
- Fechamento manual das apostas de uma partida
- Encerramento manual da partida com resultado final
- Atualização automática de partidas para `IN_PROGRESS` por agendamento

### 🎯 Apostas
- Registro de uma aposta por usuário por partida
- Bloqueio de apostas em partidas não abertas para aposta
- Processamento **sequencial por partida** para evitar concorrência e disputa de lock
- Cálculo dinâmico de **odds** e pontuação do usuário
- Recalculo de pontuação dos usuários quando a partida é encerrada

### 🧑‍🤝‍🧑 Bolões
- Criação de bolões
- Busca de bolão por código único
- Entrada de usuário em bolão compartilhado
- Listagem de usuários do bolão ordenados por pontuação

### 📡 Tempo real e integração
- Stream reativa de partidas via **SSE** no endpoint `/match`
- Integração com servidor de autenticação externo para validação de JWT por JWKs
- Publicação de evento Kafka no tópico `sync-application`

---

## 🧱 Arquitetura do projeto

A estrutura principal segue uma separação por camadas:

- `src/main/java/br/com/hahn/toxicbet/application/`
  - `controller/` — controladores REST
  - `service/` — regras de negócio
  - `mapper/` — mapeamento entre entidades e DTOs
- `src/main/java/br/com/hahn/toxicbet/domain/`
  - `model/` — entidades e objetos de domínio
  - `repository/` — repositórios reativos
  - `exception/` — exceções de negócio
- `src/main/java/br/com/hahn/toxicbet/infrastructure/`
  - `config/` — configurações técnicas
  - `security/` — segurança e autenticação JWT
  - `service/` — integrações técnicas
  - `scheduling/` — tarefas agendadas
- `src/main/resources/`
  - `application-local.yml`
  - `application-aws.yml`
  - `application-docker.yml`
  - `db/migration/`
  - `static/swagger.yml`

---

## ⚙️ Configuração de variáveis de ambiente

O projeto agora usa três perfis principais de execução:

- `local` — aplicação rodando pela IDE/terminal e PostgreSQL local
- `docker` — aplicação e PostgreSQL rodando em containers
- `aws` — aplicação rodando em container com dependências externas já existentes na AWS

Use o arquivo `.env.example` como base.

### 📋 Variáveis disponíveis

| Variável | Descrição |
|---|---|
| `POSTGRES_LOCAL_HOST` | Host do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_PORT` | Porta do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_USER` | Usuário do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_PASSWORD` | Senha do PostgreSQL do perfil `local` |
| `POSTGRES_LOCAL_DB` | Banco do perfil `local` |
| `POSTGRES_DOCKER_HOST` | Host do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_PORT` | Porta do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_USER` | Usuário do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_PASSWORD` | Senha do PostgreSQL do perfil `docker` |
| `POSTGRES_DOCKER_DB` | Banco do perfil `docker` |
| `POSTGRES_AWS_HOST` | Host do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_PORT` | Porta do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_USER` | Usuário do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_PASSWORD` | Senha do PostgreSQL do perfil `aws` |
| `POSTGRES_AWS_DB` | Banco do perfil `aws` |

### 📄 Exemplo

```dotenv
# Postgres Local
POSTGRES_LOCAL_HOST=localhost
POSTGRES_LOCAL_PORT=5435
POSTGRES_LOCAL_USER=postgres
POSTGRES_LOCAL_PASSWORD=postgres
POSTGRES_LOCAL_DB=toxic-bet-local

# Postgres Docker
POSTGRES_DOCKER_HOST=postgres
POSTGRES_DOCKER_PORT=5432
POSTGRES_DOCKER_USER=postgres
POSTGRES_DOCKER_PASSWORD=postgres
POSTGRES_DOCKER_DB=toxic-bet

# Postgres AWS
POSTGRES_AWS_HOST=aws-postgres-host
POSTGRES_AWS_PORT=5432
POSTGRES_AWS_USER=postgres
POSTGRES_AWS_PASSWORD=postgres
POSTGRES_AWS_DB=toxic-bet
```

> **Importante:** além do banco, a aplicação também depende de serviços externos para Kafka e autenticação.
>
> - **Kafka** em `localhost:9093` no perfil `local`, `kafka:9092` no perfil `docker` e endpoint configurado por variável no perfil `aws`
> - **Auth-Server** com JWKs em `http://localhost:2310/auth-server/public-key/jwks` no perfil `local`, `http://auth-server:2310/auth-server/public-key/jwks` no perfil `docker` e `https://auth.toxicbet.com.br/auth-server/public-key/jwks` por padrão no perfil `aws`

---

## 🚀 Como executar

### 📦 Execução com Docker Compose

Agora o repositório possui três arquivos dedicados:

- `compose.local.yaml` — sobe apenas o PostgreSQL local do perfil `local`
- `compose.docker.yaml` — sobe API + PostgreSQL para o perfil `docker`
- `compose.aws.yaml` — sobe apenas a API com perfil `aws`, assumindo infraestrutura externa já existente

> **Observação importante:** Kafka e Auth-Server não são mais gerenciados por este projeto via Docker Compose. No perfil `docker`, eles continuam sendo consumidos como serviços externos pela rede Docker compartilhada `SHARED_SERVICES_NETWORK`.

### 1. Criar o arquivo `.env`

```bash
cp .env.example .env
```

### 2. Criar a rede externa compartilhada (uma vez)

```bash
docker network create shared-services
```

> Se sua rede tiver outro nome, ajuste `SHARED_SERVICES_NETWORK` no `.env`.

### 3. Subir o compose desejado

```bash
docker compose -f compose.local.yaml up -d
docker compose -f compose.docker.yaml up -d --build
docker compose -f compose.aws.yaml up -d --build
```

### 4. Verificar containers ativos

```bash
docker compose -f compose.local.yaml ps
docker compose -f compose.docker.yaml ps
docker compose -f compose.aws.yaml ps
```

### 5. Parar infraestrutura

```bash
docker compose -f compose.local.yaml down
docker compose -f compose.docker.yaml down
docker compose -f compose.aws.yaml down
```

---

### 💻 Execução local da aplicação

### Pré-requisitos

- **Java 21**
- Docker (opcional, para PostgreSQL)
- **Kafka** acessível em `localhost:9093`
- **Auth-Server** acessível em `http://localhost:2310/auth-server/public-key/jwks`

### 1. Exportar variáveis no terminal

```bash
export POSTGRES_LOCAL_HOST="localhost"
export POSTGRES_LOCAL_PORT="5435"
export POSTGRES_LOCAL_USER="postgres"
export POSTGRES_LOCAL_PASSWORD="postgres"
export POSTGRES_LOCAL_DB="toxic-bet-local"
```

### 2. Iniciar a aplicação com o perfil `local`

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

A aplicação usará por padrão:

- **Porta da API:** `10000`
- **Banco:** PostgreSQL configurado por `POSTGRES_LOCAL_*`
- **Kafka:** `localhost:9093`
- **JWK Set URI:** `http://localhost:2310/auth-server/public-key/jwks`

---

### 🐳 Perfil `docker`

O arquivo `application-docker.yml` é usado automaticamente no container da API e espera serviços acessíveis por nome de rede:

- PostgreSQL: `postgres:5432`
- Kafka: `${KAFKA_BOOTSTRAP_SERVERS}`
- Auth-Server: `${AUTH_SERVER_JWK_SET_URI}`
- Porta da aplicação: `20000`

Ao subir via `docker compose -f compose.docker.yaml up -d --build`, a API inicia com `SPRING_PROFILES_ACTIVE=docker`.

---

### ☁️ Perfil `aws`

O arquivo `application-aws.yml` reaproveita a configuração que antes estava no perfil `docker`, mas agora dedicada ao cenário AWS.

- Porta da aplicação: `20000`
- JWK padrão: `https://auth.toxicbet.com.br/auth-server/public-key/jwks`
- Banco: configurado preferencialmente por `POSTGRES_AWS_*`, com compatibilidade para `POSTGRES_DOCKER_*`
- Kafka: configurado preferencialmente por `KAFKA_AWS_BOOTSTRAP_SERVERS`

Para subir a API nesse cenário:

```bash
docker compose -f compose.aws.yaml up -d --build
```

---

## 🔐 Segurança e autenticação

A API é protegida com **JWT Bearer Token**.

### Endpoints públicos

Os seguintes caminhos estão liberados pela configuração de segurança:

- `/actuator/**`
- `/public/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

### Endpoints protegidos

Todos os demais endpoints exigem token JWT válido emitido por um servidor compatível com o conjunto de chaves públicas informado em `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`.

### Regra administrativa

As operações de gestão de partidas exigem usuário com papel **ADMIN**.

---

## 🗄️ Banco de dados e migrações

As migrações ficam em `src/main/resources/db/migration/`.

### Migrações atuais

- `V1__create_initial_tables.sql`
- `V2__create_role_colum_users.sql`

### Estruturas principais criadas

- `users`
- `teams`
- `championship`
- `championship_teams`
- `match`
- `bet`
- `betting_pool`

### Regras de banco relevantes

- `users.email` é único
- `bet` possui restrição única por `(user_id, match_id)`
- a tabela `users` possui papel com valores `USER` ou `ADMIN`
- a tabela `match` armazena odds, totais de aposta e status da partida
- existe um **trigger** que impede inserção de aposta após o início da partida

---

## 📘 Documentação da API

O contrato OpenAPI fonte está em:

- `src/main/resources/static/swagger.yml`

Esse arquivo também é usado pelo plugin `openapi-generator-maven-plugin` para gerar interfaces e modelos OpenAPI no build.

### Endpoints documentados no contrato

- **Users**
  - `GET /users/existsByEmail/`
  - `POST /users`
  - `GET /users`
- **Match**
  - `POST /match`
  - `GET /match` *(SSE)*
  - `PATCH /match/end-bet`
  - `PATCH /match`
  - `PATCH /match/open`
- **Bet**
  - `POST /bet`
- **Betting Pool**
  - `POST /bettingPool`
  - `GET /bettingPool/{bettingPoolKey}`
  - `PATCH /bettingPool/{bettingPoolKey}`
  - `GET /bettingPoll/getUsers/{bettingPoolKey}`

### Acesso à documentação em execução

Quando a aplicação estiver rodando, a UI costuma ficar disponível em um destes caminhos padrão do Springdoc:

```text
http://localhost:10000/swagger-ui.html
http://localhost:10000/swagger-ui/index.html
```

E o JSON OpenAPI em:

```text
http://localhost:10000/v3/api-docs
```

Para o perfil `docker`, troque a porta para `20000`.

---

## ⏱️ Rotinas agendadas

O projeto possui uma rotina agendada em `ApplicationScheduler`:

- execução a cada minuto
- atualiza partidas com status `NOT_STARTED` para `IN_PROGRESS` quando o horário da partida já passou

---

## 🧪 Testes

O projeto possui testes em `src/test/java/br/com/hahn/toxicbet/application/service/` cobrindo:

- `UserService`
- `TeamService`
- `ChampionshipService`
- `OddsService`
- `BetProcessorServiceLoadTest`

### Executar testes

```bash
./mvnw test
```

### Validação realizada

Na análise deste repositório, a suíte foi executada com sucesso:

- **15 testes executados**
- **0 falhas**
- **0 erros**

---

## 🧩 Observações importantes

- O backend depende de um **Auth-Server externo** para validação dos tokens JWT.
- O backend publica evento Kafka no tópico **`sync-application`** para sincronização de usuários OAuth.
- Os arquivos `compose.local.yaml`, `compose.docker.yaml` e `compose.aws.yaml` cobrem os três cenários suportados.
- Kafka/Auth continuam externos a este repositório e não possuem containers próprios aqui.
- O cache atualmente configurado nos perfis disponíveis é **Caffeine**.

---

## 📁 Arquivos importantes

- `pom.xml` — dependências, plugins e build
- `compose.local.yaml` — PostgreSQL para o perfil local
- `compose.docker.yaml` — API + PostgreSQL para o perfil docker
- `compose.aws.yaml` — API para o perfil aws
- `.env.example` — variáveis de ambiente base
- `src/main/resources/application-local.yml` — perfil local
- `src/main/resources/application-aws.yml` — perfil aws
- `src/main/resources/application-docker.yml` — perfil docker
- `src/main/resources/db/migration/` — migrações Flyway
- `src/main/resources/static/swagger.yml` — contrato OpenAPI

---

## 📄 Licença

Não foi identificado um arquivo de licença neste repositório. Se desejar, é recomendável adicionar um `LICENSE` para explicitar o modelo de uso do projeto.

