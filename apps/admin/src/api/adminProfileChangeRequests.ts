import { apiGet, apiPatch } from "./client";

export type ProfileChangeRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface ProfileChangeRequestResponse {
  id: string;
  userId: string;
  userDisplayName: string | null;
  churchName: string;
  name: string;
  group: string;
  gender: string;
  status: ProfileChangeRequestStatus;
  requestedAt: string;
  reviewedBy: string | null;
  reviewedAt: string | null;
  rejectReason: string | null;
}

export interface ReviewProfileChangeRequest {
  action: "APPROVE" | "REJECT";
  rejectReason?: string;
}

export function listProfileChangeRequests(
  status: ProfileChangeRequestStatus = "PENDING",
): Promise<ProfileChangeRequestResponse[]> {
  return apiGet<ProfileChangeRequestResponse[]>(
    `/admin/profile-change-requests?status=${status}`,
  );
}

export function reviewProfileChangeRequest(
  id: string,
  request: ReviewProfileChangeRequest,
): Promise<ProfileChangeRequestResponse> {
  return apiPatch<ProfileChangeRequestResponse>(
    `/admin/profile-change-requests/${id}`,
    request,
  );
}
