# Bank Simulation System (Microservices Architecture)

## 📌 Project Overview
Developed during my internship at **Arab Bank**, this project is a distributed banking system built using **Spring Boot** microservices. The system simulates core banking operations including customers and account management, card services, and loan processing.

## 🏗️ Architecture & Features
- **Microservices Orchestration:** A decentralized system comprising services for Customers, Accounts, Cards, Loans, and Logging.
- **Inter-Service Communication:** Implemented seamless data integration using **Message Queues (MQ)** and **HTTP Clients**.
- **Data Persistence:** Utilizes a multi-database approach with **MongoDB** for flexible data structures and **H2/JPA** for relational persistence.
- **Fault Tolerance:** Implemented resilience patterns and scheduling to ensure high system availability.
- **Security:** Integrated **JWT (JSON Web Tokens)** for secure API authentication and authorization.

## 🛠️ Tech Stack
- **Backend:** Java (Spring Boot)
- **APIs:** RESTful APIs
- **Databases:** MongoDB, H2 
- **Tools:** Git, Postman, Swagger

## 📂 Service Directory
- `customerservice`: Manages user profile and core registration/login logic.
- `accountservice`: Manages multiple user accounts and core account logic.
- `cardservice`: Handles credit/debit card lifecycle and transactions.
- `loanservice`: Processes loan applications and repayment schedules.
- `loggerservice`: Centralized logging for system audit trails.
