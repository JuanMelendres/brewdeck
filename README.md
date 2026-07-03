# ☕ BrewDeck

BrewDeck is a personal coffee companion platform designed to help coffee enthusiasts track, optimize, and reproduce their brewing recipes.

It allows users to store coffee profiles, define brewing recipes, and log brewing sessions to continuously improve extraction results.

---

## 📖 Description

BrewDeck aims to solve a common problem among coffee enthusiasts:

> “How do I consistently reproduce the perfect cup of coffee?”

Instead of relying on notes scattered across apps or memory, BrewDeck centralizes all brewing knowledge into a structured, data-driven system.

Future vision includes a **physical companion device (e-paper based)** that syncs with the platform.

---

## 🚀 Features

- ☕ Coffee management (origin, process, roast, notes)
- 🧪 Brewing recipes (grind size, ratio, temperature, time)
- 📊 Brew session tracking (results, ratings, adjustments)
- ⭐ Favorite recipes
- 📚 Historical brewing data for optimization
- 🔍 Clean REST API design
- 🧱 Scalable architecture for future features

---

## 🛠 Tech Stack

### Backend
- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Flyway (database migrations)
- Maven

### Code Quality
- Spotless (code formatting)
- PMD (static analysis)
- OWASP Dependency Check (security scanning)

### DevOps (planned / in progress)
- Docker
- GitHub Actions (CI/CD)
- SonarCloud (code quality & coverage)
- Dependabot (dependency updates)

---

## 🏗 Architecture

```text
Client (Next.js - future)
        ↓
Spring Boot REST API
        ↓
PostgreSQL Database
        ↓
Flyway (schema versioning)
```

---

## Future extension
```text
E-paper device (Raspberry Pi Pico)
        ↓
Sync API
```

---

## 📁 Project Structure
```text
brewdeck/
├── brewdeck-api/        # Spring Boot backend
│   ├── src/main/java/com/brewdeck/
│   │   ├── coffee/
│   │   ├── recipe/
│   │   ├── session/
│   │   └── method/
│   ├── src/main/resources/
│   │   └── db/migration/
│   └── pom.xml
│
├── brewdeck-web/        # Next.js frontend (coming soon)
│
├── scripts/             # Git hooks scripts
├── docker-compose.yml   # Local PostgreSQL
└── README.md
```

---

## ⚙️ Local Setup

### 1. Clone repository
```bash
   git clone https://github.com/JuanMelendres/brewdeck.git
   cd brewdeck
 ```
### 2. Start PostgreSQL
```bash
   docker compose up -d
 ```
### 3. Run backend
```bash
   cd brewdeck-api
   ./mvnw spring-boot:run
```
### 4. Access API
http://localhost:8080

### 5. Swagger UI:
http://localhost:8080/swagger-ui/index.html

### 6. Postman Collection

A Postman collection is available for manual API testing.

Files:

```text
docs/postman/brewdeck.postman_collection.json
docs/postman/brewdeck.local.postman_environment.json
```

---

## 🔐 Environment Variables

The backend is configured through environment variables (with safe local
defaults). No secrets are committed.

```text
SPRING_PROFILES_ACTIVE   # active profile (default: local)
SERVER_PORT              # HTTP port (default: 8080)
DB_URL                   # JDBC URL (default: jdbc:postgresql://localhost:5432/brewdeck)
DB_USER                  # database user (default: brewdeck)
DB_PASSWORD              # database password (default: brewdeck)
CORS_ALLOWED_ORIGINS     # allowed CORS origins (default: http://localhost:3000)
```

CORS is enabled for `/api/**` for the configured origins, and a health probe
is exposed at `/actuator/health` for the frontend and CI.

---

## 📡 API Endpoints

> Collection `GET` endpoints are paginated and support `page`, `size` and `sort`
> query parameters (e.g. `?page=0&size=10&sort=id,asc`). They return a
> `PageResponse<T>` envelope; `GET` by id returns the DTO directly.

### Coffee  
```html
GET    /api/coffees
GET    /api/coffees/{id}
POST   /api/coffees
PUT    /api/coffees/{id}
DELETE /api/coffees/{id}
```

### Methods
```html
GET    /api/brew-methods
GET    /api/brew-methods/{id}
POST   /api/brew-methods
PUT    /api/brew-methods/{id}
DELETE /api/brew-methods/{id}
```

### Recipes
```html
GET    /api/recipes
GET    /api/recipes/{id}
GET    /api/recipes/favorites
GET    /api/recipes/coffee/{coffeeId}
GET    /api/recipes/method/{methodId}
POST   /api/recipes
PUT    /api/recipes/{id}
PATCH  /api/recipes/{id}/favorite
PATCH  /api/recipes/{id}/unfavorite
DELETE /api/recipes/{id}
```

### Brew Sessions
```html
GET    /api/brew-sessions
GET    /api/brew-sessions/{id}
GET    /api/brew-sessions/recipe/{recipeId}
POST   /api/brew-sessions
PUT    /api/brew-sessions/{id}
DELETE /api/brew-sessions/{id}
```

### Dashboard
```html
GET    /api/dashboard/summary
```

### System
```html
GET    /actuator/health
```

---

## 🧪 Quality Gates

The project enforces professional-grade quality standards:

### SonarQube
#### Quality Gate
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=JuanMelendres_brewdeck)](https://sonarcloud.io/summary/new_code?id=JuanMelendres_brewdeck)

#### Bugs
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=JuanMelendres_brewdeck&metric=bugs)](https://sonarcloud.io/summary/new_code?id=JuanMelendres_brewdeck)

#### Coverage
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=JuanMelendres_brewdeck&metric=coverage)](https://sonarcloud.io/summary/new_code?id=JuanMelendres_brewdeck)

#### Reliability Rating
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=JuanMelendres_brewdeck&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=JuanMelendres_brewdeck)


### Code Formatting
```bash
./mvnw spotless:apply
```

### Validation
```bash
./mvnw spotless:check
./mvnw test
./mvnw pmd:check
```

### Security Scan
```bash
./mvnw dependency-check:check
```

### Git Hooks
```text
Pre-commit → format + tests
Pre-push → full validation + security scan
```

---

## 🗺 Roadmap
### Phase 1
- [x] Coffee CRUD
- [x] PostgreSQL + Flyway
- [x] Code quality setup
- [x] Git hooks

### Phase 2
- [x] Brew Methods
- [x] Recipes
- [x] Brew Sessions
- [x] Favorites
- [x] Pagination, filters, dashboard summary
- [x] CORS, health probe, OpenAPI docs, structured logs

### Phase 3 (Current)
-  [ ] Next.js frontend
-  [ ] Mobile-first UI
-  [ ] API integration

### Phase 4
-  [ ] Authentication
-  [ ] User profiles
-  [ ] Cloud deployment

### Phase 5 (Vision 🚀)
-  [ ] Hardware integration (e-paper device)
-  [ ] Offline sync
-  [ ] Advanced analytics

---

## 💡 Vision

BrewDeck is not just a CRUD app.

It is evolving into:

- ☕ A data-driven brewing system
- 📱 A mobile-first coffee companion
- 📟 A physical smart coffee device

---

## 👨‍💻 Author

Juan Melendres

Backend Developer | Java & Spring Boot | Microservices

---

## ⭐ Support

If you like this project:

Star ⭐ the repository

Contribute ideas or improvements

---

