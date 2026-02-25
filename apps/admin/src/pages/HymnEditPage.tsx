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
      setError(err instanceof Error ? err.message : "찬양 상세를 불러오지 못했습니다.");
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
      setError("제목을 입력해 주세요.");
      return;
    }
    if (normalizedTitle.length > TITLE_MAX_LENGTH) {
      setError(`제목은 ${TITLE_MAX_LENGTH}자 이하로 입력해 주세요.`);
      return;
    }
    if (normalizedNumber.length > NUMBER_MAX_LENGTH) {
      setError(`번호는 ${NUMBER_MAX_LENGTH}자 이하로 입력해 주세요.`);
      return;
    }
    if (!hasDirtyChanges) {
      setError("변경된 항목이 없습니다.");
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
      setError(err instanceof Error ? err.message : "찬양 수정에 실패했습니다.");
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
      setAssetActionError(err instanceof Error ? err.message : "에셋 목록을 새로고침하지 못했습니다.");
    }
  }, [id]);

  const handleDeleteHymn = async () => {
    if (!id || !hymn) return;
    if (!window.confirm(`"${hymn.title}" 찬양을 삭제하시겠습니까? 연결된 에셋/메타데이터도 함께 삭제됩니다.`)) return;
    setDeleting(true);
    setError(null);
    try {
      await deleteHymn(id);
      navigate("/hymns", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "찬양 삭제에 실패했습니다.");
    } finally {
      setDeleting(false);
    }
  };

  const handleDeleteAsset = async (assetId: string) => {
    if (!window.confirm("이 에셋을 삭제하시겠습니까?")) return;
    setDeletingAssetId(assetId);
    setAssetActionError(null);
    try {
      await deleteAsset(assetId);
      await handleAssetUploaded();
    } catch (err) {
      setAssetActionError(err instanceof Error ? err.message : "에셋 삭제에 실패했습니다.");
    } finally {
      setDeletingAssetId(null);
    }
  };

  if (loading) return <p className="text-sm text-slate-500">찬양 상세를 불러오는 중입니다...</p>;

  if (!hymn && error) {
    return (
      <div className="space-y-3">
        <div className="soy-alert soy-alert-error">{error}</div>
        <button type="button" onClick={() => void loadHymn()} className="soy-btn soy-btn-secondary">
          다시 시도
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <section className="soy-panel max-w-4xl">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">카탈로그</p>
            <h1 className="soy-title">찬양 수정</h1>
            {hymn && <p className="soy-description font-mono">ID: {hymn.id}</p>}
          </div>
          <button type="button" onClick={() => void loadHymn()} className="soy-btn soy-btn-secondary">
            새로고침
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <label className="md:col-span-2">
            <span className="soy-label">제목 *</span>
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
            <span className="soy-label">번호</span>
            <input
              id="number"
              type="text"
              value={number}
              onChange={(event) => setNumber(event.target.value)}
              maxLength={NUMBER_MAX_LENGTH}
              placeholder="예: 23, A-12"
              className="soy-input"
            />
          </label>

          <label>
            <span className="soy-label">태그 (쉼표 구분)</span>
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
            앱에서 활성화
          </label>

          {!hasDirtyChanges && <p className="md:col-span-2 text-xs text-slate-500">변경된 항목이 없습니다.</p>}
          {error && <div className="md:col-span-2 soy-alert soy-alert-error">{error}</div>}

          <div className="md:col-span-2 flex flex-wrap gap-2">
            <button type="submit" disabled={saving || !normalizedTitle || !hasDirtyChanges} className="soy-btn soy-btn-primary">
              {saving ? "저장 중..." : "변경 저장"}
            </button>
            <button type="button" onClick={() => navigate("/hymns")} className="soy-btn soy-btn-secondary">
              취소
            </button>
            <button type="button" onClick={handleDeleteHymn} disabled={deleting} className="soy-btn soy-btn-danger ml-auto">
              {deleting ? "삭제 중..." : "찬양 삭제"}
            </button>
          </div>
        </form>
      </section>

      {hymn && (
        <>
          {hymn.assets.length > 0 && (
            <section className="soy-panel">
              <h2 className="soy-title">등록된 에셋</h2>
              {assetActionError && <div className="mt-3 soy-alert soy-alert-error">{assetActionError}</div>}
              <div className="soy-table-wrap mt-3">
                <table className="soy-table min-w-[760px]">
                  <thead>
                    <tr>
                      <th>타입</th>
                      <th>파트</th>
                      <th>Object Key</th>
                      <th>URL</th>
                      <th>관리</th>
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
                              열기
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
                            {deletingAssetId === asset.id ? "삭제 중..." : "삭제"}
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
            <h2 className="soy-title">에셋 업로드</h2>
            <p className="soy-description">Presign, Upload, Confirm 순서로 업로드를 완료합니다.</p>
            <div className="mt-3">
              <AdminAssetUploadPage hymnId={id} onConfirmed={handleAssetUploaded} />
            </div>
          </section>
        </>
      )}
    </div>
  );
}
