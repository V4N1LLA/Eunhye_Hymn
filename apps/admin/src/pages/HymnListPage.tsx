import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { deleteHymn, listHymns, updateHymn, type HymnResponse } from "../api/hymns";
import InsightCard from "../components/InsightCard";

type EnabledFilter = "all" | "enabled" | "disabled";

export default function HymnListPage() {
  const [hymns, setHymns] = useState<HymnResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const [search, setSearch] = useState("");
  const [enabledFilter, setEnabledFilter] = useState<EnabledFilter>("all");
  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());

  const loadHymns = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listHymns();
      setHymns(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load hymns.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadHymns();
  }, [loadHymns]);

  const clearActionFeedback = () => {
    setActionError(null);
    setActionSuccess(null);
  };

  const filteredHymns = useMemo(() => {
    const query = search.trim().toLowerCase();
    return hymns.filter((hymn) => {
      if (enabledFilter === "enabled" && !hymn.enabled) return false;
      if (enabledFilter === "disabled" && hymn.enabled) return false;

      if (query) {
        const titleMatch = hymn.title.toLowerCase().includes(query);
        const numberMatch = hymn.number != null && hymn.number.toLowerCase().includes(query);
        const tagsMatch = hymn.tags != null && hymn.tags.toLowerCase().includes(query);
        if (!titleMatch && !numberMatch && !tagsMatch) return false;
      }

      return true;
    });
  }, [hymns, search, enabledFilter]);

  const enabledCount = useMemo(() => hymns.filter((item) => item.enabled).length, [hymns]);
  const disabledCount = hymns.length - enabledCount;
  const hasActiveFilter = search.trim().length > 0 || enabledFilter !== "all";

  const clearFilters = () => {
    setSearch("");
    setEnabledFilter("all");
  };

  const handleToggleEnabled = async (hymn: HymnResponse) => {
    clearActionFeedback();
    setTogglingIds((prev) => new Set(prev).add(hymn.id));
    try {
      const updated = await updateHymn(hymn.id, { enabled: !hymn.enabled });
      setHymns((prev) => prev.map((item) => (item.id === hymn.id ? { ...item, enabled: updated.enabled } : item)));
      setActionSuccess(`Updated \"${updated.title}\" to ${updated.enabled ? "enabled" : "disabled"}.`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to update hymn status.");
    } finally {
      setTogglingIds((prev) => {
        const next = new Set(prev);
        next.delete(hymn.id);
        return next;
      });
    }
  };

  const handleDelete = async (hymn: HymnResponse) => {
    if (!window.confirm(`Delete \"${hymn.title}\"? This removes linked assets and metadata.`)) return;
    clearActionFeedback();
    setDeletingIds((prev) => new Set(prev).add(hymn.id));
    try {
      await deleteHymn(hymn.id);
      setHymns((prev) => prev.filter((item) => item.id !== hymn.id));
      setActionSuccess(`Deleted \"${hymn.title}\".`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to delete hymn.");
    } finally {
      setDeletingIds((prev) => {
        const next = new Set(prev);
        next.delete(hymn.id);
        return next;
      });
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">Catalog</p>
            <h2 className="soy-title">Hymn Management</h2>
            <p className="soy-description">Search by title/number/tags and manage availability.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => void loadHymns()} disabled={loading} className="soy-btn soy-btn-secondary">
              {loading ? "Loading..." : "Reload"}
            </button>
            <Link to="/hymns/new" className="soy-btn soy-btn-primary">
              Add Hymn
            </Link>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <InsightCard title="Total Hymns" value={hymns.length} tone="indigo" badge="HM" description="Registered entries" loading={loading} />
          <InsightCard
            title="Enabled"
            value={enabledCount}
            tone="emerald"
            badge="LIVE"
            description="Visible to users"
            ratio={hymns.length > 0 ? enabledCount / hymns.length : 0}
            loading={loading}
          />
          <InsightCard
            title="Disabled"
            value={disabledCount}
            tone="amber"
            badge="OFF"
            description="Hidden from users"
            ratio={hymns.length > 0 ? disabledCount / hymns.length : 0}
            loading={loading}
          />
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-[1fr_220px_auto]">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search title, number, tags"
            className="soy-input"
          />
          <select value={enabledFilter} onChange={(e) => setEnabledFilter(e.target.value as EnabledFilter)} className="soy-select">
            <option value="all">All</option>
            <option value="enabled">Enabled</option>
            <option value="disabled">Disabled</option>
          </select>
          {hasActiveFilter && (
            <button type="button" onClick={clearFilters} className="soy-btn soy-btn-secondary">
              Clear Filters
            </button>
          )}
        </div>
      </section>

      {loading && <p className="text-sm text-slate-500">Loading hymns...</p>}
      {error && (
        <div className="soy-alert soy-alert-error flex flex-wrap items-center justify-between gap-2">
          <span>{error}</span>
          <button type="button" onClick={() => void loadHymns()} className="soy-btn soy-btn-secondary">
            Retry
          </button>
        </div>
      )}
      {actionSuccess && <div className="soy-alert soy-alert-success">{actionSuccess}</div>}
      {actionError && <div className="soy-alert soy-alert-error">{actionError}</div>}

      {!loading && !error && (
        <section className="soy-panel p-0">
          <div className="soy-table-wrap">
            <table className="soy-table min-w-[760px]">
              <thead>
                <tr>
                  <th>Number</th>
                  <th>Title</th>
                  <th>Tags</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredHymns.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="soy-empty m-3">
                        {hasActiveFilter ? "No hymns match the current filters." : "No hymns yet."}
                        {!hasActiveFilter && (
                          <div className="mt-3">
                            <Link to="/hymns/new" className="soy-btn soy-btn-secondary">
                              Create First Hymn
                            </Link>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
                {filteredHymns.map((hymn) => (
                  <tr key={hymn.id}>
                    <td>{hymn.number ?? "-"}</td>
                    <td>
                      <Link to={`/hymns/${hymn.id}/edit`} className="font-semibold text-indigo-700 hover:underline">
                        {hymn.title}
                      </Link>
                    </td>
                    <td>{hymn.tags ?? "-"}</td>
                    <td>
                      <button
                        type="button"
                        onClick={() => void handleToggleEnabled(hymn)}
                        disabled={togglingIds.has(hymn.id)}
                        className={`soy-pill ${hymn.enabled ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-600"}`}
                      >
                        {togglingIds.has(hymn.id) ? "Updating..." : hymn.enabled ? "Enabled" : "Disabled"}
                      </button>
                    </td>
                    <td>
                      <button
                        type="button"
                        onClick={() => void handleDelete(hymn)}
                        disabled={deletingIds.has(hymn.id)}
                        className="soy-btn soy-btn-ghost text-red-600 hover:text-red-700"
                      >
                        {deletingIds.has(hymn.id) ? "Deleting..." : "Delete"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && !error && (
        <p className="text-sm text-slate-500">
          Showing {filteredHymns.length.toLocaleString("ko-KR")} of {hymns.length.toLocaleString("ko-KR")} hymns.
        </p>
      )}
    </div>
  );
}
