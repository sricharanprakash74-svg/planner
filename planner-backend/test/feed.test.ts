import { describe, it, before, after } from "node:test";
import assert from "node:assert/strict";
import { PrismaClient } from "@prisma/client";
import {
  calculateDeltaWeight,
  calculateExpectedReadSeconds,
  clampWeight,
  processInteraction,
} from "../src/feed/affinity.js";
import {
  applyAntiFatigue,
  generatePersonalizedFeed,
  scoreAndRankCandidates,
} from "../src/feed/engine.js";
import type { CandidatePost } from "../src/feed/types.js";

const prisma = new PrismaClient();

describe("Recommendation Feed & User Retention Algorithm Test Suite", () => {
  let userAId: number;
  let userBId: number;
  let tagStudyId: number;
  let tagWorkoutId: number;
  let tagRoutineId: number;
  let testPostIds: number[] = [];

  before(async () => {
    // Setup test users
    const userA = await prisma.user.create({
      data: { displayName: "User_Taste_Study" },
    });
    userAId = userA.id;

    const userB = await prisma.user.create({
      data: { displayName: "User_Taste_Workout" },
    });
    userBId = userB.id;

    // Setup tags
    const tagStudy = await prisma.tag.upsert({
      where: { name: "study-prep" },
      create: { name: "study-prep" },
      update: {},
    });
    tagStudyId = tagStudy.id;

    const tagWorkout = await prisma.tag.upsert({
      where: { name: "workout-routine" },
      create: { name: "workout-routine" },
      update: {},
    });
    tagWorkoutId = tagWorkout.id;

    const tagRoutine = await prisma.tag.upsert({
      where: { name: "morning-routine" },
      create: { name: "morning-routine" },
      update: {},
    });
    tagRoutineId = tagRoutine.id;
  });

  after(async () => {
    // Cleanup created test records
    if (testPostIds.length > 0) {
      await prisma.post.deleteMany({
        where: { id: { in: testPostIds } },
      });
    }
    await prisma.user.deleteMany({
      where: { id: { in: [userAId, userBId] } },
    });
    await prisma.$disconnect();
  });

  it("Requirement 1: Short dwell (<4s) triggers negative penalty, while deep read and save boost affinity", async () => {
    // 1. Math verification
    const expectedRead = calculateExpectedReadSeconds(35); // 35 / 3.5 = 10.0 seconds
    assert.equal(expectedRead, 10.0);

    // Short dwell (< 4.0s) -> Bounce
    const bounceDelta = calculateDeltaWeight("view", expectedRead, 2.5);
    assert.equal(bounceDelta, -1.5);

    // Deep Read (>= 70% of 10.0s = >= 7.0s)
    const deepReadDelta = calculateDeltaWeight("view", expectedRead, 8.0);
    assert.equal(deepReadDelta, 3.0);

    // Skim (30% to 70% of 10.0s = 3.0s to 6.9s, and >= 4s)
    const skimDelta = calculateDeltaWeight("view", expectedRead, 5.0);
    assert.equal(skimDelta, 1.0);

    // Save
    const saveDelta = calculateDeltaWeight("save", expectedRead);
    assert.equal(saveDelta, 5.0);

    // Clamping limits [-5.0, 30.0]
    assert.equal(clampWeight(35.0), 30.0);
    assert.equal(clampWeight(-10.0), -5.0);
    assert.equal(clampWeight(15.0), 15.0);

    // 2. Integration with Prisma interaction
    const testPost = await prisma.post.create({
      data: {
        title: "Deep Focus Routine",
        content: "A quick 35 word guide on maintaining deep focus throughout long morning sessions.",
        wordCount: 35,
        isCoreTheme: true,
      },
    });
    testPostIds.push(testPost.id);

    await prisma.postTag.create({
      data: {
        postId: testPost.id,
        tagId: tagStudyId,
        sourceType: "CREATOR",
      },
    });

    // Initial interaction: Bounce (< 4.0s)
    await processInteraction(prisma, {
      userId: userAId,
      post_id: testPost.id,
      interaction_type: "view",
      active_seconds: 2.0,
    });

    let affinity = await prisma.userTagAffinity.findUnique({
      where: { userId_tagId: { userId: userAId, tagId: tagStudyId } },
    });
    // Default weight 1.0 - 1.5 = -0.5
    assert.ok(affinity);
    assert.equal(affinity.weight, -0.5);

    // Follow-up: Deep Read (8.0s)
    await processInteraction(prisma, {
      userId: userAId,
      post_id: testPost.id,
      interaction_type: "view",
      active_seconds: 8.0,
    });

    affinity = await prisma.userTagAffinity.findUnique({
      where: { userId_tagId: { userId: userAId, tagId: tagStudyId } },
    });
    // -0.5 + 3.0 = 2.5
    assert.ok(affinity);
    assert.equal(affinity.weight, 2.5);

    // Save interaction (+5.0)
    await processInteraction(prisma, {
      userId: userAId,
      post_id: testPost.id,
      interaction_type: "save",
    });

    affinity = await prisma.userTagAffinity.findUnique({
      where: { userId_tagId: { userId: userAId, tagId: tagStudyId } },
    });
    // 2.5 + 5.0 = 7.5
    assert.ok(affinity);
    assert.equal(affinity.weight, 7.5);
  });

  it("Requirement 2: Two users with different affinity profiles receive distinctly tailored feed orders", async () => {
    // User A has high affinity for 'study-prep' (15.0)
    await prisma.userTagAffinity.upsert({
      where: { userId_tagId: { userId: userAId, tagId: tagStudyId } },
      create: { userId: userAId, tagId: tagStudyId, weight: 15.0 },
      update: { weight: 15.0 },
    });

    // User B has high affinity for 'workout-routine' (15.0)
    await prisma.userTagAffinity.upsert({
      where: { userId_tagId: { userId: userBId, tagId: tagWorkoutId } },
      create: { userId: userBId, tagId: tagWorkoutId, weight: 15.0 },
      update: { weight: 15.0 },
    });

    // Create a catalog post for Study
    const studyPost = await prisma.post.create({
      data: {
        title: "Calculus Exam Study Prep",
        content: "Detailed study guide for college calculus with flashcards and routine checklists.",
        wordCount: 80,
        isCoreTheme: false,
      },
    });
    testPostIds.push(studyPost.id);
    await prisma.postTag.create({
      data: { postId: studyPost.id, tagId: tagStudyId, sourceType: "CREATOR" },
    });

    // Create a catalog post for Workout
    const workoutPost = await prisma.post.create({
      data: {
        title: "Full Body Dumbbell Circuit",
        content: "Quick 20 minute high intensity dumbbell workout for strength and core stability.",
        wordCount: 80,
        isCoreTheme: false,
      },
    });
    testPostIds.push(workoutPost.id);
    await prisma.postTag.create({
      data: { postId: workoutPost.id, tagId: tagWorkoutId, sourceType: "CREATOR" },
    });

    // Fetch feed for User A
    const feedA = await generatePersonalizedFeed(prisma, { userId: userAId, limit: 10 });
    // Fetch feed for User B
    const feedB = await generatePersonalizedFeed(prisma, { userId: userBId, limit: 10 });

    const postIdsA = feedA.posts.map((p) => p.id);
    const postIdsB = feedB.posts.map((p) => p.id);

    // Feeds must be distinctly tailored
    assert.notDeepEqual(postIdsA, postIdsB, "User A and User B should receive different feed rankings");

    // Top post for User A must reflect User A's taste (study-prep)
    const topPostA = feedA.posts[0];
    assert.ok(topPostA);
    assert.ok(
      topPostA.tags.some((t) => t.name === "study-prep"),
      `Top post for User A should match study-prep tag, got: ${topPostA.primary_tag}`
    );

    // Top post for User B must reflect User B's taste (workout-routine)
    const topPostB = feedB.posts[0];
    assert.ok(topPostB);
    assert.ok(
      topPostB.tags.some((t) => t.name === "workout-routine"),
      `Top post for User B should match workout-routine tag, got: ${topPostB.primary_tag}`
    );

    // Verify personalized score differential for the same posts across users
    const scoreStudyInA = feedA.posts.find((p) => p.id === studyPost.id)!.score;
    const scoreWorkoutInA = feedA.posts.find((p) => p.id === workoutPost.id)!.score;
    assert.ok(
      scoreStudyInA > scoreWorkoutInA,
      `User A should score study post (${scoreStudyInA}) higher than workout post (${scoreWorkoutInA})`
    );

    const scoreStudyInB = feedB.posts.find((p) => p.id === studyPost.id)!.score;
    const scoreWorkoutInB = feedB.posts.find((p) => p.id === workoutPost.id)!.score;
    assert.ok(
      scoreWorkoutInB > scoreStudyInB,
      `User B should score workout post (${scoreWorkoutInB}) higher than study post (${scoreStudyInB})`
    );
  });

  it("Requirement 3: Posts flagged as is_core_theme = true receive 1.35x boost and maintain top visibility", () => {
    const now = new Date();

    const candidateCore: CandidatePost = {
      id: 101,
      title: "Essential Morning Planning Checklist",
      content: "Core routine setup.",
      wordCount: 50,
      isCoreTheme: true,
      likesCount: 15,
      savesCount: 10,
      sharesCount: 5,
      commentsCount: 2,
      createdAt: new Date(now.getTime() - 1000 * 3600 * 2), // 2 hours old
      tags: [],
      primaryTag: "planning",
    };

    const candidateViralOffTopic: CandidatePost = {
      id: 102,
      title: "Random Viral Story",
      content: "Off topic gossip.",
      wordCount: 50,
      isCoreTheme: false,
      likesCount: 30,
      savesCount: 10,
      sharesCount: 5,
      commentsCount: 2,
      createdAt: new Date(now.getTime() - 1000 * 3600 * 2), // 2 hours old
      tags: [],
      primaryTag: "stories",
    };

    const affinities = new Map<number, number>();
    const scored = scoreAndRankCandidates(
      [candidateCore, candidateViralOffTopic],
      affinities,
      now
    );

    // Confirm core post received 1.35 coreMultiplier
    const scoredCore = scored.find((s) => s.id === 101);
    const scoredViral = scored.find((s) => s.id === 102);

    assert.ok(scoredCore);
    assert.ok(scoredViral);
    assert.equal(scoredCore.coreMultiplier, 1.35);
    assert.equal(scoredViral.coreMultiplier, 1.0);

    // Because of the 1.35x core multiplier, core post final score outranks viral off-topic post
    assert.ok(
      scoredCore.finalScore > scoredViral.finalScore,
      `Core theme post (${scoredCore.finalScore}) should outrank off-topic viral post (${scoredViral.finalScore})`
    );
    assert.equal(scored[0]?.id, 101);
  });

  it("Requirement 4: Anti-fatigue diversity guardrail prevents consecutive identical primary tags", () => {
    const now = new Date();

    const post1: CandidatePost = {
      id: 201,
      title: "Study Guide Part 1",
      content: "Part 1",
      wordCount: 50,
      isCoreTheme: true,
      likesCount: 100,
      savesCount: 50,
      sharesCount: 10,
      commentsCount: 10,
      createdAt: now,
      tags: [{ id: 1, name: "study-prep", sourceType: "CREATOR", is_creator_tag: true, is_community_tag: false, endorsement_count: 5 }],
      primaryTag: "study-prep",
    };

    const post2: CandidatePost = {
      id: 202,
      title: "Study Guide Part 2",
      content: "Part 2",
      wordCount: 50,
      isCoreTheme: true,
      likesCount: 90,
      savesCount: 45,
      sharesCount: 8,
      commentsCount: 8,
      createdAt: now,
      tags: [{ id: 1, name: "study-prep", sourceType: "CREATOR", is_creator_tag: true, is_community_tag: false, endorsement_count: 5 }],
      primaryTag: "study-prep",
    };

    const post3: CandidatePost = {
      id: 203,
      title: "Study Guide Part 3",
      content: "Part 3",
      wordCount: 50,
      isCoreTheme: true,
      likesCount: 80,
      savesCount: 40,
      sharesCount: 7,
      commentsCount: 6,
      createdAt: now,
      tags: [{ id: 1, name: "study-prep", sourceType: "CREATOR", is_creator_tag: true, is_community_tag: false, endorsement_count: 5 }],
      primaryTag: "study-prep",
    };

    const post4: CandidatePost = {
      id: 204,
      title: "Evening Workout Plan",
      content: "Cardio",
      wordCount: 50,
      isCoreTheme: true,
      likesCount: 50,
      savesCount: 20,
      sharesCount: 5,
      commentsCount: 2,
      createdAt: now,
      tags: [{ id: 2, name: "workout-routine", sourceType: "CREATOR", is_creator_tag: true, is_community_tag: false, endorsement_count: 3 }],
      primaryTag: "workout-routine",
    };

    const post5: CandidatePost = {
      id: 205,
      title: "Morning Habit Stacking",
      content: "Habits",
      wordCount: 50,
      isCoreTheme: true,
      likesCount: 30,
      savesCount: 15,
      sharesCount: 3,
      commentsCount: 2,
      createdAt: now,
      tags: [{ id: 3, name: "morning-routine", sourceType: "CREATOR", is_creator_tag: true, is_community_tag: false, endorsement_count: 4 }],
      primaryTag: "morning-routine",
    };

    const affinities = new Map<number, number>();
    const scored = scoreAndRankCandidates(
      [post1, post2, post3, post4, post5],
      affinities,
      now
    );

    // Initial ranked list without anti-fatigue would be:
    // post1 (study-prep), post2 (study-prep), post3 (study-prep), post4 (workout-routine), post5 (morning-routine)
    assert.equal(scored[0]?.primaryTag, "study-prep");
    assert.equal(scored[1]?.primaryTag, "study-prep");
    assert.equal(scored[2]?.primaryTag, "study-prep");

    // Apply diversity anti-fatigue
    const diverse = applyAntiFatigue(scored);

    // Verify that NO two consecutive posts share the exact same primary tag
    for (let i = 1; i < diverse.length; i++) {
      const prev = diverse[i - 1]?.primaryTag;
      const curr = diverse[i]?.primaryTag;
      if (prev && curr) {
        assert.notEqual(
          prev,
          curr,
          `Consecutive posts at index ${i - 1} and ${i} share identical primary tag: ${curr}`
        );
      }
    }

    // Expected structure:
    // index 0: study-prep (post1)
    // index 1: workout-routine (post4 moved forward to break monotony)
    // index 2: study-prep (post2)
    // index 3: morning-routine (post5 moved forward to break monotony)
    // index 4: study-prep (post3)
    assert.equal(diverse[0]?.primaryTag, "study-prep");
    assert.equal(diverse[1]?.primaryTag, "workout-routine");
    assert.equal(diverse[2]?.primaryTag, "study-prep");
    assert.equal(diverse[3]?.primaryTag, "morning-routine");
    assert.equal(diverse[4]?.primaryTag, "study-prep");
  });

  it("Requirement 5: Commenter sub-tags upsert as COMMENTER, increment endorsements, and boost commenter affinity", async () => {
    const post = await prisma.post.create({
      data: {
        title: "Productivity Hacks",
        content: "Tips for staying productive throughout the day.",
        wordCount: 40,
        isCoreTheme: true,
      },
    });
    testPostIds.push(post.id);

    // Initial comment with sub-tag "time-blocking"
    await processInteraction(prisma, {
      userId: userAId,
      post_id: post.id,
      interaction_type: "comment",
      sub_tags: ["time-blocking"],
    });

    const tagTimeBlocking = await prisma.tag.findUnique({
      where: { name: "time-blocking" },
    });
    assert.ok(tagTimeBlocking);

    let postTag = await prisma.postTag.findUnique({
      where: {
        postId_tagId_sourceType: {
          postId: post.id,
          tagId: tagTimeBlocking.id,
          sourceType: "COMMENTER",
        },
      },
    });
    assert.ok(postTag);
    assert.equal(postTag.endorsementCount, 1);

    // Check commenter affinity boosted by +3.5 (from default 1.0 -> 4.5)
    let userAffinity = await prisma.userTagAffinity.findUnique({
      where: {
        userId_tagId: {
          userId: userAId,
          tagId: tagTimeBlocking.id,
        },
      },
    });
    assert.ok(userAffinity);
    assert.equal(userAffinity.weight, 4.5);

    // Another user reinforces the same tag with another comment
    await processInteraction(prisma, {
      userId: userBId,
      post_id: post.id,
      interaction_type: "comment",
      sub_tags: ["time-blocking"],
    });

    postTag = await prisma.postTag.findUnique({
      where: {
        postId_tagId_sourceType: {
          postId: post.id,
          tagId: tagTimeBlocking.id,
          sourceType: "COMMENTER",
        },
      },
    });
    assert.ok(postTag);
    // Endorsement count should increment to 2
    assert.equal(postTag.endorsementCount, 2);
  });
});

