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

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const normalizedTitle = title.trim();
    const normalizedNumber = number.trim();

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
      setError(err instanceof Error ? err.message : "찬양 생성에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel max-w-3xl">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">카탈로그</p>
            <h1 className="soy-title">새 찬양 등록</h1>
            <p className="soy-description">제목, 번호, 태그, 노출 상태를 입력해 찬양을 추가합니다.</p>
          </div>
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
              {title.trim().length}/{TITLE_MAX_LENGTH}
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
            <div className="mt-1 text-xs text-slate-500">숫자/문자 모두 입력 가능합니다.</div>
          </label>

          <label>
            <span className="soy-label">태그 (쉼표 구분)</span>
            <input
              id="tags"
              type="text"
              value={tags}
              onChange={(event) => setTags(event.target.value)}
              placeholder="예: 경배, 기도"
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
            앱에서 즉시 활성화
          </label>

          {error && <div className="md:col-span-2 soy-alert soy-alert-error">{error}</div>}

          <div className="md:col-span-2 flex flex-wrap gap-2">
            <button type="submit" disabled={saving || !title.trim()} className="soy-btn soy-btn-primary">
              {saving ? "저장 중..." : "저장"}
            </button>
            <button type="button" onClick={() => navigate("/hymns")} className="soy-btn soy-btn-secondary">
              취소
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
