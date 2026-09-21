import express from "express";
import cors from "cors";
import { PrismaClient } from "@prisma/client";
import Stripe from "stripe";
import dotenv from "dotenv";
import { createFeedRouter } from "./feed/routes.js";
import { createProxyRouter } from "./proxy.js";
import { createSocialRouter } from "./social/routes.js";

dotenv.config();

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || "sk_test_mock", {
  apiVersion: "2024-06-20" as any, // or the latest
});

const app = express();
const prisma = new PrismaClient();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Feed Recommendation & Interaction routes
app.use("/api/feed", createFeedRouter(prisma));

// Social Planning & Community routes
app.use("/api/social", createSocialRouter(prisma));

// Secure Upstream API Proxy Gateway
app.use("/api/proxy", createProxyRouter());

// Stub for Firebase Auth verification
// In production, we'd use `firebase-admin` to verify the ID token.
app.post("/api/auth/login", async (req, res) => {
  const { firebaseToken, email, displayName } = req.body;

  if (!firebaseToken) {
    return res.status(400).json({ error: "Missing firebaseToken" });
  }

  try {
    // For now, we trust the client and use the token directly as the UID
    const uid = firebaseToken; 

    // Find or create user
    let user = await prisma.user.findUnique({
      where: { firebaseUid: uid },
    });

    if (!user) {
      user = await prisma.user.create({
        data: {
          firebaseUid: uid,
          email: email,
          displayName: displayName || "User",
        },
      });
    }

    res.json(user);
  } catch (error) {
    console.error("Auth error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

app.post("/api/sync", async (req, res) => {
  const { userId, plans, templates, checkins, badges } = req.body;

  if (!userId) {
    return res.status(400).json({ error: "Missing userId" });
  }

  try {
    // Resolve user: find by firebaseUid or id, or create a record for this userId
    let user = await prisma.user.findFirst({
      where: {
        OR: [
          { firebaseUid: String(userId) },
          ...(typeof userId === "number" || /^\d+$/.test(String(userId)) ? [{ id: Number(userId) }] : []),
        ],
      },
    });

    if (!user) {
      user = await prisma.user.create({
        data: {
          firebaseUid: String(userId),
          displayName: `User_${String(userId).substring(0, 8)}`,
        },
      });
    }
    const dbUserId = user.id;

    // 1. Insert/Update Plans
    if (plans && Array.isArray(plans)) {
      for (const p of plans) {
        const planId = Number(p.planId ?? p.id);
        if (isNaN(planId) || planId <= 0) continue;

        await prisma.plan.upsert({
          where: { id: planId },
          update: {
            heading: p.heading,
            description: p.description ?? null,
            startDate: p.startDate,
            endDate: p.endDate,
          },
          create: {
            id: planId,
            userId: dbUserId,
            heading: p.heading,
            description: p.description ?? null,
            startDate: p.startDate,
            endDate: p.endDate,
          },
        });
      }
    }

    // 2. Insert/Update Templates
    if (templates && Array.isArray(templates)) {
      for (const t of templates) {
        const templateId = Number(t.templateId ?? t.id);
        const planId = Number(t.planId);
        if (isNaN(templateId) || templateId <= 0 || isNaN(planId) || planId <= 0) continue;

        await prisma.taskTemplate.upsert({
          where: { id: templateId },
          update: {
            taskDescription: t.taskDescription,
            selectedDays: t.selectedDays,
          },
          create: {
            id: templateId,
            planId: planId,
            taskDescription: t.taskDescription,
            selectedDays: t.selectedDays,
          },
        });
      }
    }

    // 3. Insert/Update Checkins
    if (checkins && Array.isArray(checkins)) {
      for (const c of checkins) {
        const checkinId = Number(c.checkinId ?? c.id);
        const templateId = Number(c.templateId);
        if (isNaN(checkinId) || checkinId <= 0 || isNaN(templateId) || templateId <= 0) continue;

        const completedAt = c.completedAt
          ? (typeof c.completedAt === "number" ? new Date(c.completedAt) : new Date(String(c.completedAt)))
          : null;

        await prisma.dailyCheckin.upsert({
          where: { id: checkinId },
          update: {
            isCompleted: Boolean(c.isCompleted),
            completedAt: completedAt,
            timezoneOffset: c.timezoneOffset ?? null,
          },
          create: {
            id: checkinId,
            templateId: templateId,
            exactDate: c.exactDate,
            isCompleted: Boolean(c.isCompleted),
            completedAt: completedAt,
            timezoneOffset: c.timezoneOffset ?? null,
          },
        });
      }
    }

    res.json({ success: true, message: "Sync successful" });
  } catch (error) {
    console.error("Sync error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// ── Creator Monetization & Payout Endpoints ──────────────────

async function getOrCreateCreatorProfile(userId: number) {
  let profile = await prisma.creatorProfile.findUnique({
    where: { userId },
  });
  if (!profile) {
    let user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      user = await prisma.user.create({
        data: {
          id: userId,
          displayName: `Creator_${userId}`,
        },
      });
    }
    profile = await prisma.creatorProfile.create({
      data: {
        userId: user.id,
        payoutMethod: "PAYPAL",
        payoutAccount: "",
        earnedCredits: 0,
        redeemableCredits: 0,
        totalPaidUsd: 0.0,
      },
    });
  }
  return profile;
}

// 1. Get Creator Dashboard Metrics
app.get("/api/creator/:userId/dashboard", async (req, res) => {
  try {
    const userId = parseInt(req.params.userId, 10);
    if (isNaN(userId)) {
      return res.status(400).json({ error: "Invalid userId" });
    }

    const profile = await getOrCreateCreatorProfile(userId);
    const recentSales = await prisma.planSale.findMany({
      where: { creatorId: userId },
      orderBy: { createdAt: "desc" },
      take: 10,
    });
    const recentPayouts = await prisma.payoutRequest.findMany({
      where: { creatorId: userId },
      orderBy: { createdAt: "desc" },
      take: 10,
    });
    const totalSalesCount = await prisma.planSale.count({
      where: { creatorId: userId },
    });

    const estimatedUsd = +(profile.redeemableCredits * 0.007).toFixed(2);

    res.json({
      userId: profile.userId,
      earnedCredits: profile.earnedCredits,
      redeemableCredits: profile.redeemableCredits,
      estimatedUsd,
      totalPaidUsd: profile.totalPaidUsd,
      payoutMethod: profile.payoutMethod,
      payoutAccount: profile.payoutAccount,
      salesCount: totalSalesCount,
      minCashOutCredits: 500,
      minCashOutUsd: 3.50,
      recentSales: recentSales.map((s) => ({
        id: s.id,
        planTitle: s.planTitle,
        buyerName: s.buyerName,
        creditAmount: s.creditAmount,
        creatorEarning: s.creatorEarning,
        createdAt: s.createdAt.toISOString(),
      })),
      recentPayouts: recentPayouts.map((p) => ({
        id: p.id,
        referenceId: p.referenceId,
        creditsDeducted: p.creditsDeducted,
        amountUsd: p.amountUsd,
        payoutMethod: p.payoutMethod,
        payoutAccount: p.payoutAccount,
        status: p.status,
        createdAt: p.createdAt.toISOString(),
      })),
    });
  } catch (error) {
    console.error("Creator dashboard error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// 2. Update Creator Payout Settings
app.post("/api/creator/payout-settings", async (req, res) => {
  try {
    const { userId, payoutMethod, payoutAccount } = req.body;
    if (!userId || !payoutMethod || !payoutAccount) {
      return res.status(400).json({ error: "Missing required fields" });
    }
    const id = parseInt(userId, 10);
    await getOrCreateCreatorProfile(id);

    const updated = await prisma.creatorProfile.update({
      where: { userId: id },
      data: {
        payoutMethod,
        payoutAccount,
      },
    });

    res.json({
      success: true,
      message: "Payout settings updated successfully",
      payoutMethod: updated.payoutMethod,
      payoutAccount: updated.payoutAccount,
    });
  } catch (error) {
    console.error("Payout settings error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// 3. Register Plan Sale (User spent credits to unlock creator's plan)
app.post("/api/creator/sell-plan", async (req, res) => {
  try {
    const { creatorId, buyerId, buyerName, planId, planTitle, creditAmount } = req.body;
    if (!creatorId || !planTitle || creditAmount === undefined) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    const cId = parseInt(creatorId, 10);
    const credits = parseInt(creditAmount, 10);
    const creatorEarning = Math.max(1, Math.round(credits * 0.70));

    await getOrCreateCreatorProfile(cId);

    await prisma.creatorProfile.update({
      where: { userId: cId },
      data: {
        earnedCredits: { increment: creatorEarning },
        redeemableCredits: { increment: creatorEarning },
      },
    });

    const sale = await prisma.planSale.create({
      data: {
        creatorId: cId,
        buyerId: buyerId ? parseInt(buyerId, 10) : null,
        buyerName: buyerName || "Community Member",
        planId: planId ? parseInt(planId, 10) : 0,
        planTitle,
        creditAmount: credits,
        creatorEarning,
      },
    });

    res.json({
      success: true,
      creatorEarning,
      saleId: sale.id,
    });
  } catch (error) {
    console.error("Sell plan error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// 4. Request Creator Cash-Out (Deducts credits, issues payout)
app.post("/api/creator/payout", async (req, res) => {
  try {
    const { userId, creditsToRedeem, payoutMethod, payoutAccount } = req.body;
    const id = parseInt(userId, 10);
    const credits = parseInt(creditsToRedeem, 10);

    if (isNaN(id) || isNaN(credits) || credits <= 0) {
      return res.status(400).json({ success: false, message: "Invalid payout request parameters" });
    }

    const profile = await getOrCreateCreatorProfile(id);

    if (credits < 500) {
      return res.status(400).json({
        success: false,
        message: "Minimum cash-out threshold is 500 credits ($3.50 USD)",
      });
    }

    if (profile.redeemableCredits < credits) {
      return res.status(400).json({
        success: false,
        message: `Insufficient redeemable credits. You have ${profile.redeemableCredits} available.`,
      });
    }

    const method = payoutMethod || profile.payoutMethod || "PAYPAL";
    const account = payoutAccount || profile.payoutAccount;

    if (!account) {
      return res.status(400).json({
        success: false,
        message: "Please configure your payout account information first.",
      });
    }

    const amountUsd = +(credits * 0.007).toFixed(2);
    const referenceId = `PAYOUT_${Date.now()}_${Math.random().toString(36).substring(2, 7).toUpperCase()}`;

    const [payoutRequest, updatedProfile] = await prisma.$transaction([
      prisma.payoutRequest.create({
        data: {
          creatorId: id,
          creditsDeducted: credits,
          amountUsd,
          payoutMethod: method,
          payoutAccount: account,
          status: "COMPLETED",
          referenceId,
        },
      }),
      prisma.creatorProfile.update({
        where: { userId: id },
        data: {
          redeemableCredits: { decrement: credits },
          totalPaidUsd: { increment: amountUsd },
        },
      }),
    ]);

    res.json({
      success: true,
      message: `Successfully processed cash-out of $${amountUsd.toFixed(2)} USD!`,
      referenceId,
      amountUsd,
      creditsDeducted: credits,
      remainingCredits: updatedProfile.redeemableCredits,
    });
  } catch (error) {
    console.error("Payout error:", error);
    res.status(500).json({ success: false, message: "Internal server error" });
  }
});

// 5. Get Creator Payouts History
app.get("/api/creator/:userId/payouts", async (req, res) => {
  try {
    const userId = parseInt(req.params.userId, 10);
    if (isNaN(userId)) {
      return res.status(400).json({ error: "Invalid userId" });
    }

    const payouts = await prisma.payoutRequest.findMany({
      where: { creatorId: userId },
      orderBy: { createdAt: "desc" },
    });

    res.json({
      userId,
      payouts: payouts.map((p) => ({
        id: p.id,
        referenceId: p.referenceId,
        creditsDeducted: p.creditsDeducted,
        amountUsd: p.amountUsd,
        payoutMethod: p.payoutMethod,
        payoutAccount: p.payoutAccount,
        status: p.status,
        createdAt: p.createdAt.toISOString(),
      })),
    });
  } catch (error) {
    console.error("Get payouts error:", error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// ── Stripe Payments Integration ──────────────────

app.post("/api/payments/create-intent", async (req, res) => {
  try {
    const { amount, currency = "usd", userId } = req.body;
    
    if (!amount) {
      return res.status(400).json({ error: "Amount is required" });
    }

    // In a real app, create a Stripe Customer for the user to save cards:
    // const customer = await stripe.customers.create();
    
    // Create a PaymentIntent with the order amount and currency
    const paymentIntent = await stripe.paymentIntents.create({
      amount: amount, // amount in cents
      currency: currency,
      // automatic_payment_methods: { enabled: true },
      // To use PaymentSheet on Android, you often just need standard config,
      // but let's keep it simple.
    });

    res.json({
      clientSecret: paymentIntent.client_secret,
      paymentIntentId: paymentIntent.id
    });
  } catch (error: any) {
    console.error("Stripe error:", error);
    res.status(500).json({ error: error.message });
  }
});

app.post("/api/payments/verify", async (req, res) => {
  try {
    const { paymentIntentId } = req.body;
    
    if (!paymentIntentId) {
       return res.status(400).json({ error: "Missing paymentIntentId" });
    }
    
    const intent = await stripe.paymentIntents.retrieve(paymentIntentId);
    
    if (intent.status === "succeeded") {
       // In a full implementation, you'd record this intent in the DB to prevent double-crediting
       return res.json({ success: true, status: intent.status });
    } else {
       return res.json({ success: false, status: intent.status });
    }
  } catch (error: any) {
    console.error("Verify payment error:", error);
    res.status(500).json({ error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`Planner API running on http://localhost:${PORT}`);
});
