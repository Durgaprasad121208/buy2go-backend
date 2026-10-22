# Buy2Go Backend Service

Buy2Go Enterprise E-Commerce Backend Service built with Spring Boot 3, Java 21, and MongoDB.

## Features
- **Authentication & Security**: Spring Security with JWT (JJWT)
- **Database**: Spring Data MongoDB
- **API Documentation**: SpringDoc OpenAPI / Swagger UI
- **Report Generation**: Apache POI (Excel export)
- **Architecture**: Modular controller-service-repository pattern

## Prerequisites
- Java 21+
- Apache Maven 3.8+
- MongoDB 6.0+

## Getting Started

### 1. Configuration
Check `src/main/resources/application.yml` for database and server properties.

### 2. Build & Run
```bash
mvn clean package
mvn spring-boot:run
```

The application runs by default on port `8188` with context path `/v1.0/buy2go-service`.

### 3. API Documentation
Once running, Swagger UI is available at:
`http://localhost:8188/v1.0/buy2go-service/swagger-ui/index.html`
