# NetWin Backend Integration Documentation

## Overview
This documentation provides comprehensive details for backend developers and admin panel developers to integrate with the NetWin Android application.

## Table of Contents
1. [Database Schema](#database-schema)
2. [API Endpoints](#api-endpoints)
3. [Real-time Updates](#real-time-updates)
4. [Admin Panel Requirements](#admin-panel-requirements)
5. [Security Requirements](#security-requirements)
6. [Integration Points](#integration-points)
7. [Testing Requirements](#testing-requirements)
8. [Deployment Requirements](#deployment-requirements)

## Database Schema

### Tournaments Collection
```json
{
  "tournaments": {
    "tournamentId": {
      "name": "String",
      "description": "String",
      "prizePool": "Number",
      "perKillPrize": "Number",
      "entryFee": "Number",
      "registeredTeams": "Number",
      "maxTeams": "Number",
      "isFeatured": "Boolean",
      "startDate": "Timestamp",
      "endDate": "Timestamp",
      "status": "String (UPCOMING/ONGOING/COMPLETED)",
      "imageUrl": "String",
      "createdAt": "Timestamp",
      "updatedAt": "Timestamp"
    }
  }
}
```

### Matches Collection
```json
{
  "matches": {
    "matchId": {
      "tournamentId": "String",
      "matchNumber": "Number",
      "status": "String (SCHEDULED/ONGOING/COMPLETED)",
      "results": [{
        "userId": "String",
        "teamId": "String",
        "kills": "Number",
        "position": "Number",
        "points": "Number"
      }],
      "startTime": "Timestamp",
      "endTime": "Timestamp",
      "createdAt": "Timestamp",
      "updatedAt": "Timestamp"
    }
  }
}
```

### Users Collection
```json
{
  "users": {
    "userId": {
      "name": "String",
      "email": "String",
      "phone": "String",
      "walletBalance": "Number",
      "kycStatus": "String (PENDING/APPROVED/REJECTED)",
      "createdAt": "Timestamp",
      "updatedAt": "Timestamp"
    }
  }
}
```

### Transactions Collection
```json
{
  "transactions": {
    "transactionId": {
      "userId": "String",
      "amount": "Number",
      "type": "String (DEPOSIT/WITHDRAWAL/TOURNAMENT_ENTRY/PRIZE_WIN)",
      "status": "String (PENDING/COMPLETED/FAILED)",
      "paymentMethod": "String",
      "createdAt": "Timestamp",
      "updatedAt": "Timestamp"
    }
  }
}
```

### KYC Documents Collection
```json
{
  "kycDocuments": {
    "documentId": {
      "userId": "String",
      "documentType": "String",
      "documentUrl": "String",
      "status": "String (PENDING/APPROVED/REJECTED)",
      "rejectionReason": "String",
      "createdAt": "Timestamp",
      "updatedAt": "Timestamp"
    }
  }
}
```

## API Endpoints

### Tournament Management
```
GET    /api/tournaments              - Get all tournaments
GET    /api/tournaments/featured     - Get featured tournaments
GET    /api/tournaments/{id}         - Get tournament details
POST   /api/tournaments              - Create new tournament
PUT    /api/tournaments/{id}         - Update tournament
DELETE /api/tournaments/{id}         - Delete tournament
POST   /api/tournaments/{id}/join    - Join tournament
POST   /api/tournaments/{id}/leave   - Leave tournament
```

### Match Management
```
GET    /api/tournaments/{id}/matches - Get tournament matches
GET    /api/matches/{id}             - Get match details
POST   /api/matches                  - Create new match
PUT    /api/matches/{id}             - Update match
DELETE /api/matches/{id}             - Delete match
POST   /api/matches/{id}/results     - Add match results
PUT    /api/matches/{id}/results     - Update match results
```

### User Management
```
GET    /api/users/{id}               - Get user details
PUT    /api/users/{id}               - Update user
GET    /api/users/{id}/wallet        - Get wallet balance
POST   /api/users/{id}/deposit       - Add money to wallet
POST   /api/users/{id}/withdraw      - Withdraw money
```

### Transaction Management
```
GET    /api/users/{id}/transactions  - Get user transactions
GET    /api/transactions/{id}        - Get transaction details
POST   /api/transactions             - Create transaction
PUT    /api/transactions/{id}        - Update transaction
```

### KYC Management
```
GET    /api/users/{id}/kyc           - Get KYC status
POST   /api/users/{id}/kyc           - Submit KYC
PUT    /api/users/{id}/kyc           - Update KYC
```

## Real-time Updates

The Android app uses Firebase for real-time updates. The backend should implement:

1. **Firebase Cloud Functions** for:
   - Tournament status updates
   - Match result updates
   - Transaction status changes
   - KYC status updates

2. **WebSocket Endpoints** for:
   - Live match updates
   - Tournament status changes
   - Wallet balance updates

## Admin Panel Requirements

### Tournament Management
- Create/Edit/Delete tournaments
- Set tournament status
- Manage tournament participants
- View tournament statistics

### Match Management
- Create/Edit/Delete matches
- Update match results
- View match statistics
- Manage match schedules

### User Management
- View user details
- Manage KYC verification
- Handle wallet transactions
- View user statistics

### Transaction Management
- View all transactions
- Approve/Reject transactions
- Generate transaction reports
- Handle payment gateway integration

### Analytics Dashboard
- Tournament participation statistics
- Revenue reports
- User growth metrics
- Match statistics

## Security Requirements

1. **Authentication**
   - JWT token-based authentication
   - Role-based access control (Admin/User)
   - Session management

2. **Data Security**
   - Encrypted data transmission
   - Secure storage of sensitive data
   - Regular security audits

3. **API Security**
   - Rate limiting
   - Input validation
   - CORS configuration
   - API key management

## Integration Points

### Payment Gateway
- Razorpay/Cashfree integration
- UPI integration
- Payment status webhooks

### Notification System
- Push notifications
- Email notifications
- SMS notifications

### File Storage
- Tournament poster images
- KYC documents
- User profile pictures

## Testing Requirements

1. **API Testing**
   - Unit tests
   - Integration tests
   - Load testing
   - Security testing

2. **WebSocket Testing**
   - Connection testing
   - Real-time update testing
   - Error handling testing

## Deployment Requirements

1. **Server Requirements**
   - Node.js/Python/Java backend
   - MongoDB/Firebase database
   - Redis for caching
   - Nginx for reverse proxy

2. **Monitoring**
   - Error logging
   - Performance monitoring
   - User activity tracking
   - Server health checks

## Contact
For any queries or clarifications, please contact:
- Android Developer: [Your Name]
- Email: [Your Email]
- Phone: [Your Phone Number] 