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
      setError(err instanceof Error ? err.message : "찬양을 불러올 수 없습니다.");
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
      normalizedTitle !== hymn.title
      || (normalizedNumber || null) !== hymn.number
      || (normalizedTags || null) !== hymn.tags
      || enabled !== hymn.enabled
    );
  }, [enabled, hymn, normalizedNumber, normalizedTags, normalizedTitle]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    if (!normalizedTitle) {
      setError("제목을 입력해 주세요.");
      return;
    }
    if (normalizedTitle.length > TITLE_MAX_LENGTH) {
      setError(`제목은 최대 ${TITLE_MAX_LENGTH}자까지 입력할 수 있습니다.`);
      return;
    }
    if (normalizedNumber.length > NUMBER_MAX_LENGTH) {
      setError(`번호는 최대 ${NUMBER_MAX_LENGTH}자까지 입력할 수 있습니다.`);
      return;
    }
    if (!hasDirtyChanges) {
      setError("변경된 내용이 없습니다.");
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
      setError(err instanceof Error ? err.message : "수정에 실패했습니다.");
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
    if (!window.confirm(`"${hymn.title}" 찬양을 삭제하시겠습니까? 관련된 에셋, 메모, 상태, 이벤트가 모두 삭제됩니다.`)) return;
    setDeleting(true);
    setError(null);
    try {
      await deleteHymn(id);
      navigate("/hymns", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "삭제에 실패했습니다.");
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

  if (loading) return <p className="text-gray-500">로딩 중...</p>;
  if (!hymn && error) {
    return (
      <div className="space-y-3">
        <p className="text-red-600">{error}</p>
        <button
          type="button"
          onClick={() => void loadHymn()}
          className="rounded border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100"
        >
          다시 시도
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-2xl">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">찬양 수정</h1>
          {hymn && <p className="mt-1 text-xs text-slate-500">ID: {hymn.id}</p>}
        </div>
        <button
          type="button"
          onClick={() => void loadHymn()}
          className="rounded border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100"
        >
          데이터 새로고침
        </button>
      </div>

      <form onSubmit={handleSubmit} className="mb-8 flex flex-col gap-4 rounded-lg bg-white p-6 shadow">
        <div>
          <label htmlFor="title" className="mb-1 block text-sm font-medium text-gray-700">
            제목 *
          </label>
          <input
            id="title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={TITLE_MAX_LENGTH}
            className="w-full rounded border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <div className="mt-1 text-right text-xs text-slate-500">
            {normalizedTitle.length}/{TITLE_MAX_LENGTH}
          </div>
        </div>

        <div>
          <label htmlFor="number" className="mb-1 block text-sm font-medium text-gray-700">
            번호
          </label>
          <input
            id="number"
            type="text"
            value={number}
            onChange={(e) => setNumber(e.target.value)}
            maxLength={NUMBER_MAX_LENGTH}
            placeholder="예: 23, A-12"
            className="w-full rounded border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <div className="mt-1 text-xs text-slate-500">숫자/문자 모두 입력 가능합니다.</div>
        </div>

        <div>
          <label htmlFor="tags" className="mb-1 block text-sm font-medium text-gray-700">
            태그 (쉼표 구분)
          </label>
          <input
            id="tags"
            type="text"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            className="w-full rounded border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <div className="flex items-center gap-2">
          <input
            id="enabled"
            type="checkbox"
            checked={enabled}
            onChange={(e) => setEnabled(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
          />
          <label htmlFor="enabled" className="text-sm text-gray-700">
            활성화
          </label>
        </div>

        {!hasDirtyChanges && <p className="text-xs text-slate-500">변경한 항목이 없으면 저장되지 않습니다.</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={saving || !normalizedTitle || !hasDirtyChanges}
            className="rounded bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {saving ? "저장 중..." : "저장"}
          </button>
          <button
            type="button"
            onClick={() => navigate("/hymns")}
            className="rounded border border-gray-300 px-4 py-2 hover:bg-gray-50"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleDeleteHymn}
            disabled={deleting}
            className="ml-auto rounded bg-red-600 px-4 py-2 text-white hover:bg-red-700 disabled:opacity-50"
          >
            {deleting ? "삭제 중..." : "삭제"}
          </button>
        </div>
      </form>

      {hymn && (
        <>
          {hymn.assets.length > 0 && (
            <div className="mb-6 rounded-lg bg-white p-6 shadow">
              <h2 className="mb-3 text-lg font-semibold">등록된 에셋</h2>
              {assetActionError && <div className="mb-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{assetActionError}</div>}
              <div className="overflow-x-auto">
                <table className="min-w-[640px] w-full text-left text-sm">
                  <thead className="border-b">
                    <tr>
                      <th className="pb-2 font-medium text-gray-600">타입</th>
                      <th className="pb-2 font-medium text-gray-600">파트</th>
                      <th className="pb-2 font-medium text-gray-600">ObjectKey</th>
                      <th className="pb-2 font-medium text-gray-600">URL</th>
                      <th className="pb-2 font-medium text-gray-600"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {hymn.assets.map((asset) => (
                      <tr key={asset.id} className="border-b last:border-b-0">
                        <td className="py-2">{asset.type}</td>
                        <td className="py-2">{asset.part}</td>
                        <td className="max-w-xs truncate py-2 text-gray-500">{asset.objectKey}</td>
                        <td className="py-2">
                          {/^https?:\/\//i.test(asset.url) ? (
                            <a
                              href={asset.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-indigo-600 hover:underline"
                            >
                              열기
                            </a>
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>
                        <td className="py-2">
                          <button
                            type="button"
                            onClick={() => void handleDeleteAsset(asset.id)}
                            disabled={deletingAssetId === asset.id}
                            className="text-sm font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                          >
                            {deletingAssetId === asset.id ? "삭제 중..." : "삭제"}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-3 text-lg font-semibold">에셋 업로드</h2>
            <AdminAssetUploadPage hymnId={id} onConfirmed={handleAssetUploaded} />
          </div>
        </>
      )}
    </div>
  );
}
