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

const API_BASE_URL = "/api/v1";

async function postJson<T>(path: string, payload: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  if (!response.ok || data?.success === false) {
    const message = data?.error?.message ?? "요청 처리 중 오류가 발생했습니다.";
    throw new Error(message);
  }
  return data.data as T;
}

export function presignAsset(request: PresignRequest): Promise<PresignResponse> {
  return postJson<PresignResponse>("/admin/assets/presign", request);
}

export function confirmAsset(request: ConfirmRequest): Promise<ConfirmResponse> {
  return postJson<ConfirmResponse>("/admin/assets/confirm", request);
}
