# AURA PRIVÉ — Bespoke Private Event Management Platform

Aura Privé is a private concierge and event curation platform. The system orchestrates zero-exposure royal galas, heritage palace buyouts, and closed-door diplomatic summits with an emphasis on privacy, bespoke logistics, and strict security protocols.

The application is built on a Core Java backend, utilizing JDBC for relational database persistence, Java Servlets, Filters, and JSPs for web execution, and a responsive OKLCH glassmorphism UI.

---

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

### Model-View-Controller (MVC) & Role-Based Security

- **Model:** Encapsulates business logic, domain entities (`Event`, `GalaEvent`, `SummitEvent`), and centralized server-side payload validation (`validatePayload()`).
- **View:** Responsive JSPs (`index.jsp`, `login.jsp`, `dashboard.jsp`) with XSS output sanitization (`Event.escapeHtml()`) and an OKLCH 24K gold and obsidian glassmorphism theme.
- **Controller & Security Filter:** `EventServlet.java`, `AuthServlet.java`, and `AuthFilter.java` (`@WebFilter("/events")`) enforce role-isolated HTTP sessions (`ADMIN`, `CLIENT`, `VENDOR`), handle dynamic account registration, and coordinate full CRUD database operations.

### Object-Oriented Design (OOP)

- **Interfaces (`SecuredEntity.java`):** Defines formal clearance contracts enforcing verification across high-profile entities.
- **Inheritance & Abstract Classes (`Event.java`):** A root abstraction encapsulating core event attributes, future/present date validation (`LocalDate`), and security clearance allow-lists.
- **Polymorphism (`GalaEvent.java`, `SummitEvent.java`):** Specialized subclasses overriding `calculateBaseLogisticsFee()` to dynamically compute custom pricing based on event architecture and VIP volume.
- **Exception Handling (`ExclusiveAccessException.java`):** Custom checked domain exception thrown and caught across `Event.validatePayload()`, `VIPRegistry.register()`, and `EventServlet`.

### Collections, Generics & Concurrency

- **Generic Registry (`VIPRegistry<T extends SecuredEntity>`):** Enforces compile-time type safety over Java Collections (`List<T>`) and validates entities upon registration.
- **Thread-Safe Simulator & Multithreading (`DiscreetNotificationService.java`):** Uses `Executors.newFixedThreadPool(3)` for non-blocking background dispatches with fine-grained `synchronized` state tracking, alongside `CopyOnWriteArrayList`, `ConcurrentHashMap`, and `AtomicInteger` ID sequences in `EventDAO`.

### Data Access Layer (JDBC & DAO)

- **Singleton Connection Manager (`DatabaseConnection.java`):** Thread-safe shared JDBC connection manager reading credentials from environment variables (`DB_PASSWORD`).
- **Data Access Object (`EventDAO.java`):** Encapsulates full CRUD logic (`INSERT`, `SELECT ... LEFT JOIN clients`, `UPDATE`, `DELETE`) using parameterized `PreparedStatement` queries across `exclusive_events`, `clients`, and `elite_vendors`.

---

## 3. Directory Architecture

```text
AuraPrive-EventManagement/
│
├── database/
│   └── javapro.sql                              # Relational MariaDB schema & Indian seed records
├── docs/
│   ├── account_create.png                      # Unified login & account creation gate preview
│   ├── client_view.png                         # Private client sanctuary & live telemetry preview
│   ├── dashboard.png                           # Concierge terminal preview
│   ├── database_check.png                      # Terminal JDBC database verification view
│   ├── landing.png                             # Client-facing bespoke landing portal preview
│   ├── terminal_run_test.png                   # Standalone CLI rubric verifier output
│   └── vendor_view.png                         # Syndicate vendor logistics dispatch preview
|
├── lib/
│   └── mysql-connector-j-8.3.0.jar              # MySQL/MariaDB JDBC connector library
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/auraprive/
│       │       ├── dao/
│       │       │   ├── DatabaseConnection.java  # Thread-safe JDBC Singleton connection manager
│       │       │   └── EventDAO.java            # Full CRUD & JOIN queries + concurrent fallback
│       │       ├── exception/
│       │       │   └── ExclusiveAccessException.java # Custom security exception wrapper
│       │       ├── model/
│       │       │   ├── SecuredEntity.java       # Access contract interface
│       │       │   ├── Event.java               # Abstract base entity & centralized validator
│       │       │   ├── GalaEvent.java           # Polymorphic implementation subclass 1
│       │       │   ├── SummitEvent.java         # Polymorphic implementation subclass 2
│       │       │   └── VIPRegistry.java         # Type-safe bounded Generic collection
│       │       ├── service/
│       │       │   └── DiscreetNotificationService.java # ExecutorService thread-pool worker
│       │       ├── servlet/
│       │       │   ├── AuthFilter.java          # @WebFilter protecting /events routes
│       │       │   ├── AuthServlet.java         # Multi-role login, signup & password controller
│       │       │   └── EventServlet.java        # Role-isolated CRUD controller
│       │       └── SystemVerification.java      # Standalone rubric runner & cookie-session server
│       │
│       └── webapp/
│           ├── css/
│           │   └── luxury-style.css             # OKLCH Glassmorphism responsive UI
│           ├── js/
│           │   └── app.js                       # Client interactions
│           ├── index.jsp                        # Client-facing bespoke entry portal
│           ├── login.jsp                        # Role-based login, sign-up & password change gate
│           ├── dashboard.jsp                    # Role-isolated concierge & client interface
│           └── WEB-INF/
│               └── web.xml                      # Deployment descriptor
│
├── run-verification.bat                         # One-click compiler, rubric verifier & web launcher
└── README.md                                    # Architectural specification & execution guide
```

