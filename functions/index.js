require('dotenv').config();
const apiKey = process.env.API_KEY;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const Razorpay = require("razorpay");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

function getRazorpayClient() {
  const keyId = process.env.RZP_KEY_ID || functions.config().rzp?.key_id;
  const keySecret = process.env.RZP_KEY_SECRET || functions.config().rzp?.key_secret;
  if (!keyId || !keySecret) {
    throw new Error("Razorpay keys not configured");
  }
  return new Razorpay({ key_id: keyId, key_secret: keySecret });
}

// POST /payments/razorpay/order
app.post("/payments/razorpay/order", async (req, res) => {
  try {
    const { amountMinor, currency = "INR", receipt, notes } = req.body || {};
    if (!amountMinor || amountMinor <= 0) {
      return res.status(400).json({ error: "amountMinor must be > 0" });
    }

    const rzp = getRazorpayClient();
    const order = await rzp.orders.create({
      amount: amountMinor,
      currency,
      receipt: receipt || `rcpt_${Date.now()}`,
      notes: notes || {},
    });
    return res.json({ order });
  } catch (e) {
    console.error("/payments/razorpay/order error", e);
    return res.status(500).json({ error: e.message });
  }
});

// POST /webhooks/razorpay
app.post("/webhooks/razorpay", async (req, res) => {
  try {
    const webhookSecret = process.env.RZP_WEBHOOK_SECRET || functions.config().rzp?.webhook_secret;
    if (!webhookSecret) {
      return res.status(500).json({ error: "Webhook secret not configured" });
    }

    const signature = req.header("x-razorpay-signature");
    const payload = JSON.stringify(req.body);
    const expected = crypto.createHmac("sha256", webhookSecret).update(payload).digest("hex");
    if (signature !== expected) {
      console.warn("Invalid webhook signature");
      return res.status(401).json({ error: "Invalid signature" });
    }

    const evt = req.body?.event;
    const entity = req.body?.payload?.payment?.entity || req.body?.payload?.order?.entity;

    // Map to Firestore
    // Expect notes to include userId and internal transactionId
    const notes = entity?.notes || {};
    const userId = notes.userId;
    const transactionId = notes.transactionId;
    const amount = (entity?.amount || 0) / 100.0;
    const currency = entity?.currency || "INR";
    const status = entity?.status;
    const gatewayReference = entity?.id || entity?.order_id;

    if (userId && transactionId) {
      const txRef = db.collection("wallet_transactions").doc(transactionId);
      const walletRef = db.collection("wallets").doc(userId);

      await db.runTransaction(async (t) => {
        const txSnap = await t.get(txRef);
        const txExists = txSnap.exists;

        // Update transaction status
        t.set(txRef, {
          userId,
          amount,
          currency,
          gateway: "RAZORPAY",
          gatewayReference,
          status: (evt === "payment.captured" || evt === "order.paid") ? "COMPLETED" : (evt?.includes("failed") ? "FAILED" : status?.toUpperCase() || "PENDING"),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });

        if ((evt === "payment.captured" || evt === "order.paid") && amount > 0) {
          // Credit wallet on success
          const walletSnap = await t.get(walletRef);
          const withdrawable = (walletSnap.get("withdrawableBalance") || 0) + amount;
          const bonus = walletSnap.get("bonusBalance") || 0;
          const total = withdrawable + bonus;
          t.set(walletRef, {
            withdrawableBalance: withdrawable,
            balance: total,
            currency,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
          }, { merge: true });
        }
      });
    }

    return res.json({ ok: true });
  } catch (e) {
    console.error("/webhooks/razorpay error", e);
    return res.status(500).json({ error: e.message });
  }
});

exports.api = functions.https.onRequest(app);


