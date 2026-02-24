import { beforeEach, describe, expect, it } from "vitest";

import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  migrateLegacyLocalStorageTokens,
  setTokens,
} from "./tokenStore";

describe("tokenStore", () => {
  beforeEach(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
  });

  it("migrates legacy localStorage tokens into sessionStorage", () => {
    window.localStorage.setItem("accessToken", "legacy-access");
    window.localStorage.setItem("refreshToken", "legacy-refresh");

    migrateLegacyLocalStorageTokens();

    expect(window.sessionStorage.getItem("accessToken")).toBe("legacy-access");
    expect(window.sessionStorage.getItem("refreshToken")).toBe("legacy-refresh");
    expect(window.localStorage.getItem("accessToken")).toBeNull();
    expect(window.localStorage.getItem("refreshToken")).toBeNull();
  });

  it("does not overwrite session tokens during legacy migration", () => {
    window.sessionStorage.setItem("accessToken", "current-access");
    window.sessionStorage.setItem("refreshToken", "current-refresh");
    window.localStorage.setItem("accessToken", "legacy-access");
    window.localStorage.setItem("refreshToken", "legacy-refresh");

    migrateLegacyLocalStorageTokens();

    expect(window.sessionStorage.getItem("accessToken")).toBe("current-access");
    expect(window.sessionStorage.getItem("refreshToken")).toBe("current-refresh");
    expect(window.localStorage.getItem("accessToken")).toBeNull();
    expect(window.localStorage.getItem("refreshToken")).toBeNull();
  });

  it("stores and clears tokens in sessionStorage", () => {
    setTokens("new-access", "new-refresh");

    expect(getAccessToken()).toBe("new-access");
    expect(getRefreshToken()).toBe("new-refresh");

    clearTokens();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });
});
