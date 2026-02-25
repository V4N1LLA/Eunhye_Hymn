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
      return "Approved";
    case "REJECTED":
      return "Rejected";
    default:
      return "Pending";
  }
}

function genderLabel(gender: string | null | undefined): string {
  switch ((gender ?? "").trim().toUpperCase()) {
    case "MALE":
      return "Male";
    case "FEMALE":
      return "Female";
    default:
      return "Unknown";
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
      setError(err instanceof Error ? err.message : "Failed to load profile change requests.");
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
    try {
      await reviewProfileChangeRequest(item.id, { action: "APPROVE" });
      setNotice("Request approved.");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to approve request.");
    } finally {
      setBusyId(null);
    }
  };

  const handleReject = async (item: ProfileChangeRequestResponse) => {
    if (busyId) return;
    const reason = window.prompt("Enter reject reason (optional)", "");
    if (reason === null) return;

    setBusyId(item.id);
    setNotice(null);
    try {
      await reviewProfileChangeRequest(item.id, {
        action: "REJECT",
        rejectReason: reason.trim() || undefined,
      });
      setNotice("Request rejected.");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to reject request.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">Review Queue</p>
            <h2 className="soy-title">Profile Change Requests</h2>
            <p className="soy-description">Approve or reject requested profile updates from users.</p>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as ProfileChangeRequestStatus)}
              className="soy-select w-[180px]"
            >
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
            <button type="button" onClick={() => void load()} disabled={loading} className="soy-btn soy-btn-secondary">
              Reload
            </button>
          </div>
        </div>

        <div className="mt-3 rounded-lg border border-indigo-100 bg-indigo-50 px-3 py-2 text-sm text-indigo-800">
          Current list: {items.length.toLocaleString("ko-KR")} | Pending: {pendingCount.toLocaleString("ko-KR")}
        </div>
      </section>

      {notice && <div className="soy-alert soy-alert-success">{notice}</div>}
      {error && <div className="soy-alert soy-alert-error">{error}</div>}

      <section className="soy-panel p-0">
        <div className="soy-table-wrap">
          <table className="soy-table min-w-[1080px]">
            <thead>
              <tr>
                <th>User</th>
                <th>Requested Profile</th>
                <th>Status</th>
                <th>Requested / Reviewed</th>
                <th>Review</th>
              </tr>
            </thead>
            <tbody>
              {!loading && items.length === 0 && (
                <tr>
                  <td colSpan={5}>
                    <div className="soy-empty m-3">No requests for this status.</div>
                  </td>
                </tr>
              )}

              {items.map((item) => {
                const isBusy = busyId === item.id;
                return (
                  <tr key={item.id}>
                    <td>
                      <div className="font-semibold text-slate-900">{item.userDisplayName ?? "(No display name)"}</div>
                      <div className="mt-1 font-mono text-xs text-slate-500">{item.userId}</div>
                    </td>
                    <td>
                      <div>Church: {item.churchName}</div>
                      <div>Name: {item.name}</div>
                      <div>Group: {item.group}</div>
                      <div>Gender: {genderLabel(item.gender)}</div>
                      {item.rejectReason && <div className="mt-1 text-xs text-red-600">Reject reason: {item.rejectReason}</div>}
                    </td>
                    <td>
                      <span className={`soy-pill ${statusClass(item.status)}`}>{statusLabel(item.status)}</span>
                    </td>
                    <td className="text-xs text-slate-600">
                      <div>Requested: {formatDateTime(item.requestedAt)}</div>
                      <div>Reviewed: {formatDateTime(item.reviewedAt)}</div>
                    </td>
                    <td>
                      {item.status === "PENDING" ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => void handleApprove(item)}
                            disabled={isBusy}
                            className="soy-btn bg-emerald-600 text-white hover:bg-emerald-700"
                          >
                            Approve
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleReject(item)}
                            disabled={isBusy}
                            className="soy-btn soy-btn-secondary text-red-700"
                          >
                            Reject
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-slate-500">Completed</span>
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
