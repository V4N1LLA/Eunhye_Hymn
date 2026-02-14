import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type SocialProvider = "GOOGLE" | "KAKAO";

function isLocalDevHost(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1";
}

export default function LoginPage() {
  const { login, loginWithSocial } = useAuth();
  const navigate = useNavigate();
  const showDevLogin = isLocalDevHost(window.location.hostname);

  const [socialProvider, setSocialProvider] = useState<SocialProvider | null>(null);
  const [socialToken, setSocialToken] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [needsInviteCode, setNeedsInviteCode] = useState(false);

  const [showDevLoginForm, setShowDevLoginForm] = useState(false);
  const [displayName, setDisplayName] = useState("");

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const socialTokenLabel = socialProvider === "KAKAO" ? "Access Token" : "ID Token";
  const socialTokenPlaceholder =
    socialProvider === "KAKAO" ? "Paste Kakao Access Token (without Bearer)" : "Paste Google ID Token";
  const socialTokenGuide =
    socialProvider === "KAKAO"
      ? "Kakao login requires an Access Token. Do not paste an ID Token."
      : "Google login requires an ID Token.";

  const handleSocialLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!socialProvider || !socialToken.trim()) return;

    setError(null);
    setLoading(true);
    try {
      await loginWithSocial(socialProvider, socialToken.trim(), inviteCode.trim() || undefined);
      navigate("/hymns", { replace: true });
    } catch (err) {
      const message = err instanceof Error ? err.message : "Login failed.";
      if (message.includes("초대코드")) {
        setNeedsInviteCode(true);
      }
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleDevLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName.trim()) {
      setError("Display name is required.");
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
      setError(err instanceof Error ? err.message : "Login failed.");
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

        {!socialProvider ? (
          <div className="flex flex-col gap-3 mb-6">
            <button
              type="button"
              onClick={() => setSocialProvider("GOOGLE")}
              className="w-full flex items-center justify-center gap-2 border border-gray-300 rounded py-2 font-medium hover:bg-gray-50"
            >
              <span className="text-lg">G</span>
              Google Login
            </button>
            <button
              type="button"
              onClick={() => setSocialProvider("KAKAO")}
              className="w-full flex items-center justify-center gap-2 bg-yellow-300 rounded py-2 font-medium hover:bg-yellow-400"
            >
              <span className="text-lg">K</span>
              Kakao Login
            </button>
          </div>
        ) : (
          <form onSubmit={handleSocialLogin} className="flex flex-col gap-3 mb-6">
            <h2 className="text-sm font-semibold text-gray-700">
              {socialProvider === "GOOGLE" ? "Google" : "Kakao"} Login
            </h2>
            <div>
              <label htmlFor="socialToken" className="block text-sm font-medium text-gray-700 mb-1">
                {socialTokenLabel}
              </label>
              <input
                id="socialToken"
                type="text"
                value={socialToken}
                onChange={(e) => setSocialToken(e.target.value)}
                placeholder={socialTokenPlaceholder}
                required
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <p className="mt-1 text-xs text-gray-500">{socialTokenGuide}</p>
            </div>
            {needsInviteCode && (
              <div>
                <label htmlFor="inviteCode" className="block text-sm font-medium text-gray-700 mb-1">
                  Invite Code
                </label>
                <input
                  id="inviteCode"
                  type="text"
                  value={inviteCode}
                  onChange={(e) => setInviteCode(e.target.value)}
                  placeholder="Enter invite code"
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
                {loading ? "Signing in..." : "Sign In"}
              </button>
              <button
                type="button"
                onClick={resetSocial}
                className="border border-gray-300 rounded px-4 py-2 hover:bg-gray-50"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {showDevLogin && (
          <>
            <div className="flex items-center gap-3 mb-4">
              <div className="flex-1 h-px bg-gray-300" />
              <span className="text-xs text-gray-400">or</span>
              <div className="flex-1 h-px bg-gray-300" />
            </div>

            <button
              type="button"
              onClick={() => setShowDevLoginForm((v) => !v)}
              className="w-full text-sm text-gray-500 hover:text-gray-700 mb-2"
            >
              {showDevLoginForm ? "Hide Dev Login" : "Dev Login (local only)"}
            </button>

            {showDevLoginForm && (
              <form onSubmit={handleDevLogin} className="flex flex-col gap-3">
                <div>
                  <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 mb-1">
                    Display Name
                  </label>
                  <input
                    id="displayName"
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Admin"
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <p className="text-xs text-gray-500">Dev login is enabled only on localhost.</p>
                <button
                  type="submit"
                  disabled={loading}
                  className="bg-gray-600 text-white rounded py-2 font-medium hover:bg-gray-700 disabled:opacity-50"
                >
                  {loading ? "Signing in..." : "Dev Sign In"}
                </button>
              </form>
            )}
          </>
        )}
      </div>
    </div>
  );
}
