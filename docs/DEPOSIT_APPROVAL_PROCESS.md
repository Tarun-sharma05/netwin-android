# Deposit Approval Process - Complete Guide

**For:** NetWin Admin Team  
**Updated:** 11 Oct 2025

---

## Overview

This document explains how to approve pending deposits submitted by users through the Manual UPI system.

---

## 1. How Deposits Work

### User Side:
1. User goes to **Wallet → Add Cash**
2. Sees NetWin UPI ID: `sakshipuria@okaxis`
3. Makes payment via their UPI app (Google Pay, PhonePe, etc.)
4. Takes screenshot of successful payment
5. Enters UPI Transaction ID (12 digits)
6. Uploads screenshot
7. Submits deposit request

### What Happens:
- Document created in `pending_deposits` collection
- Screenshot uploaded to Firebase Storage
- Status set to **"PENDING"**
- User sees deposit in "Pending Deposits" section

---

## 2. Approval Methods

### Method 1: Admin Web App (RECOMMENDED)

**Location:** `netwin-tournaments-admin/netwin-tournament-admin/`

**Steps:**
1. **Login to Admin App**
   - URL: [Your admin app URL]
   - Use admin credentials

2. **Navigate to Wallet Transactions**
   - Click "Wallet" in sidebar
   - Go to "Pending Deposits" tab

3. **Review Deposit:**
   - See user details
   - View amount
   - Check UPI Transaction ID
   - **Click to view screenshot**

4. **Verify Payment:**
   - Match UPI Transaction ID
   - Verify amount matches
   - Check screenshot is genuine
   - Confirm payment to correct UPI ID

5. **Approve or Reject:**
   - **Approve:** Click "Approve" button
   - **Reject:** Click "Reject" button, add reason

6. **Automatic Processing:**
   - ✅ Creates `wallet_transactions` document
   - ✅ Updates user's `wallets/{userId}` balance
   - ✅ Changes status to "APPROVED"
   - ✅ User receives notification (if enabled)

---

### Method 2: Firebase Console (Manual)

**Use only if Admin App is unavailable**

**Steps:**

**1. Open Firebase Console**
```
https://console.firebase.google.com/
→ Select: netwin-tournament project
→ Go to: Firestore Database
```

**2. Navigate to pending_deposits**
```
Firestore Database
  ├─ pending_deposits
      └─ [depositId]
```

**3. Find Pending Deposit**
- Look for documents with `status: "PENDING"`
- Check `submittedAt` timestamp for recent ones

**Example Document:**
```json
{
  "id": "BwQde3Ih5GX1WqkWn8jw",
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "amount": 1.0,
  "currency": "INR",
  "upiTransactionId": "563064706380",
  "paymentScreenshot": "https://firebasestorage.googleapis.com/...",
  "status": "PENDING",
  "submittedAt": "2025-10-09T06:34:16Z"
}
```

**4. View Screenshot**
- Copy `paymentScreenshot` URL
- Paste in browser
- Verify payment details

**5. Verify Payment**
- Check UPI Transaction ID matches
- Verify amount
- Confirm payment made to: `sakshipuria@okaxis`

**6. If Approved - Perform THREE Operations:**

**Operation A: Update pending_deposits status**
```javascript
// In pending_deposits/{depositId}
{
  "status": "APPROVED",
  "processedAt": [current timestamp],
  "processedBy": [your admin userId]
}
```

**Operation B: Create wallet_transactions document**
```javascript
// Create new document in wallet_transactions
{
  "id": "txn_deposit_[random]",
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "type": "deposit",
  "amount": 1.0,
  "status": "completed",
  "description": "UPI Deposit",
  "currency": "INR",
  "timestamp": [current timestamp],
  "referenceId": "563064706380"
}
```

**Operation C: Update wallets balance**
```javascript
// In wallets/{userId}
{
  "balance": 1401.0 + 1.0 = 1402.0,
  "withdrawableBalance": 600.0 + 1.0 = 601.0,
  "lastUpdated": [current timestamp]
}
```

**7. If Rejected:**
```javascript
// In pending_deposits/{depositId}
{
  "status": "REJECTED",
  "processedAt": [current timestamp],
  "processedBy": [your admin userId],
  "rejectionReason": "Invalid screenshot" // Or other reason
}
```

**⚠️ IMPORTANT:** If rejected, DO NOT update wallet balance or create transaction.

---

## 3. What Changes After Approval

### Firebase Changes:

**1. pending_deposits/{depositId}**
```diff
- status: "PENDING"
+ status: "APPROVED"
+ processedAt: "2025-10-09T07:00:00Z"
+ processedBy: "admin_user_id"
```

**2. wallet_transactions (NEW document)**
```json
{
  "id": "txn_deposit_001",
  "userId": "INyrJG4Fb7UQ4qwmcc2jkkArSXW2",
  "type": "deposit",
  "amount": 1.0,
  "status": "completed",
  "description": "UPI Deposit",
  "currency": "INR",
  "timestamp": "2025-10-09T07:00:00Z",
  "referenceId": "563064706380"
}
```

