# AURA PRIVÉ — Bespoke Private Event Management Platform

Aura Privé is a private concierge and event curation platform engineered for ultra-high-net-worth individuals (UHNWIs). The system orchestrates zero-exposure gatherings, private island summits, and unlisted estate buyouts with an emphasis on privacy, bespoke logistics, and security protocols.

The application is built on a Core Java backend, utilizing JDBC for relational database persistence, Apache Tomcat for web servlet execution, and a responsive luxury UI.

---

---

--

## 1. Application Preview

### Bespoke Client Portal

![Landing Page](docs/landing.png)

### Restricted Concierge Terminal & Database Engine

![Concierge Dashboard](docs/dashboard.png)

### Live Database Verification via JDBC

![Database Check](docs/database_check.png)

---

## 2. Technical Architecture & Core Concepts

The system implements enterprise Java design patterns across the application stack:

### Model-View-Controller (MVC) Architecture

- **Model:** Encapsulates business logic, domain entities, and data validation rules.
- **View:** Responsive JSPs (`index.jsp`, `dashboard.jsp`) styled with a minimalist obsidian-and-gold visual theme.
- **Controller:** `EventServlet.java` inspects HTTP request verbs (`GET`, `POST`), coordinates DAO database operations, and forwards data to views.

### Object-Oriented Design (OOP)

- **Interfaces (`SecuredEntity.java`):** Defines formal clearance contracts enforcing verification across high-profile entities.
- **Inheritance & Abstract Classes (`Event.java`):** A root abstraction encapsulating core event attributes (title, sanctuary venue, security level).
- **Polymorphism (`GalaEvent.java`):** Overrides `calculateBaseLogisticsFee()` to dynamically compute custom pricing based on VIP guest volume ($25,000 baseline + $1,500/guest).
- **Exception Handling (`ExclusiveAccessException.java`):** Custom domain exceptions to intercept security breaches and invalid booking payloads cleanly.

### Collections & Generics

- **Generic Registry (`VIPRegistry<T extends SecuredEntity>`):** Enforces compile-time type safety over Java Collections (`List<T>`). Restricts registry access strictly to entities fulfilling the `SecuredEntity` interface and provides thread-safe access to records.

### Multithreading & Synchronization

- **Asynchronous Service (`DiscreetNotificationService.java`):** Spawns dedicated background worker threads (`Runnable`) to simulate secure dispatch transmissions without blocking the primary HTTP request thread. Utilizes synchronized locks (`synchronized(lock)`) to maintain thread safety across concurrent requests.

### Data Access Layer (JDBC & DAO)

- **Singleton Connection Manager (`DatabaseConnection.java`):** Centralizes MariaDB connection pooling via JDBC drivers, preventing redundant connection overhead.
- **Data Access Object (`EventDAO.java`):** Encapsulates CRUD logic using parameterized `PreparedStatement` queries to prevent SQL injection vulnerabilities, with automatic fallback simulation if the local database is offline.

---

## 3. Directory Architecture

AuraPrive-EventManagement/
│
├── database/
│ └── javapro.sql # Relational MariaDB schema & seed records
│
├── docs/
│ ├── landing.png # Client portal preview
│ ├── dashboard.png # Concierge terminal preview
│ └── database_check.png # Terminal JDBC database verification view
│
├── lib/
│ └── mysql-connector-j-8.3.0.jar # MySQL/MariaDB JDBC connector library
│
├── src/
│ └── main/
│ ├── java/
│ │ └── com/auraprive/
│ │ ├── dao/
│ │ │ ├── DatabaseConnection.java # Thread-safe JDBC Singleton manager
│ │ │ └── EventDAO.java # Parameterized database CRUD operations
│ │ ├── exception/
│ │ │ └── ExclusiveAccessException.java # Custom security exception wrapper
│ │ ├── model/
│ │ │ ├── SecuredEntity.java # Access contract interface
│ │ │ ├── Event.java # Abstract base domain entity
│ │ │ ├── GalaEvent.java # Polymorphic implementation subclass
│ │ │ └── VIPRegistry.java # Type-safe bounded Generic collection
│ │ ├── service/
│ │ │ └── DiscreetNotificationService.java # Synchronized multithreaded worker
│ │ ├── servlet/
│ │ │ └── EventServlet.java # Web request dispatcher and controller
│ │ └── SystemVerification.java # Standalone evaluation runner & embedded server
│ │
│ └── webapp/
│ ├── css/
│ │ └── luxury-style.css # Obsidian & champagne-gold responsive UI
│ ├── js/
│ │ └── app.js # Client interactions
│ ├── index.jsp # Client-facing bespoke entry portal
│ ├── dashboard.jsp # Restricted concierge management interface
│ └── WEB-INF/
│ ├── web.xml # Deployment descriptor
│ └── classes/ # Compiled JVM bytecodes (.class files)
│
├── run-verification.bat # One-click compiler, rubric verifier & web launcher
└── README.md # Architectural specification & execution guide

---

## 4. Requirements, Setup & How to Run

### Prerequisites

- **Java Development Kit (JDK):** Version 11, 17, or 21+
- **Database (Optional):** MariaDB or MySQL Server on port `3306`. _(If MariaDB/MySQL is not installed on the device, the application automatically switches to **Offline In-Memory Mode** so all features and web pages still work)._

### One-Click Launch (Windows)

1. Extract or clone the `AuraPrive-EventManagement` folder.
2. Double-click **`run-verification.bat`**.
3. The script will compile the code, run the 5 backend rubric checks in the terminal, and automatically open **`http://localhost:8080`** in your browser.
4. Click **"Request Private Portfolio"** (or visit **`http://localhost:8080/events`**) to view the Concierge Dashboard and register new engagements.

```

```
 
