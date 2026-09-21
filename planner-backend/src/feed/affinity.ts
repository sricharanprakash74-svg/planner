import { PrismaClient } from "@prisma/client";
import type { FeedInteractionPayload, InteractionType } from "./types.js";

export function slugify(text: string): string {
  return text
    .trim()
    .toLowerCase()
    .replace(/[\s_]+/g, "-")
    .replace(/[^a-z0-9-]/g, "")
    .replace(/-+/g, "-");
}

export function calculateExpectedReadSeconds(wordCount: number): number {
  return Math.max(wordCount / 3.5, 4.0);
}

export function clampWeight(weight: number): number {
  return Math.max(-5.0, Math.min(30.0, weight));
}

export function calculateDeltaWeight(
  interactionType: InteractionType,
  expectedReadSeconds: number,
  activeSeconds?: number
): number {
  switch (interactionType) {
    case "view": {
      if (activeSeconds === undefined || activeSeconds === null) {
        return 0.0;
      }
      if (activeSeconds < 4.0) {
        // Bounce penalty
        return -1.5;
      }
      // Cap excess duration at 3.0x expected read time
      const effectiveSeconds = Math.min(activeSeconds, expectedReadSeconds * 3.0);
      const ratio = effectiveSeconds / expectedReadSeconds;

      if (ratio >= 0.7) {
        // Deep Read
        return 3.0;
      } else if (ratio >= 0.3) {
        // Skim
        return 1.0;
      }
      return 0.0;
    }
    case "like":
      return 1.5;
    case "comment":
      return 3.5;
    case "share":
      return 4.0;
    case "save":
    case "adopt_plan":
      return 5.0;
    case "dismiss":
      return -2.0;
    default:
      return 0.0;
  }
}

export async function processInteraction(
  prisma: PrismaClient,
  payload: FeedInteractionPayload
) {
  const post = await prisma.post.findUnique({
    where: { id: payload.post_id },
    include: {
      postTags: {
        include: {
          tag: true,
        },
      },
    },
  });

  if (!post) {
    throw new Error(`Post ${payload.post_id} not found`);
  }

  // 1. Update post counters if applicable
  const counterUpdates: Record<string, { increment: number }> = {};
  if (payload.interaction_type === "like") {
    counterUpdates.likesCount = { increment: 1 };
  } else if (payload.interaction_type === "comment") {
    counterUpdates.commentsCount = { increment: 1 };
  } else if (payload.interaction_type === "share") {
    counterUpdates.sharesCount = { increment: 1 };
  } else if (
    payload.interaction_type === "save" ||
    payload.interaction_type === "adopt_plan"
  ) {
    counterUpdates.savesCount = { increment: 1 };
  }

  if (Object.keys(counterUpdates).length > 0) {
    await prisma.post.update({
      where: { id: post.id },
      data: counterUpdates,
    });
  }

  // 2. Compute delta weight for post tags
  const expectedReadSeconds = calculateExpectedReadSeconds(post.wordCount);
  const deltaW = calculateDeltaWeight(
    payload.interaction_type,
    expectedReadSeconds,
    payload.active_seconds
  );

  // 3. Update UserTagAffinities for existing post tags
  const tagIds = Array.from(new Set(post.postTags.map((pt) => pt.tagId)));

  for (const tagId of tagIds) {
    const currentAffinity = await prisma.userTagAffinity.findUnique({
      where: {
        userId_tagId: {
          userId: payload.userId,
          tagId,
        },
      },
    });

    const currentWeight = currentAffinity ? currentAffinity.weight : 1.0;
    const newWeight = clampWeight(currentWeight + deltaW);

    await prisma.userTagAffinity.upsert({
      where: {
        userId_tagId: {
          userId: payload.userId,
          tagId,
        },
      },
      create: {
        userId: payload.userId,
        tagId,
        weight: newWeight,
        lastActiveAt: new Date(),
      },
      update: {
        weight: newWeight,
        lastActiveAt: new Date(),
      },
    });
  }

  // 4. Handle Comment Sub-Tags if present
  if (
    payload.interaction_type === "comment" &&
    payload.sub_tags &&
    Array.isArray(payload.sub_tags) &&
    payload.sub_tags.length > 0
  ) {
    for (const rawTag of payload.sub_tags) {
      const slug = slugify(rawTag);
      if (!slug) continue;

      // Upsert Tag
      const tag = await prisma.tag.upsert({
        where: { name: slug },
        create: { name: slug },
        update: {},
      });

      // Upsert PostTag with source_type = 'COMMENTER'
      const existingPostTag = await prisma.postTag.findUnique({
        where: {
          postId_tagId_sourceType: {
            postId: post.id,
            tagId: tag.id,
            sourceType: "COMMENTER",
          },
        },
      });

      if (existingPostTag) {
        await prisma.postTag.update({
          where: {
            postId_tagId_sourceType: {
              postId: post.id,
              tagId: tag.id,
              sourceType: "COMMENTER",
            },
          },
          data: {
            endorsementCount: { increment: 1 },
          },
        });
      } else {
        await prisma.postTag.create({
          data: {
            postId: post.id,
            tagId: tag.id,
            sourceType: "COMMENTER",
            endorsementCount: 1,
          },
        });
      }

      // Apply +3.5 to commenter's affinity for these sub-tags
      const commenterAffinity = await prisma.userTagAffinity.findUnique({
        where: {
          userId_tagId: {
            userId: payload.userId,
            tagId: tag.id,
          },
        },
      });

      const commenterWeight = commenterAffinity ? commenterAffinity.weight : 1.0;
      const newCommenterWeight = clampWeight(commenterWeight + 3.5);

      await prisma.userTagAffinity.upsert({
        where: {
          userId_tagId: {
            userId: payload.userId,
            tagId: tag.id,
          },
        },
        create: {
          userId: payload.userId,
          tagId: tag.id,
          weight: newCommenterWeight,
          lastActiveAt: new Date(),
        },
        update: {
          weight: newCommenterWeight,
          lastActiveAt: new Date(),
        },
      });
    }
  }

  return {
    success: true,
    postId: post.id,
    deltaW,
    expectedReadSeconds,
  };
}
