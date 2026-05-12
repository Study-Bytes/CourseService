# CourseService CI/CD

This document describes the CourseService GitHub Actions pipeline and server deployment flow.

## Pipeline overview

The workflow file is located at:

```text
.github/workflows/course-service-ci-cd.yml
```

The pipeline has two jobs:

```text
ci      -> runs on pull requests and pushes to main
deploy  -> runs only after push/merge to main
```

## CI job

The CI job validates the service before merge.

It runs:

```bash
./mvnw -B clean test
./mvnw -B verify -Popenapi
git diff --exit-code docs/openapi/course-service-openapi.yaml
docker build -t studybytes/course-service:ci .
```

The OpenAPI check is intentional. If controllers or DTOs change and the committed local OpenAPI file is stale, CI must fail.

## CI PostgreSQL

The workflow starts a PostgreSQL service container for OpenAPI generation and application startup checks:

```text
POSTGRES_DB=course_service
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

The test profile still uses H2 where configured by the tests. The PostgreSQL service is mainly needed because the OpenAPI Maven profile starts the Spring Boot application.

## Deploy job

The deploy job runs only on:

```text
push to main
```

It does not run for pull requests.

Deployment flow:

```text
GitHub Actions
  -> SSH to VPS
  -> cd $VPS_DEPLOY_PATH
  -> git fetch origin main
  -> git checkout main
  -> git pull --ff-only origin main
  -> docker compose up -d --build
  -> curl http://localhost:8082/health
```

## Required GitHub Secrets

Add these secrets in GitHub repository settings or in the `production` environment:

```text
VPS_HOST
VPS_PORT
VPS_USER
VPS_SSH_KEY
VPS_DEPLOY_PATH
```

Example:

```text
VPS_HOST=203.0.113.10
VPS_PORT=22
VPS_USER=deploy
VPS_DEPLOY_PATH=/opt/studybytes/course-service
```

`VPS_SSH_KEY` must contain the private SSH key used by GitHub Actions to connect to the server.

## Required VPS setup

The VPS must already have:

```text
Docker
Docker Compose plugin
Git
CourseService repository cloned into $VPS_DEPLOY_PATH
.env file created in $VPS_DEPLOY_PATH
SSH access for VPS_USER
shared backend Docker network created
```

Example initial setup on VPS:

```bash
sudo mkdir -p /opt/studybytes
sudo chown -R deploy:deploy /opt/studybytes
cd /opt/studybytes
git clone git@github.com:Study-Bytes/CourseService.git course-service
cd course-service
cp .env.example .env
nano .env
```

Then fill real values in `.env`.

Create the shared backend network used by `docker-compose.yml`:

```bash
docker network create studybytes_backend_net
```

Do not create `course_db_net` manually. CourseService compose creates it as an internal private network for `course-service` and `course-postgres`.

If `course_db_net` already exists from an older setup, remove it once before deployment:

```bash
docker network rm course_db_net
```

## Secrets policy

Real secrets must not be stored in the repository.

Allowed in repository:

```text
.env.example
README.md
docs/**/*.md
```

Not allowed in repository:

```text
.env
.env.*
secrets/
*.pem
real database passwords
real internal API keys
real JWT secrets or private keys
```

For this project stage, deployment uses `.env` on the VPS and GitHub Actions Secrets for SSH access. Docker secrets are intentionally not used yet.

## Server `.env`

The server `.env` must contain CourseService runtime settings:

```env
COURSE_SERVICE_PORT=8082
COURSE_BACKEND_NETWORK=studybytes_backend_net
COURSE_DB_NETWORK=course_db_net
COURSE_SERVICE_DB_NAME=course_service
COURSE_SERVICE_DB_USERNAME=postgres
COURSE_SERVICE_DB_PASSWORD=change-me
COURSE_SERVICE_DB_URL=jdbc:postgresql://course-postgres:5432/course_service
COURSE_SERVICE_INTERNAL_API_KEY=change-me-internal-api-key
USER_SERVICE_JWT_ISSUER_URI=http://user-service:8081
USER_SERVICE_JWT_JWK_SET_URI=http://user-service:8081/api/v1/auth/.well-known/jwks.json
USER_SERVICE_JWT_AUDIENCE=study-platform
COURSE_SERVICE_JWT_ROLES_CLAIM=roles
COURSE_SERVICE_JWT_ROLE_PREFIX=ROLE_
SPRING_PROFILES_ACTIVE=prod
```

## Manual deployment command

On VPS:

```bash
cd /opt/studybytes/course-service
git pull --ff-only origin main
docker compose up -d --build
curl -f http://localhost:8082/health
```

## Rollback basics

Find previous commit:

```bash
git log --oneline -10
```

Rollback to a previous commit:

```bash
git checkout <commit_sha>
docker compose up -d --build
curl -f http://localhost:8082/health
```

Then return to main when fixed:

```bash
git checkout main
git pull --ff-only origin main
docker compose up -d --build
```

## Notes

The deploy job assumes the repository is already cloned on the VPS. It does not copy the full project over SSH.

If the health check fails, GitHub Actions marks the deployment as failed. Check server logs:

```bash
cd /opt/studybytes/course-service
docker compose logs -f course-service
```
