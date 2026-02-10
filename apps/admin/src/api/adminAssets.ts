import { apiPost } from "./client";

export type AssetType = "PDF" | "AUDIO";
export type PartType = "S" | "A" | "T" | "B" | "ALL";

export interface PresignRequest {
  hymnId: string;
  type: AssetType;
  part?: PartType;
  filename: string;
  contentType: string;
}

export interface PresignResponse {
  uploadUrl: string;
  publicUrl: string;
  objectKey: string;
}

export interface ConfirmRequest {
  hymnId: string;
  type: AssetType;
  part?: PartType;
  publicUrl: string;
  objectKey: string;
  checksum?: string;
  version?: string;
}

export interface ConfirmResponse {
  assetId: string;
  hymnId: string;
  type: AssetType;
  part: PartType;
  url: string;
  objectKey: string;
}

export function presignAsset(request: PresignRequest): Promise<PresignResponse> {
  return apiPost<PresignResponse>("/admin/assets/presign", request);
}

export function confirmAsset(request: ConfirmRequest): Promise<ConfirmResponse> {
  return apiPost<ConfirmResponse>("/admin/assets/confirm", request);
}
