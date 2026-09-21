import { Router } from "express";
import { PrismaClient } from "@prisma/client";

export function createSocialRouter(prisma: PrismaClient): Router {
  const router = Router();

  /**
   * GET /api/social/search
   * Unified search returning cleanly separated plans and users
   */
  router.get("/search", async (req, res) => {
    try {
      const q = ((req.query.q as string) || "").trim().toLowerCase();
      if (!q) {
        return res.json({ plans: [], creators: [] });
      }

      // Search matching public plans
      const matchingPlans = await prisma.publicPlan.findMany({
        where: {
          isArchived: false,
          OR: [
            { title: { contains: q } },
            { description: { contains: q } },
            { tags: { contains: q } },
            { category: { contains: q } },
          ],
        },
        include: {
          user: {
            select: {
              id: true,
              displayName: true,
              email: true,
              creatorProfile: true,
            },
          },
        },
        take: 30,
      });

      // Search matching creators / users
      const matchingUsers = await prisma.user.findMany({
        where: {
          OR: [
            { displayName: { contains: q } },
            { email: { contains: q } },
          ],
        },
        include: {
          creatorProfile: true,
          _count: {
            select: {
              publicPlans: true,
              followers: true,
              following: true,
            },
          },
        },
        take: 20,
      });

      res.json({
        plans: matchingPlans,
        creators: matchingUsers,
      });
    } catch (error: any) {
      console.error("Social search error:", error);
      res.status(500).json({ error: error.message || "Search failed" });
    }
  });

  /**
   * GET /api/social/plans
   * List public plans with filters: trending, recent, most_downloaded, following
   */
  router.get("/plans", async (req, res) => {
    try {
      const filter = (req.query.filter as string) || "trending";
      const category = req.query.category as string | undefined;
      const tag = req.query.tag as string | undefined;
      const currentUserId = req.query.userId ? parseInt(req.query.userId as string, 10) : undefined;

      const whereClause: any = { isArchived: false };

      if (category && category !== "All") {
        whereClause.category = category;
      }
      if (tag) {
        whereClause.tags = { contains: tag.toLowerCase() };
      }

      let orderBy: any = { createdAt: "desc" };
      if (filter === "trending") {
        orderBy = [{ upvoteCount: "desc" }, { createdAt: "desc" }];
      } else if (filter === "most_downloaded") {
        orderBy = [{ downloadCount: "desc" }, { createdAt: "desc" }];
      } else if (filter === "recent") {
        orderBy = { createdAt: "desc" };
      } else if (filter === "following" && currentUserId) {
        const followingRecords = await prisma.userFollow.findMany({
          where: { followerId: currentUserId },
          select: { followingId: true },
        });
        const followingIds = followingRecords.map((f) => f.followingId);
        whereClause.userId = { in: followingIds };
      }

      const plans = await prisma.publicPlan.findMany({
        where: whereClause,
        orderBy,
        take: 50,
        include: {
          user: {
            select: {
              id: true,
              displayName: true,
              email: true,
              creatorProfile: true,
            },
          },
        },
      });

      res.json(plans);
    } catch (error: any) {
      console.error("Fetch plans error:", error);
      res.status(500).json({ error: error.message || "Failed to fetch plans" });
    }
  });

  /**
   * GET /api/social/plans/:id
   * Get single public plan with template details
   */
  router.get("/plans/:id", async (req, res) => {
    try {
      const planId = parseInt(req.params.id, 10);
      const plan = await prisma.publicPlan.findUnique({
        where: { id: planId },
        include: {
          user: {
            select: {
              id: true,
              displayName: true,
              email: true,
              creatorProfile: true,
            },
          },
          _count: {
            select: {
              votes: true,
              comments: true,
              saves: true,
              uses: true,
            },
          },
        },
      });

      if (!plan || plan.isArchived) {
        return res.status(404).json({ error: "Public plan not found" });
      }

      res.json(plan);
    } catch (error: any) {
      console.error("Get plan error:", error);
      res.status(500).json({ error: error.message || "Failed to load plan" });
    }
  });

  /**
   * POST /api/social/plans
   * Publish a plan to community feed
   */
  router.post("/plans", async (req, res) => {
    try {
      const {
        userId,
        title,
        description,
        targetDurationDays,
        defaultTaskDurationDays,
        tags,
        category,
        planTemplateJson,
      } = req.body;

      if (!userId || !title) {
        return res.status(400).json({ error: "Missing required fields: userId, title" });
      }

      const newPlan = await prisma.publicPlan.create({
        data: {
          userId: parseInt(userId, 10),
          title,
          description: description || "",
          targetDurationDays: targetDurationDays || 30,
          defaultTaskDurationDays: defaultTaskDurationDays || 1,
          tags: Array.isArray(tags) ? tags.join(",") : (tags || ""),
          category: category || "General",
          planTemplateJson: typeof planTemplateJson === "string" ? planTemplateJson : JSON.stringify(planTemplateJson || {}),
        },
      });

      res.status(201).json(newPlan);
    } catch (error: any) {
      console.error("Publish plan error:", error);
      res.status(500).json({ error: error.message || "Failed to publish plan" });
    }
  });

  /**
   * POST /api/social/plans/:id/vote
   * Cast Upvote or Downvote
   */
  router.post("/plans/:id/vote", async (req, res) => {
    try {
      const planId = parseInt(req.params.id, 10);
      const { userId, voteType } = req.body; // 'UP' | 'DOWN'

      if (!userId || !voteType) {
        return res.status(400).json({ error: "Missing userId or voteType" });
      }

      const existingVote = await prisma.planVote.findUnique({
        where: {
          userId_planId: { userId: parseInt(userId, 10), planId },
        },
      });

      if (existingVote) {
        if (existingVote.voteType === voteType) {
          // Cancel vote
          await prisma.planVote.delete({ where: { id: existingVote.id } });
          const delta = voteType === "UP" ? -1 : 1;
          await prisma.publicPlan.update({
            where: { id: planId },
            data: { upvoteCount: { increment: delta } },
          });
          return res.json({ vote: null });
        } else {
          // Switch vote
          await prisma.planVote.update({
            where: { id: existingVote.id },
            data: { voteType },
          });
          const delta = voteType === "UP" ? 2 : -2;
          await prisma.publicPlan.update({
            where: { id: planId },
            data: { upvoteCount: { increment: delta } },
          });
          return res.json({ vote: voteType });
        }
      } else {
        await prisma.planVote.create({
          data: {
            userId: parseInt(userId, 10),
            planId,
            voteType,
          },
        });
        const delta = voteType === "UP" ? 1 : -1;
        await prisma.publicPlan.update({
          where: { id: planId },
          data: { upvoteCount: { increment: delta } },
        });
        return res.json({ vote: voteType });
      }
    } catch (error: any) {
      console.error("Vote error:", error);
      res.status(500).json({ error: error.message || "Failed to vote" });
    }
  });

  /**
   * POST /api/social/plans/:id/clone
   * Record "Use this plan" clone action
   */
  router.post("/plans/:id/clone", async (req, res) => {
    try {
      const planId = parseInt(req.params.id, 10);
      const { userId, clonedPlanId } = req.body;

      if (!userId) {
        return res.status(400).json({ error: "Missing userId" });
      }

      await prisma.planUse.create({
        data: {
          userId: parseInt(userId, 10),
          originalPlanId: planId,
          clonedPlanId: clonedPlanId ? parseInt(clonedPlanId, 10) : null,
        },
      });

      const updated = await prisma.publicPlan.update({
        where: { id: planId },
        data: { downloadCount: { increment: 1 } },
      });

      res.json({ success: true, downloadCount: updated.downloadCount });
    } catch (error: any) {
      console.error("Clone error:", error);
      res.status(500).json({ error: error.message || "Failed to record clone" });
    }
  });

  /**
   * GET /api/social/users/:id
   * Creator profile info and public plans
   */
  router.get("/users/:id", async (req, res) => {
    try {
      const userId = parseInt(req.params.id, 10);
      const user = await prisma.user.findUnique({
        where: { id: userId },
        include: {
          creatorProfile: true,
          publicPlans: {
            where: { isArchived: false },
            orderBy: { createdAt: "desc" },
          },
          _count: {
            select: {
              followers: true,
              following: true,
              publicPlans: true,
            },
          },
        },
      });

      if (!user) {
        return res.status(404).json({ error: "User not found" });
      }

      res.json(user);
    } catch (error: any) {
      console.error("Get user error:", error);
      res.status(500).json({ error: error.message || "Failed to fetch user" });
    }
  });

  /**
   * POST /api/social/reports
   * Submit a UGC report
   */
  router.post("/reports", async (req, res) => {
    try {
      const { reporterId, targetId, targetType, reason } = req.body;
      if (!reporterId || !targetId || !targetType || !reason) {
        return res.status(400).json({ error: "Missing required report fields" });
      }

      const report = await prisma.contentReport.create({
        data: {
          reporterId: parseInt(reporterId, 10),
          targetId: String(targetId),
          targetType,
          reason,
        },
      });

      res.status(201).json(report);
    } catch (error: any) {
      console.error("Report error:", error);
      res.status(500).json({ error: error.message || "Failed to submit report" });
    }
  });

  /**
   * POST /api/social/blocks
   * Block a user
   */
  router.post("/blocks", async (req, res) => {
    try {
      const { blockerId, blockedId } = req.body;
      if (!blockerId || !blockedId) {
        return res.status(400).json({ error: "Missing blockerId or blockedId" });
      }

      const block = await prisma.userBlock.upsert({
        where: {
          blockerId_blockedId: {
            blockerId: parseInt(blockerId, 10),
            blockedId: parseInt(blockedId, 10),
          },
        },
        update: {},
        create: {
          blockerId: parseInt(blockerId, 10),
          blockedId: parseInt(blockedId, 10),
        },
      });

      // Also remove any follow relationship if present
      await prisma.userFollow.deleteMany({
        where: {
          OR: [
            { followerId: parseInt(blockerId, 10), followingId: parseInt(blockedId, 10) },
            { followerId: parseInt(blockedId, 10), followingId: parseInt(blockerId, 10) },
          ],
        },
      });

      res.json(block);
    } catch (error: any) {
      console.error("Block error:", error);
      res.status(500).json({ error: error.message || "Failed to block user" });
    }
  });

  return router;
}
