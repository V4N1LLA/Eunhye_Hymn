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
      return "승인 완료";
    case "REJECTED":
      return "반려";
    default:
      return "승인 대기";
  }
}

function genderLabel(gender: string | null | undefined): string {
  switch ((gender ?? "").trim().toUpperCase()) {
    case "MALE":
      return "남성";
    case "FEMALE":
      return "여성";
    default:
      return "미입력";
  }
}

export default function ProfileChangeRequestPage() {
  const [statusFilter, setStatusFilter] =
    useState<ProfileChangeRequestStatus>("PENDING");
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
      setError(
        err instanceof Error
          ? err.message
          : "요청 목록을 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  const pendingCount = useMemo(
    () => items.filter((item) => item.status === "PENDING").length,
    [items],
  );

  const handleApprove = async (item: ProfileChangeRequestResponse) => {
    if (busyId) return;
    setBusyId(item.id);
    setNotice(null);
    try {
      await reviewProfileChangeRequest(item.id, { action: "APPROVE" });
      setNotice("변경 요청을 승인했습니다.");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "승인 처리에 실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  const handleReject = async (item: ProfileChangeRequestResponse) => {
    if (busyId) return;
    const reason = window.prompt("반려 사유를 입력해 주세요. (선택)", "");
    if (reason === null) return;

    setBusyId(item.id);
    setNotice(null);
    try {
      await reviewProfileChangeRequest(item.id, {
        action: "REJECT",
        rejectReason: reason.trim() || undefined,
      });
      setNotice("변경 요청을 반려했습니다.");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "반려 처리에 실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-slate-900">
              개인정보 변경 요청
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              앱에서 들어온 개인정보 변경 요청을 승인/반려합니다.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={statusFilter}
              onChange={(e) =>
                setStatusFilter(e.target.value as ProfileChangeRequestStatus)
              }
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="PENDING">승인 대기</option>
              <option value="APPROVED">승인 완료</option>
              <option value="REJECTED">반려</option>
            </select>
            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50"
            >
              새로고침
            </button>
          </div>
        </div>

        <div className="mt-3 rounded-lg border border-indigo-100 bg-indigo-50 px-3 py-2 text-sm text-indigo-800">
          현재 목록: {items.length}건, 승인 대기: {pendingCount}건
        </div>
      </section>

      {notice && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {notice}
        </div>
      )}

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-[1100px] w-full text-left">
            <thead className="border-b border-slate-200 bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-slate-700">
                  사용자
                </th>
                <th className="px-4 py-3 text-sm font-semibold text-slate-700">
                  요청 정보
                </th>
                <th className="px-4 py-3 text-sm font-semibold text-slate-700">
                  상태
                </th>
                <th className="px-4 py-3 text-sm font-semibold text-slate-700">
                  요청/처리 시각
                </th>
                <th className="px-4 py-3 text-sm font-semibold text-slate-700">
                  처리
                </th>
              </tr>
            </thead>
            <tbody>
              {!loading && items.length === 0 && (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-10 text-center text-sm text-slate-500"
                  >
                    해당 상태의 요청이 없습니다.
                  </td>
                </tr>
              )}
              {items.map((item) => {
                const isBusy = busyId === item.id;
                return (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-4 py-3 align-top">
                      <div className="font-semibold text-slate-900">
                        {item.userDisplayName ?? "(이름 없음)"}
                      </div>
                      <div className="mt-1 font-mono text-xs text-slate-500">
                        {item.userId}
                      </div>
                    </td>
                    <td className="px-4 py-3 align-top text-sm text-slate-700">
                      <div>교회: {item.churchName}</div>
                      <div>이름: {item.name}</div>
                      <div>구역: {item.group}</div>
                      <div>성별: {genderLabel(item.gender)}</div>
                      {item.rejectReason && (
                        <div className="mt-1 text-xs text-red-600">
                          반려 사유: {item.rejectReason}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 align-top">
                      <span
                        className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                          item.status === "APPROVED"
                            ? "bg-emerald-100 text-emerald-700"
                            : item.status === "REJECTED"
                            ? "bg-red-100 text-red-700"
                            : "bg-amber-100 text-amber-700"
                        }`}
                      >
                        {statusLabel(item.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3 align-top text-xs text-slate-600">
                      <div>요청: {formatDateTime(item.requestedAt)}</div>
                      <div>처리: {formatDateTime(item.reviewedAt)}</div>
                    </td>
                    <td className="px-4 py-3 align-top">
                      {item.status === "PENDING" ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => void handleApprove(item)}
                            disabled={isBusy}
                            className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                          >
                            승인
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleReject(item)}
                            disabled={isBusy}
                            className="rounded-lg border border-red-300 px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-50 disabled:opacity-50"
                          >
                            반려
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
