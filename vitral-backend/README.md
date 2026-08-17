# Vitral Backend

API do projeto Vitral, desenvolvida com Java 21, Spring Boot, PostgreSQL e Flyway.

## Pré-requisitos

- Git
- Java 21
- Docker com Docker Compose v2

No Windows e no macOS, a opção mais simples é instalar o Docker Desktop. No Linux, instale o Docker Engine e o plugin Docker Compose.

Confirme as instalações:

```bash
java -version
docker --version
docker compose version
```

## Portas usadas

- Backend: `8080`
- PostgreSQL: `5432`
- Swagger: <http://localhost:8080/swagger-ui.html>

## Opção A — Subir tudo em containers (banco + backend)

O `docker-compose.yml` sobe o PostgreSQL **e** o backend (imagem construída pelo `Dockerfile`). O backend só inicia depois que o banco fica saudável (`healthcheck` + `depends_on`), e as imagens enviadas ficam em um volume persistente.

### Linux e macOS

```bash
export POSTGRES_PASSWORD="vitral_dev"
export JWT_SECRET="MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
docker compose up --build -d
docker compose ps
```

### Windows — PowerShell

```powershell
$env:POSTGRES_PASSWORD="vitral_dev"
$env:JWT_SECRET="MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
docker compose up --build -d
docker compose ps
```

Quando `vitral-backend` estiver `Up`, acesse <http://localhost:8080> e <http://localhost:8080/swagger-ui.html>. Para parar tudo: `docker compose down` (adicione `-v` para apagar banco e uploads).

## Opção B — Banco em Docker + backend local (Maven Wrapper)

Use esta opção para desenvolvimento com hot reload. Suba **apenas** o banco e rode o backend pelo Maven Wrapper.

### 1. Iniciar o PostgreSQL com Docker

### Windows — PowerShell

```powershell
cd C:\caminho\para\vitral-backend
$env:POSTGRES_PASSWORD="vitral_dev"
docker compose up -d db
docker compose ps
```

### Windows — Prompt de Comando

```bat
cd C:\caminho\para\vitral-backend
set POSTGRES_PASSWORD=vitral_dev
docker compose up -d db
docker compose ps
```

### Linux e macOS

```bash
cd /caminho/para/vitral-backend
export POSTGRES_PASSWORD="vitral_dev"
docker compose up -d db
docker compose ps
```

O banco será criado com estes valores padrão:

```text
Banco:   vitral_db
Usuário: vitral_user
Senha:   valor definido em POSTGRES_PASSWORD
```

### 2. Executar o backend

O segredo abaixo serve somente para desenvolvimento local. Em produção, utilize um segredo exclusivo e armazenado de forma segura.

> O `JWT_SECRET` precisa estar em **Base64 padrão** (o backend o decodifica com `Decoders.BASE64`) e ter no mínimo 32 bytes. Não use Base64 *URL-safe*: caracteres como `-` e `_` são inválidos e causam o erro `Illegal base64 character` na inicialização. Para gerar um válido: `openssl rand -base64 32`.

### Windows — PowerShell

Na mesma janela usada para iniciar o banco:

```powershell
$env:JWT_SECRET="MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
.\mvnw.cmd spring-boot:run
```

### Windows — Prompt de Comando

Na mesma janela usada para iniciar o banco:

```bat
set JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
mvnw.cmd spring-boot:run
```

### Linux e macOS

No mesmo terminal usado para iniciar o banco:

```bash
export JWT_SECRET="MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
./mvnw spring-boot:run
```

Quando a mensagem `Started VitralBackendApplication` aparecer, acesse:

- API: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui.html>

Para interromper o backend, pressione `Ctrl+C`.

## Executar os testes

### Windows

```powershell
.\mvnw.cmd test
```

### Linux e macOS

```bash
./mvnw test
```

## Gerar e executar o arquivo JAR

### Windows

```powershell
.\mvnw.cmd clean package
java -jar target\vitral-backend-0.0.1-SNAPSHOT.jar
```

### Linux e macOS

```bash
./mvnw clean package
java -jar target/vitral-backend-0.0.1-SNAPSHOT.jar
```

As variáveis `POSTGRES_PASSWORD` e `JWT_SECRET` precisam continuar definidas no terminal que executar o JAR.

## Comandos úteis do Docker

Execute os comandos na pasta do backend e mantenha `POSTGRES_PASSWORD` definida no terminal.

```bash
# Ver o estado do banco
docker compose ps

# Acompanhar os logs
docker compose logs -f db

# Parar e remover o contêiner, preservando os dados
docker compose down

# Reiniciar o banco
docker compose restart db
```

Para apagar também o volume e todos os dados locais do banco:

```bash
docker compose down -v
```

> Atenção: `docker compose down -v` remove permanentemente o banco local.

## Configuração opcional de e-mail

O envio de e-mail fica desabilitado por padrão. Para ativá-lo, defina estas variáveis antes de iniciar o backend:

```text
APP_MAIL_ENABLED=true
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=seu-email
SPRING_MAIL_PASSWORD=sua-senha-de-aplicativo
```

Também podem ser configuradas:

```text
APP_URL_BASE=http://localhost:8080
APP_URL_FRONTEND=http://localhost:5173
JWT_EXPIRATION=86400000
SERVER_PORT=8080
```

## Variáveis de ambiente adicionais

Todas as configurações sensíveis ou de ambiente são lidas por variáveis:

```text
# Origens permitidas no CORS (separadas por vírgula). Em produção, use o domínio real do frontend.
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173

# Diretório onde as imagens enviadas são gravadas.
# No Docker (Opção A) já aponta para o volume persistente /data/uploads/images.
APP_UPLOAD_IMAGE_DIR=uploads/images
```

Nunca envie senhas reais ou segredos JWT para o Git.

## Solução de problemas

### A porta 5432 já está em uso

Pare outro PostgreSQL local ou altere a porta publicada no `docker-compose.yml`.

### O Docker informa que `POSTGRES_PASSWORD` não foi definida

Defina a variável na mesma janela do terminal em que executar `docker compose`.

### O backend não conecta ao banco

Confira se o contêiner está ativo:

```bash
docker compose ps
docker compose logs db
```

Por padrão, o backend usa `jdbc:postgresql://localhost:5432/vitral_db` com o usuário `vitral_user`.
