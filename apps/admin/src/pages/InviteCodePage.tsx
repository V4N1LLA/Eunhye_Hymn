import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createInviteCode,
  listInviteCodes,
  revokeInviteCode,
  type InviteCodeResponse,
} from "../api/adminInviteCodes";
import InsightCard from "../components/InsightCard";

type StatusFilter = "all" | "active" | "inactive" | "expired" | "exhausted";

const CODE_MAX_LENGTH = 40;

function isExpired(code: InviteCodeResponse): boolean {
  return code.expiresAt != null && new Date(code.expiresAt).getTime() < Date.now();
}

function isExhausted(code: InviteCodeResponse): boolean {
  return code.maxUses != null && code.usedCount >= code.maxUses;
}

function statusLabel(code: InviteCodeResponse): "Active" | "Inactive" | "Expired" | "Exhausted" {
  if (!code.enabled) return "Inactive";
  if (isExpired(code)) return "Expired";
  if (isExhausted(code)) return "Exhausted";
  return "Active";
}

function statusBadgeClass(code: InviteCodeResponse): string {
  if (!code.enabled) return "bg-slate-100 text-slate-600";
  if (isExpired(code)) return "bg-amber-100 text-amber-700";
  if (isExhausted(code)) return "bg-orange-100 text-orange-700";
  return "bg-emerald-100 text-emerald-700";
}

async function copyTextToClipboard(text: string): Promise<void> {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "");
  textarea.style.position = "absolute";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  document.body.removeChild(textarea);

  if (!copied) {
    throw new Error("copy_failed");
  }
}