---

## 4. Requirements, Setup & How to Run

### Prerequisites

- **Java Development Kit (JDK):** Version 11, 17, or 21+
- **Database (Optional):** MariaDB or MySQL Server on port `3306`. _(If `DB_PASSWORD` is not set or MariaDB/MySQL is not installed, the application automatically runs in **Thread-Safe Offline Simulator Mode** so all features work out of the box)._

### One-Click Launch (Windows)

1. Extract or clone the `AuraPrive-EventManagement` folder.
2. Double-click **`run-verification.bat`**.
3. The script compiles the source files into `out/`, runs the 5 backend rubric checks in the terminal, and opens **`http://localhost:8080/login.jsp`** in your browser.

### Default Evaluator Test Accounts (Passcode: `123456` for all)

- **Admin / Developer Console:** `admin@pro.abc` (Full CRUD & multi-table administration across Events, Clients, and Vendors)
- **Client Portal:** `client@pro.abc` (Isolated private bookings & engagement commissioning)
- **Vendor Portal:** `ven@pro.abc` (update or decline events created by clients)

### Manual Terminal Execution

```bash
mkdir out
javac -encoding UTF-8 -d out -cp "lib/mysql-connector-j-8.3.0.jar;src/main/java" src/main/java/com/auraprive/exception/*.java src/main/java/com/auraprive/model/*.java src/main/java/com/auraprive/service/*.java src/main/java/com/auraprive/dao/*.java src/main/java/com/auraprive/SystemVerification.java
java -cp "lib/mysql-connector-j-8.3.0.jar;out" com.auraprive.SystemVerification
```

### Real MariaDB / MySQL Database Setup

1. Import the schema and seed records:
   ```bash
   mysql -u root -p < database/javapro.sql
   ```
2. Set the `DB_PASSWORD` environment variable before launching `run-verification.bat`:
   ```powershell
   $env:DB_PASSWORD="your_mysql_root_password"
   .\run-verification.bat
   ```

### Deploying to Apache Tomcat 9

1. Compile the classes including the Servlets and Filter against Tomcat's `servlet-api.jar`:
   ```powershell
   mkdir src/main/webapp/WEB-INF/classes
   javac -encoding UTF-8 -d src/main/webapp/WEB-INF/classes -cp "lib/mysql-connector-j-8.3.0.jar;$env:CATALINA_HOME/lib/servlet-api.jar;src/main/java" src/main/java/com/auraprive/exception/*.java src/main/java/com/auraprive/model/*.java src/main/java/com/auraprive/service/*.java src/main/java/com/auraprive/dao/*.java src/main/java/com/auraprive/servlet/*.java
   ```
2. Copy `lib/mysql-connector-j-8.3.0.jar` into `src/main/webapp/WEB-INF/lib/` and deploy `src/main/webapp` to `$CATALINA_HOME/webapps/auraprive`.

---

## 5. Known Limitations

- **In-Memory Credential Store & Unsalted SHA-256:** User login accounts (`ACCOUNTS`) are held in a thread-safe `ConcurrentHashMap` seeded at startup and hashed with standard SHA-256 (`MessageDigest`) to avoid external third-party dependencies. Production deployments should persist account credentials in a dedicated database table using salted adaptive hashing (`BCrypt` or `Argon2`).
- **Single Shared JDBC Connection:** `DatabaseConnection` maintains a single thread-safe shared JDBC `Connection` instance rather than a multi-connection pool (`HikariCP`).
