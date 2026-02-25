import { useCallback, useEffect, useMemo, useState } from "react";
import {
  listProfileChangeRequests,
  reviewProfileChangeRequest,
  type ProfileChangeRequestResponse,
  type ProfileChangeRequestStatus,
} from "../api/adminProfileChangeRequests";

function formatDateTime(value: string | null): string {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR", { hour12: false });
}

function statusLabel(status: ProfileChangeRequestStatus): string {
  switch (status) {
    case "APPROVED":
      return "승인됨";
    case "REJECTED":
      return "반려됨";
    default:
      return "대기중";
  }
}

function genderLabel(gender: string | null | undefined): string {
  switch ((gender ?? "").trim().toUpperCase()) {
    case "MALE":
      return "남성";
    case "FEMALE":
      return "여성";
    default:
      return "미상";
  }
}

function statusClass(status: ProfileChangeRequestStatus): string {
  if (status === "APPROVED") return "bg-emerald-100 text-emerald-700";
  if (status === "REJECTED") return "bg-red-100 text-red-700";
  return "bg-amber-100 text-amber-700";
}

export default function ProfileChangeRequestPage() {
  const [statusFilter, setStatusFilter] = useState<ProfileChangeRequestStatus>("PENDING");
  const [items, setItems] = useState<ProfileChangeRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listProfileChangeRequests(statusFilter);
      setItems(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "프로필 변경 요청 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  const pendingCount = useMemo(() => items.filter((item) => item.status === "PENDING").length, [items]);

  const handleApprove = async (item: ProfileChangeRequestResponse) => {
    if (busyId) return;
    setBusyId(item.id);
    setNotice(null);
    setError(null);
    try {
      await reviewProfileChangeRequest(item.id, { action: "APPROVE" });
      setNotice("요청을 승인했습니다.");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "요청 승인에 실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  const handleReject = async (item: ProfileChangeRequestResponse) => {
    if (busyId) return;
    const reason = window.prompt("반려 사유를 입력하세요. (선택)", "");
    if (reason === null) return;

    setBusyId(item.id);
    setNotice(null);
    setError(null);
    try {
      await reviewProfileChangeRequest(item.id, {
        action: "REJECT",
        rejectReason: reason.trim() || undefined,
      });
      setNotice("요청을 반려했습니다.");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "요청 반려에 실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-card p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-600">검수 큐</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-900">프로필 변경 요청</h2>
            <p className="mt-1 text-sm text-slate-500">사용자 프로필 변경 요청을 승인 또는 반려합니다.</p>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as ProfileChangeRequestStatus)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="PENDING">대기중</option>
              <option value="APPROVED">승인됨</option>
              <option value="REJECTED">반려됨</option>
            </select>
            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60"
            >
              새로고침
            </button>
          </div>
        </div>

        <div className="mt-3 rounded-lg border border-indigo-100 bg-indigo-50 px-3 py-2 text-sm text-indigo-800">
          현재 목록: {items.length.toLocaleString("ko-KR")}건 | 대기중: {pendingCount.toLocaleString("ko-KR")}건
        </div>
      </section>

      {notice && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{notice}</div>}
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}

      <section className="soy-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-[1080px] w-full text-left">
            <thead className="border-b border-slate-200 bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">사용자</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">요청 프로필</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">요청/검수 시각</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">검수</th>
              </tr>
            </thead>
            <tbody>
              {!loading && items.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-400">
                    선택한 상태의 요청이 없습니다.
                  </td>
                </tr>
              )}

              {items.map((item) => {
                const isBusy = busyId === item.id;
                return (
                  <tr key={item.id} className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="font-semibold text-slate-900">{item.userDisplayName ?? "(표시 이름 없음)"}</div>
                      <div className="mt-1 font-mono text-xs text-slate-500">{item.userId}</div>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-700">
                      <div>교회: {item.churchName}</div>
                      <div>이름: {item.name}</div>
                      <div>구역: {item.group}</div>
                      <div>성별: {genderLabel(item.gender)}</div>
                      {item.rejectReason && <div className="mt-1 text-xs text-red-600">반려 사유: {item.rejectReason}</div>}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${statusClass(item.status)}`}>
                        {statusLabel(item.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600">
                      <div>요청: {formatDateTime(item.requestedAt)}</div>
                      <div>검수: {formatDateTime(item.reviewedAt)}</div>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      {item.status === "PENDING" ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => void handleApprove(item)}
                            disabled={isBusy}
                            className="rounded-md bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-60"
                          >
                            {isBusy ? "처리 중..." : "승인"}
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleReject(item)}
                            disabled={isBusy}
                            className="rounded-md border border-red-200 bg-white px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-50 disabled:opacity-60"
                          >
                            {isBusy ? "처리 중..." : "반려"}
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-slate-500">처리 완료</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
