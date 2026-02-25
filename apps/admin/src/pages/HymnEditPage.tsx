import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { deleteAsset } from "../api/adminAssets";
import { deleteHymn, getHymnDetail, updateHymn, type HymnDetailResponse } from "../api/hymns";
import AdminAssetUploadPage from "./AdminAssetUploadPage";

const TITLE_MAX_LENGTH = 120;
const NUMBER_MAX_LENGTH = 20;

function normalizeOptional(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

export default function HymnEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [hymn, setHymn] = useState<HymnDetailResponse | null>(null);
  const [title, setTitle] = useState("");
  const [number, setNumber] = useState("");
  const [tags, setTags] = useState("");
  const [enabled, setEnabled] = useState(true);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [assetActionError, setAssetActionError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deletingAssetId, setDeletingAssetId] = useState<string | null>(null);

  const loadHymn = useCallback(async () => {
    if (!id) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await getHymnDetail(id);
      setHymn(data);
      setTitle(data.title);
      setNumber(data.number ?? "");
      setTags(data.tags ?? "");
      setEnabled(data.enabled);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load hymn details.");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void loadHymn();
  }, [loadHymn]);

  const normalizedTitle = title.trim();
  const normalizedNumber = number.trim();
  const normalizedTags = tags.trim();

  const hasDirtyChanges = useMemo(() => {
    if (!hymn) return false;
    return (
      normalizedTitle !== hymn.title ||
      (normalizedNumber || null) !== hymn.number ||
      (normalizedTags || null) !== hymn.tags ||
      enabled !== hymn.enabled
    );
  }, [enabled, hymn, normalizedNumber, normalizedTags, normalizedTitle]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!id) return;

    if (!normalizedTitle) {
      setError("Title is required.");
      return;
    }
    if (normalizedTitle.length > TITLE_MAX_LENGTH) {
      setError(`Title must be ${TITLE_MAX_LENGTH} characters or fewer.`);
      return;
    }
    if (normalizedNumber.length > NUMBER_MAX_LENGTH) {
      setError(`Number must be ${NUMBER_MAX_LENGTH} characters or fewer.`);
      return;
    }
    if (!hasDirtyChanges) {
      setError("No changes to save.");
      return;
    }

    setError(null);
    setSaving(true);
    try {
      await updateHymn(id, {
        title: normalizedTitle,
        number: normalizedNumber || null,
        tags: normalizeOptional(tags),
        enabled,
      });
      navigate("/hymns", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update hymn.");
    } finally {
      setSaving(false);
    }
  };

  const handleAssetUploaded = useCallback(async () => {
    if (!id) return;
    try {
      const data = await getHymnDetail(id);
      setHymn(data);
      setAssetActionError(null);
    } catch (err) {
      setAssetActionError(err instanceof Error ? err.message : "Failed to refresh asset list.");
    }
  }, [id]);

  const handleDeleteHymn = async () => {
    if (!id || !hymn) return;
    if (!window.confirm(`Delete \"${hymn.title}\"? This removes linked assets and metadata.`)) return;
    setDeleting(true);
    setError(null);
    try {
      await deleteHymn(id);
      navigate("/hymns", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete hymn.");
    } finally {
      setDeleting(false);
    }
  };

  const handleDeleteAsset = async (assetId: string) => {
    if (!window.confirm("Delete this asset?")) return;
    setDeletingAssetId(assetId);
    setAssetActionError(null);
    try {
      await deleteAsset(assetId);
      await handleAssetUploaded();
    } catch (err) {
      setAssetActionError(err instanceof Error ? err.message : "Failed to delete asset.");
    } finally {
      setDeletingAssetId(null);
    }
  };

  if (loading) return <p className="text-sm text-slate-500">Loading hymn details...</p>;

  if (!hymn && error) {
    return (
      <div className="space-y-3">
        <div className="soy-alert soy-alert-error">{error}</div>
        <button type="button" onClick={() => void loadHymn()} className="soy-btn soy-btn-secondary">
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <section className="soy-panel max-w-4xl">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">Catalog</p>
            <h1 className="soy-title">Edit Hymn</h1>
            {hymn && <p className="soy-description font-mono">ID: {hymn.id}</p>}
          </div>
          <button type="button" onClick={() => void loadHymn()} className="soy-btn soy-btn-secondary">
            Refresh
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <label className="md:col-span-2">
            <span className="soy-label">Title *</span>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              maxLength={TITLE_MAX_LENGTH}
              className="soy-input"
            />
            <div className="mt-1 text-right text-xs text-slate-500">
              {normalizedTitle.length}/{TITLE_MAX_LENGTH}
            </div>
          </label>

          <label>
            <span className="soy-label">Number</span>
            <input
              id="number"
              type="text"
              value={number}
              onChange={(event) => setNumber(event.target.value)}
              maxLength={NUMBER_MAX_LENGTH}
              placeholder="e.g. 23, A-12"
              className="soy-input"
            />
          </label>

          <label>
            <span className="soy-label">Tags (comma separated)</span>
            <input
              id="tags"
              type="text"
              value={tags}
              onChange={(event) => setTags(event.target.value)}
              className="soy-input"
            />
          </label>

          <label className="md:col-span-2 flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
            <input
              id="enabled"
              type="checkbox"
              checked={enabled}
              onChange={(event) => setEnabled(event.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
            />
            Enabled in app
          </label>

          {!hasDirtyChanges && <p className="md:col-span-2 text-xs text-slate-500">No local changes yet.</p>}
          {error && <div className="md:col-span-2 soy-alert soy-alert-error">{error}</div>}

          <div className="md:col-span-2 flex flex-wrap gap-2">
            <button type="submit" disabled={saving || !normalizedTitle || !hasDirtyChanges} className="soy-btn soy-btn-primary">
              {saving ? "Saving..." : "Save Changes"}
            </button>
            <button type="button" onClick={() => navigate("/hymns")} className="soy-btn soy-btn-secondary">
              Cancel
            </button>
            <button type="button" onClick={handleDeleteHymn} disabled={deleting} className="soy-btn soy-btn-danger ml-auto">
              {deleting ? "Deleting..." : "Delete Hymn"}
            </button>
          </div>
        </form>
      </section>

      {hymn && (
        <>
          {hymn.assets.length > 0 && (
            <section className="soy-panel">
              <h2 className="soy-title">Registered Assets</h2>
              {assetActionError && <div className="mt-3 soy-alert soy-alert-error">{assetActionError}</div>}
              <div className="soy-table-wrap mt-3">
                <table className="soy-table min-w-[760px]">
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Part</th>
                      <th>Object Key</th>
                      <th>URL</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {hymn.assets.map((asset) => (
                      <tr key={asset.id}>
                        <td>{asset.type}</td>
                        <td>{asset.part}</td>
                        <td className="max-w-xs truncate text-slate-500" title={asset.objectKey}>
                          {asset.objectKey}
                        </td>
                        <td>
                          {/^https?:\/\//i.test(asset.url) ? (
                            <a href={asset.url} target="_blank" rel="noopener noreferrer" className="font-semibold text-indigo-700 hover:underline">
                              Open
                            </a>
                          ) : (
                            <span className="text-slate-400">-</span>
                          )}
                        </td>
                        <td>
                          <button
                            type="button"
                            onClick={() => void handleDeleteAsset(asset.id)}
                            disabled={deletingAssetId === asset.id}
                            className="soy-btn soy-btn-ghost text-red-600 hover:text-red-700"
                          >
                            {deletingAssetId === asset.id ? "Deleting..." : "Delete"}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          <section className="soy-panel">
            <h2 className="soy-title">Upload Asset</h2>
            <p className="soy-description">Presign, upload, then confirm to register.</p>
            <div className="mt-3">
              <AdminAssetUploadPage hymnId={id} onConfirmed={handleAssetUploaded} />
            </div>
          </section>
        </>
      )}
    </div>
  );
}
