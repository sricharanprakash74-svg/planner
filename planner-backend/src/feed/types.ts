export type InteractionType =
  | "view"
  | "like"
  | "comment"
  | "share"
  | "save"
  | "adopt_plan"
  | "dismiss";

export type TagSourceType = "CREATOR" | "COMMENTER";

export interface FeedInteractionPayload {
  userId: number;
  post_id: number;
  interaction_type: InteractionType;
  active_seconds?: number | undefined;
  sub_tags?: string[] | undefined;
}

export interface PostTagInfo {
  id: number;
  name: string;
  sourceType: TagSourceType;
  is_creator_tag: boolean;
  is_community_tag: boolean;
  endorsement_count: number;
}

export interface CandidatePost {
  id: number;
  title: string;
  content: string;
  wordCount: number;
  isCoreTheme: boolean;
  likesCount: number;
  savesCount: number;
  sharesCount: number;
  commentsCount: number;
  createdAt: Date;
  tags: PostTagInfo[];
  primaryTag: string | null;
}

export interface ScoredPost extends CandidatePost {
  tagMatchScore: number;
  utilityScore: number;
  coreMultiplier: number;
  decayScore: number;
  finalScore: number;
}

export interface FeedQueryOptions {
  userId: number;
  filterTag?: string | null | undefined;
  page?: number | undefined;
  limit?: number | undefined;
}

export interface FeedResponse {
  posts: Array<{
    id: number;
    title: string;
    content: string;
    word_count: number;
    is_core_theme: boolean;
    likes_count: number;
    saves_count: number;
    shares_count: number;
    comments_count: number;
    created_at: string;
    tags: Array<{
      id: number;
      name: string;
      is_creator_tag: boolean;
      is_community_tag: boolean;
      endorsement_count: number;
    }>;
    primary_tag: string | null;
    score: number;
  }>;
  pagination: {
    page: number;
    limit: number;
    total: number;
    has_more: boolean;
  };
}
