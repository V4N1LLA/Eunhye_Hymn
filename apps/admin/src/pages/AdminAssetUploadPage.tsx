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
  if (isPresigning) return "Requesting presign...";
  if (uploadState === "idle") return "Idle";
  if (uploadState === "presigned") return "Presign complete";
  if (uploadState === "uploaded") return "Upload complete";
  return "Confirm complete";
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
      setLastError("hymnId is required.");
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
      setLastError(error instanceof Error ? error.message : "Failed to request presign URL.");
    } finally {
      setIsPresigning(false);
    }
  };

  const handleUpload = async () => {
    if (!presignResult || !file) {
      setLastError("Presign result and file are required.");
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
        throw new Error(`Upload failed (status: ${response.status})`);
      }
      setUploadState("uploaded");
    } catch (error) {
      setLastError(error instanceof Error ? error.message : "Failed to upload file.");
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
      setLastError(error instanceof Error ? error.message : "Failed to confirm uploaded asset.");
      setUploadState("uploaded");
    }
  };

  const showHymnIdField = !hymnIdProp;

  return (
    <div className="space-y-4">
      {showHymnIdField && (
        <div>
          <h1 className="soy-title">Asset Upload</h1>
          <p className="soy-description">Follow the sequence: presign, upload, then confirm.</p>
        </div>
      )}

      <section className="soy-panel">
        <div className="grid gap-4 md:grid-cols-2">
          {showHymnIdField && (
            <div className="md:col-span-2">
              <label htmlFor="hymnId" className="soy-label">
                Hymn ID (UUID)
              </label>
              <input
                id="hymnId"
                type="text"
                value={hymnId}
                onChange={(event) => setHymnId(event.target.value)}
                placeholder="hymnId UUID"
                className="soy-input"
              />
            </div>
          )}

          <div>
            <label htmlFor="assetType" className="soy-label">
              Asset Type
            </label>
            <select
              id="assetType"
              value={assetType}
              onChange={(event) => {
                const newType = event.target.value as AssetType;
                setAssetType(newType);
                if (newType === "PNG") setPart("ALL");
              }}
              className="soy-select"
            >
              <option value="PNG">PNG</option>
              <option value="MIDI">MIDI</option>
            </select>
          </div>

          <div>
            <label htmlFor="partType" className="soy-label">
              Part (optional)
            </label>
            <select
              id="partType"
              value={assetType === "PNG" ? "ALL" : part}
              onChange={(event) => setPart(event.target.value as PartType | "")}
              disabled={assetType === "PNG"}
              className="soy-select"
            >
              <option value="">(none)</option>
              <option value="ALL">ALL</option>
              <option value="S">S</option>
              <option value="A">A</option>
              <option value="T">T</option>
              <option value="B">B</option>
            </select>
          </div>

          <div className="md:col-span-2">
            <label htmlFor="file" className="soy-label">
              File
            </label>
            <input
              id="file"
              type="file"
              onChange={(event) => {
                const selectedFile = event.target.files?.[0] ?? null;
                setFile(selectedFile);
                if (selectedFile) {
                  void handlePresign(selectedFile);
                }
              }}
              className="w-full text-sm text-slate-700"
            />
            {file && <p className="mt-1 text-xs text-slate-500">Selected: {file.name}</p>}
          </div>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => {
              if (file) {
                void handlePresign(file);
              }
            }}
            disabled={!hymnId || !file || isPresigning}
            className="soy-btn soy-btn-secondary"
          >
            {isPresigning ? "Presigning..." : "1) Presign"}
          </button>
          <button
            type="button"
            onClick={() => void handleUpload()}
            disabled={!presignResult || !file || isUploading}
            className="soy-btn bg-blue-600 text-white hover:bg-blue-700"
          >
            {isUploading ? "Uploading..." : "2) Upload"}
          </button>
          <button
            type="button"
            onClick={() => void handleConfirm()}
            disabled={!presignResult || uploadState !== "uploaded"}
            className="soy-btn bg-emerald-600 text-white hover:bg-emerald-700"
          >
            3) Confirm
          </button>
        </div>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">Presign Result</h2>
        <div className="mt-2 space-y-1 text-xs text-slate-600">
          <div className="break-all">uploadUrl: {presignResult?.uploadUrl ?? "-"}</div>
          <div className="break-all">publicUrl: {presignResult?.publicUrl ?? "-"}</div>
          <div className="break-all">objectKey: {presignResult?.objectKey ?? "-"}</div>
        </div>
        <p className="mt-2 text-xs text-slate-500">Object key format: hymns/&lt;hymnId&gt;/&lt;type&gt;/&lt;part&gt;.</p>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">Optional Metadata</h2>
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <div>
            <label htmlFor="checksum" className="soy-label">
              Checksum
            </label>
            <input id="checksum" type="text" value={checksum} onChange={(event) => setChecksum(event.target.value)} className="soy-input" />
          </div>
          <div>
            <label htmlFor="version" className="soy-label">
              Version
            </label>
            <input id="version" type="text" value={version} onChange={(event) => setVersion(event.target.value)} className="soy-input" />
          </div>
        </div>
      </section>

      {confirmResult && (
        <section className="soy-panel border-emerald-200 bg-emerald-50">
          <h2 className="soy-title text-emerald-900">Confirm Result</h2>
          <pre className="mt-2 overflow-x-auto rounded-lg bg-white/80 p-3 text-xs text-emerald-900">
            {JSON.stringify(confirmResult, null, 2)}
          </pre>
        </section>
      )}

      <section className="soy-panel px-4 py-3 text-sm">
        <span className="text-slate-500">Status: </span>
        <span className="font-semibold text-slate-900">{toUploadStateLabel(uploadState, isPresigning)}</span>
        {lastError && <span className="ml-2 text-red-600">Error: {lastError}</span>}
      </section>
    </div>
  );
}
