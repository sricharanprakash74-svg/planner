import { Router } from "express";
import { PrismaClient } from "@prisma/client";
import { processInteraction, slugify } from "./affinity.js";
import { generatePersonalizedFeed } from "./engine.js";
import type { FeedInteractionPayload } from "./types.js";

export function createFeedRouter(prisma: PrismaClient): Router {
  const router = Router();

  /**
   * POST /api/feed/interaction
   * Processes dwell time, engagement signals, and community sub-tags
   */
  router.post("/interaction", async (req, res) => {
    try {
      const userId =
        req.body.user_id !== undefined
          ? parseInt(req.body.user_id, 10)
          : req.body.userId !== undefined
          ? parseInt(req.body.userId, 10)
          : req.headers["x-user-id"]
          ? parseInt(req.headers["x-user-id"] as string, 10)
          : 1;

      const postId = parseInt(req.body.post_id, 10);
      const interactionType = req.body.interaction_type;
      const activeSeconds =
        req.body.active_seconds !== undefined
          ? parseFloat(req.body.active_seconds)
          : undefined;
      const subTags = req.body.sub_tags;

      if (!postId || isNaN(postId) || !interactionType) {
        return res.status(400).json({
          error: "Missing required fields: post_id and interaction_type",
        });
      }

      // Ensure user exists
      let user = await prisma.user.findUnique({ where: { id: userId } });
      if (!user) {
        user = await prisma.user.create({
          data: {
            id: userId,
            displayName: `User_${userId}`,
          },
        });
      }

      const payload: FeedInteractionPayload = {
        userId: user.id,
        post_id: postId,
        interaction_type: interactionType,
      };

      if (activeSeconds !== undefined && !isNaN(activeSeconds)) {
        payload.active_seconds = activeSeconds;
      }
      if (subTags !== undefined && Array.isArray(subTags)) {
        payload.sub_tags = subTags;
      }

      const result = await processInteraction(prisma, payload);

      res.json(result);
    } catch (error: any) {
      console.error("Feed interaction error:", error);
      res.status(500).json({ error: error.message || "Internal server error" });
    }
  });

  /**
   * GET /api/feed
   * Returns a personalized, ranked feed with anti-fatigue diversity
   */
  router.get("/", async (req, res) => {
    try {
      const rawUserId =
        req.query.userId || req.query.user_id || req.headers["x-user-id"] || "1";
      const userId = parseInt(rawUserId as string, 10) || 1;

      const filterTag = req.query.filter_tag
        ? (req.query.filter_tag as string)
        : null;
      const page = req.query.page ? parseInt(req.query.page as string, 10) : 1;
      const limit = req.query.limit
        ? parseInt(req.query.limit as string, 10)
        : 20;

      const feed = await generatePersonalizedFeed(prisma, {
        userId,
        filterTag,
        page,
        limit,
      });

      res.json(feed);
    } catch (error: any) {
      console.error("Generate feed error:", error);
      res.status(500).json({ error: error.message || "Internal server error" });
    }
  });

  /**
   * POST /api/feed/posts
   * Helper endpoint to create a post with calculated word count and creator tags
   */
  router.post("/posts", async (req, res) => {
    try {
      const {
        title,
        content,
        is_core_theme = false,
        tags = [],
        likes_count = 0,
        saves_count = 0,
        shares_count = 0,
        comments_count = 0,
        created_at,
      } = req.body;

      if (!title || !content) {
        return res.status(400).json({ error: "title and content are required" });
      }

      const words = content.trim().split(/\s+/).filter(Boolean);
      const wordCount = words.length;

      const post = await prisma.post.create({
        data: {
          title,
          content,
          wordCount,
          isCoreTheme: Boolean(is_core_theme),
          likesCount: Number(likes_count),
          savesCount: Number(saves_count),
          sharesCount: Number(shares_count),
          commentsCount: Number(comments_count),
          createdAt: created_at ? new Date(created_at) : new Date(),
        },
      });

      // Attach tags
      for (const rawTag of tags) {
        const slug = slugify(rawTag);
        if (!slug) continue;

        const tagRecord = await prisma.tag.upsert({
          where: { name: slug },
          create: { name: slug },
          update: {},
        });

        await prisma.postTag.upsert({
          where: {
            postId_tagId_sourceType: {
              postId: post.id,
              tagId: tagRecord.id,
              sourceType: "CREATOR",
            },
          },
          create: {
            postId: post.id,
            tagId: tagRecord.id,
            sourceType: "CREATOR",
            endorsementCount: 1,
          },
          update: {},
        });
      }

      const fullPost = await prisma.post.findUnique({
        where: { id: post.id },
        include: {
          postTags: {
            include: { tag: true },
          },
        },
      });

      res.status(201).json(fullPost);
    } catch (error: any) {
      console.error("Create post error:", error);
      res.status(500).json({ error: error.message || "Internal server error" });
    }
  });

  return router;
}