export default function InviteCodePage() {
  const [codes, setCodes] = useState<InviteCodeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [mutationSuccess, setMutationSuccess] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");

  const [showForm, setShowForm] = useState(false);
  const [newCode, setNewCode] = useState("");
  const [description, setDescription] = useState("");
  const [maxUses, setMaxUses] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [creating, setCreating] = useState(false);

  const [revokingCodes, setRevokingCodes] = useState<Set<string>>(new Set());

  const loadCodes = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await listInviteCodes();
      setCodes(
        [...data].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
      );
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load invite codes.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCodes();
  }, [loadCodes]);

  const clearFeedback = () => {
    setMutationError(null);
    setMutationSuccess(null);
  };

  const resetForm = () => {
    setNewCode("");
    setDescription("");
    setMaxUses("");
    setExpiresAt("");
  };

  const handleToggleForm = () => {
    if (showForm) {
      resetForm();
    }
    setShowForm((prev) => !prev);
  };

  const hasActiveFilter = search.trim().length > 0 || statusFilter !== "all";

  const filteredCodes = useMemo(() => {
    const query = search.trim().toLowerCase();
    return codes.filter((code) => {
      const label = statusLabel(code);
      if (statusFilter === "active" && label !== "Active") return false;
      if (statusFilter === "inactive" && label !== "Inactive") return false;
      if (statusFilter === "expired" && label !== "Expired") return false;
      if (statusFilter === "exhausted" && label !== "Exhausted") return false;

      if (!query) return true;
      return code.code.toLowerCase().includes(query) || (code.description ?? "").toLowerCase().includes(query);
    });
  }, [codes, search, statusFilter]);

  const stats = useMemo(() => {
    let active = 0;
    let inactive = 0;
    let expired = 0;
    let exhausted = 0;

    for (const code of codes) {
      const label = statusLabel(code);
      if (label === "Active") active += 1;
      else if (label === "Inactive") inactive += 1;
      else if (label === "Expired") expired += 1;
      else exhausted += 1;
    }

    return {
      total: codes.length,
      active,
      inactive,
      expired,
      exhausted,
    };
  }, [codes]);

  const clearFilters = () => {
    setSearch("");
    setStatusFilter("all");
  };

  const handleCopyCode = async (code: string) => {
    clearFeedback();
    try {
      await copyTextToClipboard(code);
      setCopiedCode(code);
      setMutationSuccess(`Copied \"${code}\" to clipboard.`);
      window.setTimeout(() => setCopiedCode((prev) => (prev === code ? null : prev)), 1500);
    } catch {
      setMutationError("Clipboard copy failed. Please copy manually.");
    }
  };

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    const normalizedCode = newCode.trim().toUpperCase();

    if (!normalizedCode) {
      setMutationError("Code is required.");
      setMutationSuccess(null);
      return;
    }
    if (normalizedCode.length > CODE_MAX_LENGTH) {
      setMutationError(`Code must be ${CODE_MAX_LENGTH} characters or fewer.`);
      setMutationSuccess(null);
      return;
    }
    if (/\s/.test(normalizedCode)) {
      setMutationError("Code cannot contain spaces.");
      setMutationSuccess(null);
      return;
    }

    const parsedMaxUses = maxUses ? parseInt(maxUses, 10) : undefined;
    if (parsedMaxUses !== undefined && (Number.isNaN(parsedMaxUses) || parsedMaxUses < 1)) {
      setMutationError("Max uses must be at least 1.");
      setMutationSuccess(null);
      return;
    }

    if (expiresAt) {
      const expiresAtMillis = new Date(expiresAt).getTime();
      if (Number.isNaN(expiresAtMillis) || expiresAtMillis <= Date.now()) {
        setMutationError("Expiration must be in the future.");
        setMutationSuccess(null);
        return;
      }
    }

    setCreating(true);
    clearFeedback();
    try {
      const created = await createInviteCode({
        code: normalizedCode,
        description: description.trim() || undefined,
        maxUses: parsedMaxUses,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      });
      setCodes((prev) => [created, ...prev]);
      setMutationSuccess(`Created invite code \"${created.code}\".`);
      resetForm();
      setShowForm(false);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "Failed to create invite code.");
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (code: string) => {
    if (!window.confirm(`Deactivate code \"${code}\"?`)) return;

    setRevokingCodes((prev) => new Set(prev).add(code));
    clearFeedback();
    try {
      await revokeInviteCode(code);
      setCodes((prev) => prev.map((item) => (item.code === code ? { ...item, enabled: false } : item)));
      setMutationSuccess(`Code \"${code}\" is now inactive.`);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "Failed to revoke invite code.");
    } finally {
      setRevokingCodes((prev) => {
        const next = new Set(prev);
        next.delete(code);
        return next;
      });
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">Onboarding</p>
            <h2 className="soy-title">Invite Code Management</h2>
            <p className="soy-description">Create, track, and revoke invite codes used for sign-up.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => void loadCodes()} disabled={loading} className="soy-btn soy-btn-secondary">
              {loading ? "Loading..." : "Reload"}
            </button>
            <button type="button" onClick={handleToggleForm} className="soy-btn soy-btn-primary">
              {showForm ? "Close Form" : "Create Code"}
            </button>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <InsightCard title="Total" value={stats.total} tone="slate" badge="ALL" description="All invite codes" loading={loading} />
          <InsightCard
            title="Active"
            value={stats.active}
            tone="emerald"
            badge="ON"
            description="Can be used now"
            ratio={stats.total > 0 ? stats.active / stats.total : 0}
            loading={loading}
          />
          <InsightCard
            title="Inactive"
            value={stats.inactive}
            tone="indigo"
            badge="OFF"
            description="Disabled manually"
            ratio={stats.total > 0 ? stats.inactive / stats.total : 0}
            loading={loading}
          />
          <InsightCard
            title="Expired"
            value={stats.expired}
            tone="amber"
            badge="EXP"
            description="Past expiration"
            ratio={stats.total > 0 ? stats.expired / stats.total : 0}
            loading={loading}
          />
          <InsightCard
            title="Exhausted"
            value={stats.exhausted}
            tone="rose"
            badge="MAX"
            description="Reached max uses"
            ratio={stats.total > 0 ? stats.exhausted / stats.total : 0}
            loading={loading}
          />
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-[1fr_220px_auto]">
          <input
            type="text"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search code or description"
            className="soy-input"
          />
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
            className="soy-select"
          >
            <option value="all">All status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
            <option value="expired">Expired</option>
            <option value="exhausted">Exhausted</option>
          </select>
          {hasActiveFilter && (
            <button type="button" onClick={clearFilters} className="soy-btn soy-btn-secondary">
              Clear Filters
            </button>
          )}
        </div>
      </section>

      {mutationSuccess && <div className="soy-alert soy-alert-success">{mutationSuccess}</div>}
      {mutationError && <div className="soy-alert soy-alert-error">{mutationError}</div>}

      {showForm && (
        <form onSubmit={handleCreate} className="soy-panel space-y-3">
          <div>
            <label className="soy-label">Code *</label>
            <input
              type="text"
              value={newCode}
              onChange={(event) => setNewCode(event.target.value.toUpperCase())}
              required
              maxLength={CODE_MAX_LENGTH}
              className="soy-input"
              placeholder="EUNHYE-2026"
            />
            <div className="mt-1 text-xs text-slate-500">
              No spaces ({newCode.trim().length}/{CODE_MAX_LENGTH})
            </div>
          </div>

          <div>
            <label className="soy-label">Description</label>
            <input
              type="text"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className="soy-input"
              placeholder="2026 special campaign"
            />
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="soy-label">Max Uses</label>
              <input
                type="number"
                value={maxUses}
                onChange={(event) => setMaxUses(event.target.value)}
                min="1"
                className="soy-input"
                placeholder="Blank for unlimited"
              />
            </div>
            <div>
              <label className="soy-label">Expiration</label>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(event) => setExpiresAt(event.target.value)}
                className="soy-input"
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button type="submit" disabled={creating || !newCode.trim()} className="soy-btn soy-btn-primary">
              {creating ? "Creating..." : "Create"}
            </button>
            <button
              type="button"
              onClick={() => {
                resetForm();
                setShowForm(false);
              }}
              className="soy-btn soy-btn-secondary"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {loading && <p className="text-sm text-slate-500">Loading invite codes...</p>}
      {loadError && (
        <div className="soy-alert soy-alert-error flex flex-wrap items-center justify-between gap-2">
          <span>{loadError}</span>
          <button type="button" onClick={() => void loadCodes()} className="soy-btn soy-btn-secondary">
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && (
        <section className="soy-panel p-0">
          <div className="soy-table-wrap">
            <table className="soy-table min-w-[920px]">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Description</th>
                  <th>Usage</th>
                  <th>Status</th>
                  <th>Expires At</th>
                  <th>Created At</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredCodes.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <div className="soy-empty m-3">
                        {hasActiveFilter ? "No codes match the current filters." : "No invite codes yet."}
                      </div>
                    </td>
                  </tr>
                )}
                {filteredCodes.map((code) => (
                  <tr key={code.code}>
                    <td>
                      <div className="font-mono text-sm">{code.code}</div>
                      {copiedCode === code.code && <div className="mt-1 text-xs text-emerald-600">Copied</div>}
                    </td>
                    <td>{code.description ?? "-"}</td>
                    <td>
                      {code.usedCount}
                      {code.maxUses != null ? ` / ${code.maxUses}` : " / unlimited"}
                    </td>
                    <td>
                      <span className={`soy-pill ${statusBadgeClass(code)}`}>{statusLabel(code)}</span>
                    </td>
                    <td>
                      {code.expiresAt ? (
                        <span className={isExpired(code) ? "text-amber-700" : "text-slate-500"}>
                          {new Date(code.expiresAt).toLocaleString("ko-KR")}
                        </span>
                      ) : (
                        <span className="text-slate-400">-</span>
                      )}
                    </td>
                    <td>{new Date(code.createdAt).toLocaleString("ko-KR")}</td>
                    <td>
                      <div className="flex items-center gap-1">
                        <button type="button" onClick={() => void handleCopyCode(code.code)} className="soy-btn soy-btn-ghost text-indigo-700">
                          Copy
                        </button>
                        {code.enabled && (
                          <button
                            type="button"
                            onClick={() => void handleRevoke(code.code)}
                            disabled={revokingCodes.has(code.code)}
                            className="soy-btn soy-btn-ghost text-red-600"
                          >
                            {revokingCodes.has(code.code) ? "..." : "Deactivate"}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && !loadError && (
        <p className="text-sm text-slate-500">
          Showing {filteredCodes.length.toLocaleString("ko-KR")} of {codes.length.toLocaleString("ko-KR")} codes.
        </p>
      )}
    </div>
  );
}
