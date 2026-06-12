# E-Commerce Backend Web Application

A robust and modern Spring Boot e-commerce backend application featuring security configurations (JWT), user and order management, product catalogs, file uploads, and SMTP email notifications.

## Project Structure

```text
Ecommerce_zip_deploye/
├── Ecommerce/
│   └── demo/                 # Core Spring Boot application
│       ├── src/              # Source code and resource configurations
│       ├── Dockerfile        # Container configuration for production deployment
│       ├── pom.xml           # Project dependencies (Spring Boot, Spring Security, JPA, MySQL/PostgreSQL)
│       └── mvnw / mvnw.cmd   # Maven Wrapper files
├── render.yaml               # Infrastructure-as-code configuration for Render deployment
└── README.md                 # Project documentation
```

## Features

- **User Authentication**: Secure JWT-based authentication filter.
- **E-Commerce Operations**: APIS for handling products, orders, and user registration.
- **Email Notifications**: SMTP configurations integrated for user-triggered mail alerts.
- **Database Support**: Fallback to local MySQL with automatic migration, dynamically changing to PostgreSQL on Render production environments.
- **Docker Ready**: Pre-configured `Dockerfile` for easy containerization.

## Technology Stack

- **Framework**: Spring Boot
- **Security**: Spring Security, JWT (JSON Web Tokens)
- **Database**: MySQL (Local) / PostgreSQL (Production)
- **Build Tool**: Maven
- **Infrastructure**: Render, Docker

## Getting Started

### Local Setup

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd Ecommerce_zip_deploye
   ```

2. **Configure Database**:
   Ensure MySQL is running locally. Set up a database named `EcommerceWeb_side`. Update properties in [application.properties](file:///c:/Users/pankaj%20kumar%20saini/Desktop/Ecommerce_zip_deploye/Ecommerce/demo/src/main/resources/application.properties) if necessary.

3. **Run the Application**:
   Navigate to the Spring Boot root directory and start the application:
   ```bash
   cd Ecommerce/demo
   ./mvnw spring-boot:run
   ```
   The server will start on port `1234` by default.

### Deployment on Render

This project is configured for automated deployment via Render using the [render.yaml](file:///c:/Users/pankaj%20kumar%20saini/Desktop/Ecommerce_zip_deploye/render.yaml) configuration. It automatically provisions a PostgreSQL database and hosts the Docker environment.
