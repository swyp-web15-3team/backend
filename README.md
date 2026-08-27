# Backend

## Requirements

- Java 21
- Docker
- Docker Compose v2
- Git

## Install

```bash
git clone https://github.com/swyp-web15-3team/backend.git
cd backend

cp .env.example .env
./gradlew installGitHooks
```

### Git Hooks

- `pre-commit`: Spotless 포맷 검사
- `pre-push`: 테스트, Checkstyle, Spotless 검사

## How to run

### Development

```bash
docker compose up -d db                     # run PostgreSQL using Docker
./gradlew bootRun                           # run server

curl http://localhost:8080/actuator/health  # healthcheck

# when developing is over, stop db server
docker compose stop db
```

### Docker

```bash
docker compose up -d --build

# check logs
docker compose ps
docker compose logs -f

# stop container. PostgreSQL volume is still available
docker compose down

# only build application image
docker build -t backend .
```

## Test

```bash
./gradlew check
```