**3. wallets/{userId}**
```diff
{
- "balance": 1401.0,
+ "balance": 1402.0,
- "withdrawableBalance": 600.0,
+ "withdrawableBalance": 601.0,
  "bonusBalance": 801.0,
  "currency": "INR",
- "lastUpdated": "2025-10-09T06:33:26Z"
+ "lastUpdated": "2025-10-09T07:00:00Z"
}
```

### User App Changes:

**Wallet Screen:**
```diff
Total Balance:
- ₹1,401
+ ₹1,402

User Added Balance:
- ₹600
+ ₹601

Pending Deposits:
- Shows deposit card with "Pending" status
+ Card disappears (or shows "Approved")

Transaction History:
- "No transactions yet"
+ Shows: "UPI Deposit  +₹1.00  09 Oct 2025"
```

---

## 4. Verification Checklist

Before approving, verify:

- [ ] **UPI Transaction ID is valid** (12 digits)
- [ ] **Amount matches** screenshot and request
- [ ] **Payment made to correct UPI ID** (sakshipuria@okaxis)
- [ ] **Screenshot is clear and genuine**
- [ ] **No duplicate submission** (check transaction ID history)
- [ ] **User account is active**

---

## 5. Common Issues & Solutions

### Issue 1: Screenshot Not Loading
**Solution:** 
- Copy URL and paste in new browser tab
- Check Firebase Storage permissions
- Contact developer if Storage rules issue

### Issue 2: Duplicate Transaction ID
**Solution:**
- User may have submitted same payment twice
- Check `wallet_transactions` for existing record
- Reject duplicate with reason: "Already processed"

### Issue 3: Amount Mismatch
**Solution:**
- Screenshot shows ₹500 but request shows ₹100
- Reject with reason: "Amount mismatch - please submit correct amount"

### Issue 4: Wrong UPI ID
**Solution:**
- Payment made to different UPI ID
- Reject with reason: "Payment not received - wrong UPI ID"

### Issue 5: Fake/Edited Screenshot
**Solution:**
- Look for signs of editing (inconsistent fonts, colors)
- Verify transaction ID format
- Reject with reason: "Invalid screenshot"

---

## 6. Approval Response Times

**Target SLA:**
- **Peak hours (9 AM - 9 PM IST):** Within 30 minutes
- **Off hours:** Within 2 hours
- **Maximum:** 24 hours

**Best Practices:**
- Check pending deposits every 30 minutes during peak hours
- Set up notifications for new deposits (future feature)
- Maintain approval log for audit

---

## 7. Current Pending Deposit

**Example from logs:**

```
Document ID: BwQde3Ih5GX1WqkWn8jw
User ID: INyrJG4Fb7UQ4qwmcc2jkkArSXW2
Amount: ₹1.00
UPI Txn ID: 563064706380
Screenshot: https://firebasestorage.googleapis.com/.../deposit_INyrJG4Fb7UQ4qwmcc2jkkArSXW2_1759971838101.jpg
Status: PENDING
Submitted: 09 Oct 2025, 06:34:16 IST
```

**Action Required:** Admin needs to approve this test deposit

---

## 8. Reporting

### Daily Report:
- Total deposits received
- Total approved
- Total rejected
- Total amount processed
- Average approval time

### Weekly Report:
- Trends in deposit amounts
- Peak deposit times
- Rejection reasons analysis
- User feedback on deposit process

---

## 9. Security Considerations

**Fraud Prevention:**
- Always verify screenshot authenticity
- Check for duplicate transaction IDs
- Monitor unusual patterns (multiple small deposits, rapid submissions)
- Flag suspicious accounts for review

**Data Privacy:**
- Screenshots contain sensitive payment info
- Only authorized admins should access
- Delete old screenshots after 90 days (future implementation)

**Access Control:**
- Only users with `role: "admin"` can approve
- All approvals are logged with admin ID
- Maintain audit trail

---

## 10. Admin App Features (When Available)

**Current:**
- View pending deposits
- View screenshot
- Approve/Reject with one click
- See user wallet balance before/after

**Future Enhancements:**
- Automated verification (check with payment gateway API)
- Batch approval
- Push notifications for new deposits
- Dashboard analytics
- Export reports

---

## Quick Reference

**Admin App URL:** [To be provided]

**Firestore Collections:**
- Pending Deposits: `pending_deposits`
- Transactions: `wallet_transactions`
- Wallets: `wallets`

**Test Deposit Details:**
```
ID: BwQde3Ih5GX1WqkWn8jw
User: INyrJG4Fb7UQ4qwmcc2jkkArSXW2
Amount: ₹1.00
Status: PENDING
Action: Ready for approval
```

---

## Support

**For technical issues:**
- Contact: Developer Team
- Documentation: `/docs` folder
- Firebase Console: https://console.firebase.google.com/

**For process questions:**
- Refer to this document
- Check Admin App help section
- Contact Operations Team
