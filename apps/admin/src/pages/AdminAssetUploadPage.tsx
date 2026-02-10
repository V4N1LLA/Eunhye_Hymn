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
}

export default function AdminAssetUploadPage({ hymnId: hymnIdProp }: Props) {
  const params = useParams<{ hymnId?: string }>();
  const initialHymnId = hymnIdProp ?? params.hymnId ?? "";

  const [hymnId, setHymnId] = useState(initialHymnId);
  const [assetType, setAssetType] = useState<AssetType>("PDF");
  const [part, setPart] = useState<PartType | "">("");
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
    } catch (error) {
      setLastError(error instanceof Error ? error.message : "확인 요청에 실패했습니다.");
      setUploadState("uploaded");
    }
  };

  const showHymnIdField = !hymnIdProp;

  return (
    <div className="max-w-2xl">
      {showHymnIdField && <h1 className="text-2xl font-bold mb-6">에셋 업로드</h1>}

      <div className="flex flex-col gap-4">
        {showHymnIdField && (
          <div>
            <label htmlFor="hymnId" className="block text-sm font-medium text-gray-700 mb-1">
              찬송가 ID (UUID)
            </label>
            <input
              id="hymnId"
              type="text"
              value={hymnId}
              onChange={(e) => setHymnId(e.target.value)}
              placeholder="hymnId UUID"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        )}

        <div>
          <label htmlFor="assetType" className="block text-sm font-medium text-gray-700 mb-1">
            에셋 타입
          </label>
          <select
            id="assetType"
            value={assetType}
            onChange={(e) => setAssetType(e.target.value as AssetType)}
            className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="PDF">PDF</option>
            <option value="AUDIO">AUDIO</option>
          </select>
        </div>

        <div>
          <label htmlFor="partType" className="block text-sm font-medium text-gray-700 mb-1">
            파트 (선택)
          </label>
          <select
            id="partType"
            value={part}
            onChange={(e) => setPart(e.target.value as PartType | "")}
            className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="">(없음)</option>
            <option value="ALL">ALL</option>
            <option value="S">S</option>
            <option value="A">A</option>
            <option value="T">T</option>
            <option value="B">B</option>
          </select>
        </div>

        <div>
          <label htmlFor="file" className="block text-sm font-medium text-gray-700 mb-1">
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
        </div>
      </div>

      {/* Action buttons */}
      <div className="mt-4 flex gap-2">
        <button
          type="button"
          onClick={() => file && handlePresign(file)}
          disabled={!hymnId || !file || isPresigning}
          className="bg-gray-600 text-white px-4 py-2 rounded hover:bg-gray-700 disabled:opacity-50 text-sm"
        >
          {isPresigning ? "처리 중..." : "Presign"}
        </button>
        <button
          type="button"
          onClick={handleUpload}
          disabled={!presignResult || !file || isUploading}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50 text-sm"
        >
          {isUploading ? "업로드 중..." : "Upload"}
        </button>
        <button
          type="button"
          onClick={handleConfirm}
          disabled={!presignResult || uploadState !== "uploaded"}
          className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 disabled:opacity-50 text-sm"
        >
          Confirm
        </button>
      </div>

      {/* Presign result */}
      <div className="mt-6 p-4 bg-gray-50 rounded text-sm">
        <h2 className="font-semibold mb-2">프리사인 결과</h2>
        <div className="space-y-1 text-gray-600">
          <div>uploadUrl: {presignResult?.uploadUrl ?? "-"}</div>
          <div>publicUrl: {presignResult?.publicUrl ?? "-"}</div>
          <div>objectKey: {presignResult?.objectKey ?? "-"}</div>
        </div>
        <p className="mt-2 text-xs text-gray-400">
          objectKey는 서버 규칙에 따라 hymns/&#123;hymnId&#125;/&#123;type&#125;/&#123;part&#125;/ 로 시작해야 합니다.
          part가 없으면 ALL로 처리됩니다.
        </p>
      </div>

      {/* Confirm options */}
      <div className="mt-4 flex gap-4">
        <div className="flex-1">
          <label htmlFor="checksum" className="block text-sm font-medium text-gray-700 mb-1">
            checksum
          </label>
          <input
            id="checksum"
            type="text"
            value={checksum}
            onChange={(e) => setChecksum(e.target.value)}
            placeholder="선택"
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div className="flex-1">
          <label htmlFor="version" className="block text-sm font-medium text-gray-700 mb-1">
            version
          </label>
          <input
            id="version"
            type="text"
            value={version}
            onChange={(e) => setVersion(e.target.value)}
            placeholder="선택"
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {/* Confirm result */}
      {confirmResult && (
        <div className="mt-4 p-4 bg-green-50 rounded text-sm">
          <h2 className="font-semibold mb-2">확인 결과</h2>
          <pre className="text-xs">{JSON.stringify(confirmResult, null, 2)}</pre>
        </div>
      )}

      {/* Status */}
      <div className="mt-4 text-sm">
        <span className="text-gray-500">상태: </span>
        <span className="font-medium">
          {isPresigning
            ? "프리사인 중"
            : uploadState === "idle"
              ? "대기"
              : uploadState === "presigned"
                ? "프리사인 완료"
                : uploadState === "uploaded"
                  ? "업로드 완료"
                  : "확인 완료"}
        </span>
        {lastError && <span className="ml-3 text-red-600">오류: {lastError}</span>}
      </div>
    </div>
  );
}
