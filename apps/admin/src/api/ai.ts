import { apiPost } from "./client";

export interface RecommendHymnsRequest {
  situation: string;
  maxResults?: number;
}

export interface RecommendHymnItem {
  id: string;
  number: string | null;
  title: string;
  tags: string | null;
  reason: string;
}

export interface RecommendHymnsResponse {
  items: RecommendHymnItem[];
  requestedMaxResults: number;
  candidateCount: number;
}

export function recommendHymns(
  request: RecommendHymnsRequest,
): Promise<RecommendHymnsResponse> {
  return apiPost<RecommendHymnsResponse>("/ai/hymn-recommendations", request);
}
