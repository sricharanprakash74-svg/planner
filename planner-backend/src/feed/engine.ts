import { PrismaClient } from "@prisma/client";
import type {
  CandidatePost,
  FeedQueryOptions,
  FeedResponse,
  PostTagInfo,
  ScoredPost,
} from "./types.js";
import { slugify } from "./affinity.js";

/**
 * Extracts and maps post tags with creator/community flags and identifies the primary tag.
 */
export function extractPostTagsAndPrimary(rawPostTags: any[]): {
  tags: PostTagInfo[];
  primaryTag: string | null;
} {
  const tags: PostTagInfo[] = rawPostTags.map((pt) => ({
    id: pt.tag.id,
    name: pt.tag.name,
    sourceType: pt.sourceType,
    is_creator_tag: pt.sourceType === "CREATOR",
    is_community_tag: pt.sourceType === "COMMENTER",
    endorsement_count: pt.endorsementCount,
  }));

  // Determine primary tag:
  // 1. First choice: Creator tag with the highest endorsement count
  const creatorTags = tags.filter((t) => t.is_creator_tag);
  if (creatorTags.length > 0) {
    creatorTags.sort((a, b) => b.endorsement_count - a.endorsement_count);
    const topCreator = creatorTags[0];
    if (topCreator) {
      return { tags, primaryTag: topCreator.name };
    }
  }

  // 2. Fallback: Community tag with highest endorsement count
  const communityTags = tags.filter((t) => t.is_community_tag);
  if (communityTags.length > 0) {
    communityTags.sort((a, b) => b.endorsement_count - a.endorsement_count);
    const topCommunity = communityTags[0];
    if (topCommunity) {
      return { tags, primaryTag: topCommunity.name };
    }
  }

  return { tags, primaryTag: null };
}

/**
 * Stage 1: Candidate Retrieval (Database)
 */
export async function getCandidatePosts(
  prisma: PrismaClient,
  userId: number,
  filterTag?: string | null,
  limit: number = 100
): Promise<CandidatePost[]> {
  const candidateMap = new Map<number, CandidatePost>();

  function addRawPosts(rawPosts: any[]) {
    for (const post of rawPosts) {
      if (!candidateMap.has(post.id)) {
        const { tags, primaryTag } = extractPostTagsAndPrimary(post.postTags || []);
        candidateMap.set(post.id, {
          id: post.id,
          title: post.title,
          content: post.content,
          wordCount: post.wordCount,
          isCoreTheme: post.isCoreTheme,
          likesCount: post.likesCount,
          savesCount: post.savesCount,
          sharesCount: post.sharesCount,
          commentsCount: post.commentsCount,
          createdAt: post.createdAt,
          tags,
          primaryTag,
        });
      }
    }
  }

  if (filterTag && filterTag.trim() !== "") {
    const slug = slugify(filterTag);
    const tag = await prisma.tag.findUnique({
      where: { name: slug },
    });

    if (!tag) {
      return [];
    }

    const posts = await prisma.post.findMany({
      where: {
        postTags: {
          some: { tagId: tag.id },
        },
      },
      include: {
        postTags: {
          include: { tag: true },
        },
      },
      orderBy: { createdAt: "desc" },
      take: limit,
    });

    addRawPosts(posts);
    return Array.from(candidateMap.values());
  }

  // Blended candidate pool when filterTag is null:

  // 1. User Taste Match (60 posts)
  const topAffinities = await prisma.userTagAffinity.findMany({
    where: {
      userId,
      weight: { gt: 0 },
    },
    orderBy: { weight: "desc" },
    take: 5,
  });

  const topTagIds = topAffinities.map((a) => a.tagId);

  if (topTagIds.length > 0) {
    const tastePosts = await prisma.post.findMany({
      where: {
        postTags: {
          some: { tagId: { in: topTagIds } },
        },
      },
      include: {
        postTags: {
          include: { tag: true },
        },
      },
      orderBy: { createdAt: "desc" },
      take: 60,
    });
    addRawPosts(tastePosts);
  } else {
    // New or cold user fallback: recent posts
    const recentPosts = await prisma.post.findMany({
      include: {
        postTags: {
          include: { tag: true },
        },
      },
      orderBy: { createdAt: "desc" },
      take: 60,
    });
    addRawPosts(recentPosts);
  }

  // 2. Core Theme Anchor (25 posts)
  const corePosts = await prisma.post.findMany({
    where: { isCoreTheme: true },
    include: {
      postTags: {
        include: { tag: true },
      },
    },
    orderBy: { createdAt: "desc" },
    take: 25,
  });
  addRawPosts(corePosts);

  // 3. Discovery Pool (15 posts) - Trending in last 14 days
  const fourteenDaysAgo = new Date(Date.now() - 14 * 24 * 60 * 60 * 1000);
  const discoveryPosts = await prisma.post.findMany({
    where: {
      createdAt: { gte: fourteenDaysAgo },
    },
    include: {
      postTags: {
        include: { tag: true },
      },
    },
    orderBy: [
      { savesCount: "desc" },
      { sharesCount: "desc" },
      { likesCount: "desc" },
    ],
    take: 15,
  });
  addRawPosts(discoveryPosts);

  return Array.from(candidateMap.values());
}

