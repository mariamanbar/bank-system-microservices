# Bank Simulation System (Microservices Architecture)

**[Live demo](https://mariamanbar.github.io/bank-system-microservices/)**: try the staff and customer views in your browser. The demo runs on sample data, so no backend is needed.

## 📌 Project Overview

Developed during my internship at **Arab Bank**, this project is a distributed banking system built with **Spring Boot** microservices and an **HTML/JavaScript** frontend. It simulates core banking operations including customer registration and login, account management, card services, loan processing, and centralized audit logging.

## 🏗️ Architecture & Features

- **Microservices:** Independent services for Customers, Accounts, Cards, Loans, Logging, and Security.
- **Service Discovery:** A Eureka discovery server lets services register and find each other.
- **API Gateway:** A single entry point (port 8084) that the frontend talks to and that routes requests to the right service.
- **Inter-Service Communication:** RabbitMQ message queues and HTTP clients.
- **Data Persistence:** MongoDB Atlas for logs and H2/JPA for relational data.
- **Fault Tolerance:** Resilience4j circuit breakers, retry of failed actions, scheduled jobs, and idempotency keys.
- **Security:** JWT authentication with BCrypt password hashing.
- **Frontend:** Dependency-free HTML, CSS and JavaScript with separate staff and customer views for customers, accounts, cards, loans and the activity log.

## 🛠️ Tech Stack

- **Backend:** Java, Spring Boot, Spring Cloud (Gateway, Eureka)
- **Messaging:** RabbitMQ
- **Databases:** MongoDB, H2
- **Frontend:** HTML, CSS, vanilla JavaScript (no frameworks or build step)
- **Tools:** Git, Maven, Postman, Swagger

## 📂 Project Structure

| Folder | Port | Purpose |
| --- | --- | --- |
| `discoveryserver` | 8761 | Eureka service registry |
| `apigateway-1` | 8084 | API gateway / entry point for the frontend |
| `securityservice` | 8087 | Login, registration, JWT issuing |
| `customerservice` | 8080 | Customer profiles |
| `accountservice` | 8081 | Bank accounts, deposits, transfers |
| `cardservice` | 8082 | Credit/debit card lifecycle |
| `loanservice` | 8083 | Loan applications and installments |
| `loggerservice` | 8086 | Centralized audit logging (MongoDB) |
| `frontend` | — | Web UI |

## 🚀 Running Locally

**Requirements:** Java 17+, RabbitMQ running on `localhost:5672`, and a MongoDB Atlas cluster (or local MongoDB) for the logger service.

1. **Set up secrets.** Copy `secrets.example.properties` to `secrets.properties` in the project root and fill in your values. All services read it automatically, and it is git-ignored. You can also set the same names (`JWT_SECRET`, `MONGODB_URI`, ...) as environment variables instead.
2. **Start the services** (each from its own folder with `./mvnw spring-boot:run`, or from your IDE) in this order:
   `discoveryserver` → `securityservice` → `customerservice`, `accountservice`, `cardservice`, `loanservice`, `loggerservice` → `apigateway-1`
3. **Open the frontend:** serve the `frontend` folder at `http://127.0.0.1:5500` (for example with VS Code Live Server) and open `login.html`. The gateway only accepts browser requests from that address, so opening the file directly or using `localhost` will be blocked by CORS.

Sign in with an email ending in `@bank.jo.com` to get the staff view; any other email gets the customer view.

### Frontend structure

```
frontend/
├── *.html            one file per page (index.html is the customer directory / profile)
├── css/styles.css    all styling
├── fonts/            Public Sans (SIL Open Font License)
└── js/
    ├── core.js       API client, session, sidebar, dialogs, toasts, formatting
    ├── table.js      sortable, searchable, paginated tables
    └── pages/        one script per page
```

The gateway address is set once at the top of `js/core.js` (`API_BASE`).

### Demo mode

`js/demo.js` is a small in-browser stand-in for the backend with sample data, using the same API paths and responses as the real gateway. It turns on automatically on GitHub Pages; locally, open any page with `?demo` (for example `frontend/login.html?demo`, which also works by double-clicking the file). Use `?demo=off` or the **Exit demo** button to go back to the real backend. Changes to the sample data last until the browser tab is closed.

The live demo is published by `.github/workflows/pages.yml` whenever the `frontend` folder changes.

The H2 console for each relational service is available at `http://localhost:<port>/h2-console`.

## ✅ Automated Build

Every push runs a GitHub Actions workflow (`.github/workflows/build.yml`) that compiles and packages all eight services with Java 17 and checks that every secret the services need is listed in `secrets.example.properties`. Results are in the repository's **Actions** tab.
