import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createHymn } from "../api/hymns";

const TITLE_MAX_LENGTH = 120;
const NUMBER_MAX_LENGTH = 20;

function normalizeOptional(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

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
    const normalizedTitle = title.trim();
    const normalizedNumber = number.trim();

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

    setError(null);
    setSaving(true);
    try {
      await createHymn({
        title: normalizedTitle,
        number: normalizedNumber || null,
        tags: normalizeOptional(tags),
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
      <h1 className="mb-6 text-2xl font-bold">새 찬양 추가</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-lg bg-white p-6 shadow">
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
            {title.trim().length}/{TITLE_MAX_LENGTH}
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
            placeholder="예: 찬양, 경배"
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

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={saving || !title.trim()}
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
        </div>
      </form>
    </div>
  );
}
