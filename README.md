# Wallet Service

## 📌 Overview

A Spring Boot microservice for managing digital wallets, supporting essential financial operations such as deposits, withdrawals, and transfers between users. Built with production-ready standards including full transaction traceability for auditing purposes.

**Development Time**: ~2 hours + ~1 hour for testing you can check github commit history for time spent on testing.

## 🚀 Features

- ✅ Create wallets for users
- ✅ Check current balance
- ✅ Check historical balance at any point in time
- ✅ Deposit funds
- ✅ Withdraw funds
- ✅ Transfer funds between wallets
- ✅ Complete transaction history
- ✅ API documentation with Swagger UI
- ✅ In-memory H2 database with console access
- ✅ Health monitoring and metrics

## 📋 Requirements

- Java 17 or higher
- Maven 3.6+

## 🛠️ Installation & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/wallet-service.git
   cd wallet-service
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The service will start on `http://localhost:8080`

## 🧪 Testing

Run all tests:
```bash
mvn test
```

Run integration tests:
```bash
mvn verify
```

## 📚 API Documentation

Once the application is running, you can access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **H2 Database Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:walletdb`
  - Username: `sa`
  - Password: (leave empty)

## 🔧 API Endpoints

### Wallet Operations

#### Create Wallet
```http
POST /api/wallets?ownerId={userId}
```

#### Get Current Balance
```http
GET /api/wallets/{ownerId}
```

#### Get Historical Balance
```http
GET /api/wallets/{ownerId}/historical?timestamp={ISO-8601-datetime}
```

#### Deposit Funds
```http
POST /api/wallets/{ownerId}/deposit?amount={value}&description={optional}
```

#### Withdraw Funds
```http
POST /api/wallets/{ownerId}/withdraw?amount={value}&description={optional}
```

#### Transfer Funds
```http
POST /api/wallets/transfer
Content-Type: application/json

{
  "sourceOwnerId": "user1",
  "targetOwnerId": "user2",
  "amount": 100.00,
  "description": "Payment for services"
}
```

#### Get Transaction History
```http
GET /api/wallets/{ownerId}/transactions
```

## 📊 Example Usage

1. Create a wallet:
   ```bash
   curl -X POST "http://localhost:8080/api/wallets?ownerId=john_doe"
   ```

2. Deposit funds:
   ```bash
   curl -X POST "http://localhost:8080/api/wallets/john_doe/deposit?amount=1000&description=Initial%20deposit"
   ```

3. Check balance:
   ```bash
   curl "http://localhost:8080/api/wallets/john_doe"
   ```

4. Transfer funds:
   ```bash
   curl -X POST "http://localhost:8080/api/wallets/transfer" \
     -H "Content-Type: application/json" \
     -d '{
       "sourceOwnerId": "john_doe",
       "targetOwnerId": "jane_doe",
       "amount": 250.00,
       "description": "Rent payment"
     }'
   ```

## 🏗️ Design Choices

### Architecture
- **Layered Architecture**: Controller → Service → Repository pattern for clear separation of concerns
- **DTOs**: Used for API communication to decouple internal entities from external representation
- **Spring Boot**: Provides production-ready features out of the box

### Data Persistence
- **JPA/Hibernate**: For ORM and database operations
- **H2 In-Memory Database**: For development and testing
- **Flyway**: Database migration management
- **Optimistic Locking**: Using @Version for concurrent access control

### Transaction Management
- **ACID Compliance**: All financial operations are wrapped in database transactions
- **Full Audit Trail**: Every operation creates a transaction record
- **Balance History**: Transaction records enable point-in-time balance queries

### Error Handling
- **Global Exception Handler**: Consistent error responses across the API
- **Custom Exceptions**: Business-specific exceptions for clarity
- **Validation**: Input validation using Bean Validation annotations

### Monitoring & Operations
- **Spring Actuator**: Health checks, metrics, and monitoring endpoints
- **Micrometer**: Metrics collection for operations monitoring
- **Swagger/OpenAPI**: Auto-generated API documentation

## ⚖️ Trade-offs & Compromises

Due to time constraints (2-hour implementation), some production features were omitted:

1. **Authentication/Authorization**: No security layer implemented
2. **Production Database**: Using H2 in-memory instead of PostgreSQL/MySQL
3. **Caching**: No distributed caching layer
4. **Rate Limiting**: No API rate limiting
5. **Pagination**: Transaction lists are not paginated
6. **Idempotency**: No idempotency keys for duplicate request prevention
7. **Currency Support**: Single currency assumption
8. **Distributed Transactions**: No saga pattern for distributed systems

## 🔍 Non-functional Requirements Implementation

### High Availability
- Health checks enable load balancer integration
- Stateless design allows horizontal scaling
- Graceful shutdown support

### Auditability
- Complete transaction log with timestamps
- Balance after each transaction recorded
- Point-in-time balance reconstruction capability

### Performance
- Indexed database queries
- Lazy loading for relationships
- Connection pooling

## 📈 Monitoring Endpoints

- Health Check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Application Info: http://localhost:8080/actuator/info

## 🔮 Future Enhancements

- Add OAuth2/JWT authentication
- Implement distributed caching with Redis
- Add support for multiple currencies
- Implement idempotency for all write operations
- Add event sourcing for complete audit trail
- Implement rate limiting
- Add async processing for notifications
- Database migration to PostgreSQL
- Add comprehensive logging with correlation IDs

## 📝 License

This is a technical assessment project.