import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";

import {
  AssetType,
  ConfirmResponse,
  PartType,
  PresignResponse,
  confirmAsset,
  presignAsset,
} from "../api/adminAssets";

type UploadState = "idle" | "presigned" | "uploaded" | "confirmed";

interface Props {
  hymnId?: string;
  onConfirmed?: () => void;
}

function toUploadStateLabel(uploadState: UploadState, isPresigning: boolean): string {
  if (isPresigning) return "프리사인 요청 중";
  if (uploadState === "idle") return "대기";
  if (uploadState === "presigned") return "프리사인 완료";
  if (uploadState === "uploaded") return "업로드 완료";
  return "확정 완료";
}

export default function AdminAssetUploadPage({ hymnId: hymnIdProp, onConfirmed }: Props) {
  const params = useParams<{ hymnId?: string }>();
  const initialHymnId = hymnIdProp ?? params.hymnId ?? "";

  const [hymnId, setHymnId] = useState(initialHymnId);
  const [assetType, setAssetType] = useState<AssetType>("PNG");
  const [part, setPart] = useState<PartType | "">("ALL");
  const [file, setFile] = useState<File | null>(null);
  const [presignResult, setPresignResult] = useState<PresignResponse | null>(null);
  const [confirmResult, setConfirmResult] = useState<ConfirmResponse | null>(null);
  const [checksum, setChecksum] = useState("");
  const [version, setVersion] = useState("");
  const [lastError, setLastError] = useState<string | null>(null);
  const [uploadState, setUploadState] = useState<UploadState>("idle");
  const [isPresigning, setIsPresigning] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  const partValue = useMemo(() => (part === "" ? undefined : part), [part]);

  const handlePresign = async (selectedFile: File) => {
    if (!hymnId) {
      setLastError("찬송가 ID(hymnId)를 입력해 주세요.");
      return;
    }
    setLastError(null);
    setConfirmResult(null);
    setIsPresigning(true);
    try {
      const result = await presignAsset({
        hymnId,
        type: assetType,
        part: partValue,
        filename: selectedFile.name,
        contentType: selectedFile.type || "application/octet-stream",
      });
      setPresignResult(result);
      setUploadState("presigned");
    } catch (error) {
      setLastError(error instanceof Error ? error.message : "프리사인 요청에 실패했습니다.");
    } finally {
      setIsPresigning(false);
    }
  };

  const handleUpload = async () => {
    if (!presignResult || !file) {
      setLastError("프리사인 결과 또는 파일이 없습니다.");
      return;
    }
    setLastError(null);
    setIsUploading(true);
    try {
      const response = await fetch(presignResult.uploadUrl, {
        method: "PUT",
        headers: { "Content-Type": file.type || "application/octet-stream" },
        body: file,
      });
      if (!response.ok) {
        throw new Error(`업로드 실패 (status: ${response.status})`);
      }
      setUploadState("uploaded");
    } catch (error) {
      setLastError(error instanceof Error ? error.message : "업로드에 실패했습니다.");
      setUploadState("presigned");
    } finally {
      setIsUploading(false);
    }
  };

  const handleConfirm = async () => {
    if (!presignResult || uploadState !== "uploaded") return;
    setLastError(null);
    try {
      const result = await confirmAsset({
        hymnId,
        type: assetType,
        part: partValue,
        publicUrl: presignResult.publicUrl,
        objectKey: presignResult.objectKey,
        checksum: checksum || undefined,
        version: version || undefined,
      });
      setConfirmResult(result);
      setUploadState("confirmed");
      onConfirmed?.();
    } catch (error) {
      setLastError(error instanceof Error ? error.message : "확인 요청에 실패했습니다.");
      setUploadState("uploaded");
    }
  };

  const showHymnIdField = !hymnIdProp;

  return (
    <div className="max-w-3xl space-y-4">
      {showHymnIdField && (
        <div>
          <h1 className="text-2xl font-bold text-slate-900">에셋 업로드</h1>
          <p className="mt-1 text-sm text-slate-600">파일 선택 후 `프리사인 → 업로드 → 확정` 순서로 진행합니다.</p>
        </div>
      )}

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="grid gap-4 md:grid-cols-2">
          {showHymnIdField && (
            <div className="md:col-span-2">
              <label htmlFor="hymnId" className="mb-1 block text-sm font-medium text-gray-700">
                찬양 ID (UUID)
              </label>
              <input
                id="hymnId"
                type="text"
                value={hymnId}
                onChange={(e) => setHymnId(e.target.value)}
                placeholder="hymnId UUID"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          )}

          <div>
            <label htmlFor="assetType" className="mb-1 block text-sm font-medium text-gray-700">
              에셋 타입
            </label>
            <select
              id="assetType"
              value={assetType}
              onChange={(e) => {
                const newType = e.target.value as AssetType;
                setAssetType(newType);
                if (newType === "PNG") setPart("ALL");
              }}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="PNG">PNG</option>
              <option value="MIDI">MIDI</option>
            </select>
          </div>

          <div>
            <label htmlFor="partType" className="mb-1 block text-sm font-medium text-gray-700">
              파트 (선택)
            </label>
            <select
              id="partType"
              value={assetType === "PNG" ? "ALL" : part}
              onChange={(e) => setPart(e.target.value as PartType | "")}
              disabled={assetType === "PNG"}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100 disabled:text-gray-500"
            >
              <option value="">(없음)</option>
              <option value="ALL">ALL</option>
              <option value="S">S</option>
              <option value="A">A</option>
              <option value="T">T</option>
              <option value="B">B</option>
            </select>
          </div>

          <div className="md:col-span-2">
            <label htmlFor="file" className="mb-1 block text-sm font-medium text-gray-700">
              파일 선택
            </label>
            <input
              id="file"
              type="file"
              onChange={(e) => {
                const selectedFile = e.target.files?.[0] ?? null;
                setFile(selectedFile);
                if (selectedFile) handlePresign(selectedFile);
              }}
              className="w-full text-sm"
            />
            {file && <p className="mt-1 text-xs text-slate-500">선택됨: {file.name}</p>}
          </div>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => file && handlePresign(file)}
            disabled={!hymnId || !file || isPresigning}
            className="rounded-lg bg-slate-700 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-50"
          >
            {isPresigning ? "프리사인 중..." : "1) 프리사인"}
          </button>
          <button
            type="button"
            onClick={handleUpload}
            disabled={!presignResult || !file || isUploading}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isUploading ? "업로드 중..." : "2) 업로드"}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={!presignResult || uploadState !== "uploaded"}
            className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
          >
            3) 확정
          </button>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">프리사인 결과</h2>
        <div className="mt-2 space-y-1 text-xs text-gray-600">
          <div className="break-all">uploadUrl: {presignResult?.uploadUrl ?? "-"}</div>
          <div className="break-all">publicUrl: {presignResult?.publicUrl ?? "-"}</div>
          <div className="break-all">objectKey: {presignResult?.objectKey ?? "-"}</div>
        </div>
        <p className="mt-2 text-xs text-gray-500">
          objectKey는 hymns/&lt;hymnId&gt;/&lt;type&gt;/&lt;part&gt; 규칙으로 저장됩니다.
        </p>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">옵션 메타데이터</h2>
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <div>
            <label htmlFor="checksum" className="mb-1 block text-sm font-medium text-gray-700">
              checksum (선택)
            </label>
            <input
              id="checksum"
              type="text"
              value={checksum}
              onChange={(e) => setChecksum(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label htmlFor="version" className="mb-1 block text-sm font-medium text-gray-700">
              version (선택)
            </label>
            <input
              id="version"
              type="text"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>
      </section>

      {confirmResult && (
        <section className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 shadow-sm">
          <h2 className="text-base font-semibold text-emerald-900">확정 결과</h2>
          <pre className="mt-2 overflow-x-auto text-xs text-emerald-900">{JSON.stringify(confirmResult, null, 2)}</pre>
        </section>
      )}

      <section className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm shadow-sm">
        <span className="text-slate-500">상태:</span>{" "}
        <span className="font-semibold text-slate-900">{toUploadStateLabel(uploadState, isPresigning)}</span>
        {lastError && <span className="ml-2 text-red-600">오류: {lastError}</span>}
      </section>
    </div>
  );
}