/**
 * Stage 2: In-Memory Scoring & Ranking
 */
export function scoreAndRankCandidates(
  candidates: CandidatePost[],
  userAffinities: Map<number, number>,
  now: Date = new Date()
): ScoredPost[] {
  const scoredPosts: ScoredPost[] = candidates.map((post) => {
    // 1. Tag Match Score
    let tagMatchScore = 0.0;
    for (const tag of post.tags) {
      const userAffinity = userAffinities.get(tag.id) ?? 0.0;
      let wSource = 1.0;
      if (tag.sourceType === "COMMENTER") {
        wSource = 0.5 * Math.min(tag.endorsement_count / 5.0, 1.0);
      }
      tagMatchScore += userAffinity * wSource;
    }

    // 2. Utility & Popularity Score
    const weightedEngagement =
      post.likesCount +
      1 +
      post.savesCount * 2 +
      post.sharesCount * 3 +
      post.commentsCount * 2;
    const utilityScore = Math.log10(Math.max(weightedEngagement, 1.0)) * 4.0;

    // 3. Core App Theme Multiplier
    const coreMultiplier = post.isCoreTheme ? 1.35 : 1.0;

    // 4. Evergreen Time Decay
    const hoursOld = Math.max(
      (now.getTime() - post.createdAt.getTime()) / (1000 * 3600),
      0.0
    );
    const decayScore = 1.0 / Math.pow(hoursOld + 2.0, 0.5);

    // 5. Final Composite Score
    const finalScore =
      (tagMatchScore * 1.5 + utilityScore + 10.0) *
      coreMultiplier *
      decayScore;

    return {
      ...post,
      tagMatchScore,
      utilityScore,
      coreMultiplier,
      decayScore,
      finalScore,
    };
  });

  // Sort descending by finalScore
  scoredPosts.sort((a, b) => b.finalScore - a.finalScore);
  return scoredPosts;
}

/**
 * Stage 3: Diversity Guardrail (Anti-Fatigue Monotony Prevention)
 */
export function applyAntiFatigue(scoredPosts: ScoredPost[]): ScoredPost[] {
  const result = [...scoredPosts];

  for (let i = 1; i < result.length; i++) {
    const prevPost = result[i - 1];
    const currentPost = result[i];
    const prevPrimary = prevPost?.primaryTag;
    const currentPrimary = currentPost?.primaryTag;

    if (prevPrimary && currentPrimary && prevPrimary === currentPrimary) {
      // Find the next candidate with a different primary tag (or null)
      let swapIndex = -1;
      for (let j = i + 1; j < result.length; j++) {
        const candidatePost = result[j];
        if (!candidatePost?.primaryTag || candidatePost.primaryTag !== prevPrimary) {
          swapIndex = j;
          break;
        }
      }

      if (swapIndex !== -1) {
        const diversePost = result.splice(swapIndex, 1)[0];
        if (diversePost) {
          result.splice(i, 0, diversePost);
        }
      }
    }
  }

  return result;
}

/**
 * End-to-end recommendation feed generation pipeline.
 */
export async function generatePersonalizedFeed(
  prisma: PrismaClient,
  options: FeedQueryOptions
): Promise<FeedResponse> {
  const { userId, filterTag, page = 1, limit = 20 } = options;

  // 1. Stage 1: Candidate Retrieval
  const candidates = await getCandidatePosts(prisma, userId, filterTag, 100);

  // 2. Fetch User Tag Affinities into memory map
  const affinities = await prisma.userTagAffinity.findMany({
    where: { userId },
  });
  const affinityMap = new Map<number, number>();
  for (const a of affinities) {
    affinityMap.set(a.tagId, a.weight);
  }

  // 3. Stage 2: In-Memory Scoring & Ranking
  const scored = scoreAndRankCandidates(candidates, affinityMap);

  // 4. Diversity Guardrail
  const diverse = applyAntiFatigue(scored);

  // 5. Pagination
  const total = diverse.length;
  const startIndex = (page - 1) * limit;
  const paginated = diverse.slice(startIndex, startIndex + limit);

  return {
    posts: paginated.map((p) => ({
      id: p.id,
      title: p.title,
      content: p.content,
      word_count: p.wordCount,
      is_core_theme: p.isCoreTheme,
      likes_count: p.likesCount,
      saves_count: p.savesCount,
      shares_count: p.sharesCount,
      comments_count: p.commentsCount,
      created_at: p.createdAt.toISOString(),
      tags: p.tags.map((t) => ({
        id: t.id,
        name: t.name,
        is_creator_tag: t.is_creator_tag,
        is_community_tag: t.is_community_tag,
        endorsement_count: t.endorsement_count,
      })),
      primary_tag: p.primaryTag,
      score: +p.finalScore.toFixed(3),
    })),
    pagination: {
      page,
      limit,
      total,
      has_more: startIndex + limit < total,
    },
  };
}
