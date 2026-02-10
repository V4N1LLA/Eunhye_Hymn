import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createHymn } from "../api/hymns";

export default function HymnCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [number, setNumber] = useState("");
  const [tags, setTags] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setError("제목을 입력해 주세요.");
      return;
    }
    setError(null);
    setSaving(true);
    try {
      await createHymn({
        title: title.trim(),
        number: number ? parseInt(number, 10) : null,
        tags: tags.trim() || null,
        enabled,
      });
      navigate("/hymns", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "생성에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-lg">
      <h1 className="text-2xl font-bold mb-6">새 찬양 추가</h1>
      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 flex flex-col gap-4">
        <div>
          <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-1">
            제목 *
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
            placeholder="예: 찬양, 경배"
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
        </div>
      </form>
    </div>
  );
}
