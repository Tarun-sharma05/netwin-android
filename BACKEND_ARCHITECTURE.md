# NetWin Backend Architecture Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Core Components](#core-components)
3. [Service Layer](#service-layer)
4. [Data Layer](#data-layer)
5. [Security Architecture](#security-architecture)
6. [Wallet System](#wallet-system)
7. [KYC System](#kyc-system)
8. [API Documentation](#api-documentation)
9. [Deployment Architecture](#deployment-architecture)

## System Overview

NetWin is a tournament management platform built with Spring Boot, MongoDB, PostgreSQL, and Kafka. The system is designed to handle tournament management, user registrations, transactions, and real-time notifications.

### Technology Stack
- **Backend Framework**: Spring Boot
- **Databases**: 
  - MongoDB (Document Store)
  - PostgreSQL (Relational DB)
- **Message Broker**: Apache Kafka
- **API Gateway**: Spring Cloud Gateway
- **Security**: Spring Security + JWT
- **Containerization**: Docker
- **Orchestration**: Kubernetes

## Core Components

```
NetWin Backend
├── API Gateway (Spring Cloud Gateway)
├── Service Layer
│   ├── Tournament Service
│   ├── User Service
│   ├── Registration Service
│   ├── Transaction Service
│   ├── Wallet Service
│   ├── KYC Service
│   └── Notification Service
├── Data Layer
│   ├── MongoDB (Document Store)
│   ├── PostgreSQL (Relational DB)
│   └── Kafka (Message Broker)
└── Security Layer
    └── Spring Security + JWT
```

## Service Layer

### 1. Tournament Service
- Tournament CRUD operations
- Tournament status management
- Tournament scheduling
- MongoDB for flexible tournament data structure

### 2. User Service
- User management
- Authentication/Authorization
- Profile management
- PostgreSQL for user data

### 3. Registration Service
- Tournament registration
- Registration status management
- MongoDB for registration data

### 4. Transaction Service
- Payment processing
- Transaction history
- PostgreSQL for transaction data

### 5. Wallet Service
- Balance management
- Transaction processing
- Wallet history
- Security layer

### 6. KYC Service
- Document verification
- Identity verification
- Address verification
- Risk assessment

### 7. Notification Service
- Real-time notifications
- Email notifications
- Kafka for event-driven notifications

## Data Layer

### MongoDB Collections

```json
Tournament {
    _id: ObjectId,
    name: String,
    description: String,
    startDate: Date,
    endDate: Date,
    status: String,
    maxParticipants: Number,
    currentParticipants: Number,
    rules: Object,
    prizes: Array
}

Registration {
    _id: ObjectId,
    tournamentId: ObjectId,
    userId: String,
    status: String,
    registrationDate: Date,
    paymentStatus: String
}
```

### PostgreSQL Tables

```sql
Users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)

Transactions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES Users(id),
    amount DECIMAL,
    type VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP,
    metadata JSONB
)

Wallets (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES Users(id),
    balance DECIMAL(19,4),
    currency VARCHAR(3),
    status VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_transaction_at TIMESTAMP
)

Wallet_Transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID REFERENCES Wallets(id),
    amount DECIMAL(19,4),
    type VARCHAR(50),
    status VARCHAR(50),
    reference_id VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP
)

KYC_Profiles (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES Users(id),
    status VARCHAR(20),
    level VARCHAR(20),
    verification_date TIMESTAMP,
    expiry_date TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)

KYC_Documents (
    id UUID PRIMARY KEY,
    kyc_profile_id UUID REFERENCES KYC_Profiles(id),
    document_type VARCHAR(50),
    document_number VARCHAR(100),
    issuing_country VARCHAR(2),
    issue_date DATE,
    expiry_date DATE,
    verification_status VARCHAR(20),
    created_at TIMESTAMP
)
```

## Security Architecture

### Authentication
- JWT-based authentication
- OAuth2 integration
- Multi-factor authentication for sensitive operations

### Authorization
- Role-based access control (RBAC)
- Resource-based permissions
- API security

### Data Security
- Data encryption at rest
- Data encryption in transit
- Secure key management
- Regular security audits

## Wallet System

### Features
- Balance management
- Transaction processing
- Wallet history
- Security measures
- Integration with payment gateways

### Security Measures
- Multi-factor authentication
- Transaction signing
- Rate limiting
- IP-based restrictions
- Suspicious activity monitoring

## KYC System

### KYC Levels
1. **Level 1 (Basic)**
   - Email verification
   - Phone verification
   - Basic identity check

2. **Level 2 (Standard)**
   - Document verification
   - Address verification
   - Enhanced identity check

3. **Level 3 (Enhanced)**
   - Source of funds verification
   - Enhanced due diligence
   - Risk assessment

### Integration Points
- Document verification providers
- Identity verification services
- Address verification services
- Compliance reporting systems

## API Documentation

### Base URL
```
https://api.netwin.com/v1
```

### Endpoints

#### Tournament Endpoints
```
GET    /tournaments
POST   /tournaments
GET    /tournaments/{id}
PUT    /tournaments/{id}
DELETE /tournaments/{id}
```

#### User Endpoints
```
POST   /users/register
POST   /users/login
GET    /users/profile
PUT    /users/profile
```

#### Wallet Endpoints
```
GET    /wallet/balance
GET    /wallet/transactions
POST   /wallet/deposit
POST   /wallet/withdraw
GET    /wallet/limits
```

#### KYC Endpoints
```
POST   /kyc/verify
GET    /kyc/status
GET    /kyc/documents
POST   /kyc/address
```

## Deployment Architecture

### Infrastructure
- Docker containers
- Kubernetes orchestration
- Load balancing
- Auto-scaling

### Monitoring
- Application monitoring
- Performance monitoring
- Security monitoring
- Compliance monitoring

### CI/CD Pipeline
- Automated testing
- Continuous integration
- Continuous deployment
- Quality gates

### Backup and Recovery
- Database backups
- Disaster recovery
- High availability
- Data retention policies

## Compliance and Regulations

### KYC/AML Compliance
- Customer due diligence
- Transaction monitoring
- Suspicious activity reporting
- Compliance reporting

### Data Protection
- GDPR compliance
- Data privacy
- Data retention
- User consent management

## Error Handling and Logging

### Error Handling
- Standardized error responses
- Error tracking
- Retry mechanisms
- Fallback procedures

### Logging
- Application logs
- Security logs
- Audit logs
- Performance logs

## Future Considerations

### Scalability
- Horizontal scaling
- Vertical scaling
- Database sharding
- Caching strategies

### Integration
- Payment gateway expansion
- KYC provider expansion
- Third-party service integration
- API versioning

### Security
- Advanced threat detection
- Fraud prevention
- Security automation
- Compliance automation 