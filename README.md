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
   git clone https://github.com/YOUR_USERNAME/brewdeck.git
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

---

## 📡 API Endpoints
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

---

## 🧪 Quality Gates

The project enforces professional-grade quality standards:

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

### Phase 2 (Current)
- [x] Brew Methods
- [x] Recipes
- [x] Brew Sessions
-  [ ] Favorites

### Phase 3
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

