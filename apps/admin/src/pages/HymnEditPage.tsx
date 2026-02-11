import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { deleteAsset } from "../api/adminAssets";
import { deleteHymn, getHymnDetail, updateHymn, type HymnDetailResponse } from "../api/hymns";
import AdminAssetUploadPage from "./AdminAssetUploadPage";

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
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deletingAssetId, setDeletingAssetId] = useState<string | null>(null);

  const loadHymn = useCallback(() => {
    if (!id) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getHymnDetail(id)
      .then((data) => {
        setHymn(data);
        setTitle(data.title);
        setNumber(data.number != null ? String(data.number) : "");
        setTags(data.tags ?? "");
        setEnabled(data.enabled);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "찬양을 불러올 수 없습니다.");
      })
      .finally(() => {
        setLoading(false);
      });
  }, [id]);

  useEffect(() => {
    loadHymn();
  }, [loadHymn]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setError(null);
    setSaving(true);
    try {
      await updateHymn(id, {
        title: title.trim() || null,
        number: number ? parseInt(number, 10) : null,
        tags: tags.trim() || null,
        enabled,
      });
      navigate("/hymns", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "수정에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleAssetUploaded = useCallback(() => {
    if (!id) return;
    getHymnDetail(id)
      .then((data) => {
        setHymn(data);
      })
      .catch(() => {
        // silently ignore refresh errors
      });
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
    try {
      await deleteAsset(assetId);
      handleAssetUploaded();
    } catch (err) {
      alert(err instanceof Error ? err.message : "에셋 삭제에 실패했습니다.");
    } finally {
      setDeletingAssetId(null);
    }
  };

  if (loading) return <p className="text-gray-500">로딩 중...</p>;
  if (!hymn && error) return <p className="text-red-600">{error}</p>;

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold mb-6">찬양 수정</h1>

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 flex flex-col gap-4 mb-8">
        <div>
          <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-1">
            제목
          </label>
          <input
            id="title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div>
          <label htmlFor="number" className="block text-sm font-medium text-gray-700 mb-1">
            번호
          </label>
          <input
            id="number"
            type="number"
            value={number}
            onChange={(e) => setNumber(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div>
          <label htmlFor="tags" className="block text-sm font-medium text-gray-700 mb-1">
            태그 (쉼표 구분)
          </label>
          <input
            id="tags"
            type="text"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
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

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={saving}
            className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700 disabled:opacity-50"
          >
            {saving ? "저장 중..." : "저장"}
          </button>
          <button
            type="button"
            onClick={() => navigate("/hymns")}
            className="border border-gray-300 px-4 py-2 rounded hover:bg-gray-50"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleDeleteHymn}
            disabled={deleting}
            className="ml-auto bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 disabled:opacity-50"
          >
            {deleting ? "삭제 중..." : "삭제"}
          </button>
        </div>
      </form>

      {/* Asset section */}
      {hymn && (
        <>
          {hymn.assets.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6 mb-6">
              <h2 className="text-lg font-semibold mb-3">등록된 에셋</h2>
              <table className="w-full text-left text-sm">
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
                  {hymn.assets.map((a) => (
                    <tr key={a.id} className="border-b last:border-b-0">
                      <td className="py-2">{a.type}</td>
                      <td className="py-2">{a.part}</td>
                      <td className="py-2 text-gray-500 truncate max-w-xs">{a.objectKey}</td>
                      <td className="py-2">
                        {/^https?:\/\//i.test(a.url) ? (
                          <a
                            href={a.url}
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
                          onClick={() => handleDeleteAsset(a.id)}
                          disabled={deletingAssetId === a.id}
                          className="text-red-600 hover:text-red-800 disabled:opacity-50 text-sm font-medium"
                        >
                          {deletingAssetId === a.id ? "삭제 중..." : "삭제"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold mb-3">에셋 업로드</h2>
            <AdminAssetUploadPage hymnId={id} onConfirmed={handleAssetUploaded} />
          </div>
        </>
      )}
    </div>
  );
}
