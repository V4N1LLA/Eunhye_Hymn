import { useMemo, useState } from "react";

import {
  AssetType,
  ConfirmResponse,
  PartType,
  PresignResponse,
  confirmAsset,
  presignAsset,
} from "../api/adminAssets";

type UploadState = "idle" | "presigned" | "uploaded" | "confirmed";

export default function AdminAssetUploadPage() {
  const [hymnId, setHymnId] = useState("");
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
    if (!presignResult) {
      setLastError("프리사인 결과가 없습니다.");
      return;
    }
    if (!file) {
      setLastError("업로드할 파일을 선택해 주세요.");
      return;
    }
    setLastError(null);
    setIsUploading(true);
    try {
      const response = await fetch(presignResult.uploadUrl, {
        method: "PUT",
        headers: {
          "Content-Type": file.type || "application/octet-stream",
        },
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
    if (!presignResult) {
      setLastError("프리사인 결과가 없습니다.");
      return;
    }
    if (uploadState !== "uploaded") {
      setLastError("업로드 완료 후 확인할 수 있습니다.");
      return;
    }
    if (!presignResult.objectKey || !presignResult.publicUrl) {
      setLastError("확인에 필요한 값이 없습니다.");
      return;
    }
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

  return (
    <div style={{ maxWidth: 720, margin: "0 auto", padding: 24 }}>
      <h1>관리자 에셋 업로드</h1>

      <section style={{ display: "grid", gap: 12 }}>
        <label>
          찬송가 ID (UUID)
          <input
            type="text"
            value={hymnId}
            onChange={(event) => setHymnId(event.target.value)}
            placeholder="hymnId UUID"
            style={{ width: "100%" }}
          />
        </label>

        <label>
          에셋 타입
          <select value={assetType} onChange={(event) => setAssetType(event.target.value as AssetType)}>
            <option value="PDF">PDF</option>
            <option value="AUDIO">AUDIO</option>
          </select>
        </label>

        <label>
          파트 (선택)
          <select value={part} onChange={(event) => setPart(event.target.value as PartType | "")}>
            <option value="">(없음)</option>
            <option value="ALL">ALL</option>
            <option value="S">S</option>
            <option value="A">A</option>
            <option value="T">T</option>
            <option value="B">B</option>
          </select>
        </label>

        <label>
          파일 선택
          <input
            type="file"
            onChange={(event) => {
              const selectedFile = event.target.files?.[0] ?? null;
              setFile(selectedFile);
              if (selectedFile) {
                handlePresign(selectedFile);
              }
            }}
          />
        </label>
      </section>

      <section style={{ marginTop: 16, display: "flex", gap: 8 }}>
        <button type="button" disabled={!hymnId || !file || isPresigning}>
          Presign
        </button>
        <button type="button" onClick={handleUpload} disabled={!presignResult || !file || isUploading}>
          Upload
        </button>
        <button
          type="button"
          onClick={handleConfirm}
          disabled={!presignResult || uploadState !== "uploaded"}
        >
          Confirm
        </button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>프리사인 결과</h2>
        <div>
          <div>uploadUrl: {presignResult?.uploadUrl ?? "-"}</div>
          <div>publicUrl: {presignResult?.publicUrl ?? "-"}</div>
          <div>objectKey: {presignResult?.objectKey ?? "-"}</div>
        </div>
        <div style={{ marginTop: 8, color: "#555" }}>
          objectKey는 서버 규칙에 따라 hymns/{"{hymnId}"}/{"{type}"}/{"{part}"}/ 로 시작해야 합니다.
        </div>
        <div style={{ color: "#555" }}>part가 없으면 ALL로 처리됩니다.</div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>확인 요청 옵션</h2>
        <label>
          checksum
          <input
            type="text"
            value={checksum}
            onChange={(event) => setChecksum(event.target.value)}
            placeholder="선택"
            style={{ width: "100%" }}
          />
        </label>
        <label>
          version
          <input
            type="text"
            value={version}
            onChange={(event) => setVersion(event.target.value)}
            placeholder="선택"
            style={{ width: "100%" }}
          />
        </label>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>확인 결과</h2>
        <pre style={{ background: "#f5f5f5", padding: 12 }}>
{confirmResult ? JSON.stringify(confirmResult, null, 2) : "아직 확인하지 않았습니다."}
        </pre>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>상태</h2>
        <div>
          현재 상태:{" "}
          {isPresigning
            ? "프리사인 중"
            : uploadState === "idle"
              ? "대기"
              : uploadState === "presigned"
                ? "프리사인 완료"
                : uploadState === "uploaded"
                  ? "업로드 완료"
                  : "확인 완료"}
        </div>
        {lastError && <div style={{ color: "red" }}>오류: {lastError}</div>}
      </section>
    </div>
  );
}
