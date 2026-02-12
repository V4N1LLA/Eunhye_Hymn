import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type SocialProvider = "GOOGLE" | "KAKAO";

export default function LoginPage() {
  const { login, loginWithSocial } = useAuth();
  const navigate = useNavigate();

  // Social login state
  const [socialProvider, setSocialProvider] = useState<SocialProvider | null>(null);
  const [socialToken, setSocialToken] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [needsInviteCode, setNeedsInviteCode] = useState(false);

  // Dev login state
  const [showDevLogin, setShowDevLogin] = useState(false);
  const [displayName, setDisplayName] = useState("");

  // Shared state
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSocialLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!socialProvider || !socialToken.trim()) return;
    setError(null);
    setLoading(true);
    try {
      await loginWithSocial(socialProvider, socialToken.trim(), inviteCode.trim() || undefined);
      navigate("/hymns", { replace: true });
    } catch (err) {
      const message = err instanceof Error ? err.message : "로그인에 실패했습니다.";
      if (message.includes("초대코드")) {
        setNeedsInviteCode(true);
        setError(message);
      } else {
        setError(message);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDevLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName.trim()) {
      setError("이름을 입력해 주세요.");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await login({
        userId: crypto.randomUUID(),
        role: "ADMIN",
        displayName: displayName.trim(),
      });
      navigate("/hymns", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const resetSocial = () => {
    setSocialProvider(null);
    setSocialToken("");
    setInviteCode("");
    setNeedsInviteCode(false);
    setError(null);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white rounded-lg shadow-md w-full max-w-sm p-8">
        <h1 className="text-2xl font-bold text-center mb-6">Eunhye Admin</h1>

        {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

        {/* Social login buttons or token form */}
        {!socialProvider ? (
          <div className="flex flex-col gap-3 mb-6">
            <button
              type="button"
              onClick={() => setSocialProvider("GOOGLE")}
              className="w-full flex items-center justify-center gap-2 border border-gray-300 rounded py-2 font-medium hover:bg-gray-50"
            >
              <span className="text-lg">G</span>
              Google 로그인
            </button>
            <button
              type="button"
              onClick={() => setSocialProvider("KAKAO")}
              className="w-full flex items-center justify-center gap-2 bg-yellow-300 rounded py-2 font-medium hover:bg-yellow-400"
            >
              <span className="text-lg">K</span>
              Kakao 로그인
            </button>
          </div>
        ) : (
          <form onSubmit={handleSocialLogin} className="flex flex-col gap-3 mb-6">
            <h2 className="text-sm font-semibold text-gray-700">
              {socialProvider === "GOOGLE" ? "Google" : "Kakao"} 로그인
            </h2>
            <div>
              <label htmlFor="socialToken" className="block text-sm font-medium text-gray-700 mb-1">
                ID Token
              </label>
              <input
                id="socialToken"
                type="text"
                value={socialToken}
                onChange={(e) => setSocialToken(e.target.value)}
                placeholder="소셜 로그인 ID Token 입력"
                required
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            {needsInviteCode && (
              <div>
                <label htmlFor="inviteCode" className="block text-sm font-medium text-gray-700 mb-1">
                  초대코드
                </label>
                <input
                  id="inviteCode"
                  type="text"
                  value={inviteCode}
                  onChange={(e) => setInviteCode(e.target.value)}
                  placeholder="초대코드를 입력하세요"
                  required
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            )}
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={loading}
                className="flex-1 bg-indigo-600 text-white rounded py-2 font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {loading ? "로그인 중..." : "로그인"}
              </button>
              <button
                type="button"
                onClick={resetSocial}
                className="border border-gray-300 rounded px-4 py-2 hover:bg-gray-50"
              >
                취소
              </button>
            </div>
          </form>
        )}

        {/* Divider */}
        <div className="flex items-center gap-3 mb-4">
          <div className="flex-1 h-px bg-gray-300" />
          <span className="text-xs text-gray-400">또는</span>
          <div className="flex-1 h-px bg-gray-300" />
        </div>

        {/* Dev login (collapsible) */}
        <button
          type="button"
          onClick={() => setShowDevLogin(!showDevLogin)}
          className="w-full text-sm text-gray-500 hover:text-gray-700 mb-2"
        >
          {showDevLogin ? "▲ Dev 로그인 닫기" : "▼ Dev 로그인 (개발용)"}
        </button>

        {showDevLogin && (
          <form onSubmit={handleDevLogin} className="flex flex-col gap-3">
            <div>
              <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 mb-1">
                표시 이름
              </label>
              <input
                id="displayName"
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="관리자"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <p className="text-xs text-gray-500">
              Dev 프로필 전용. ADMIN 역할로 자동 로그인됩니다.
            </p>
            <button
              type="submit"
              disabled={loading}
              className="bg-gray-600 text-white rounded py-2 font-medium hover:bg-gray-700 disabled:opacity-50"
            >
              {loading ? "로그인 중..." : "Dev 로그인"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
