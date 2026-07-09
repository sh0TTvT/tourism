import { requestJson } from "../../api/request";
import { STORAGE_KEYS } from "../../constants/uiOptions";

export function createAuthActions(ctx) {
  function syncUserForms(profile) {
    ctx.profileForm.value = {
      displayName: profile?.displayName || "",
      email: profile?.email || "",
    };
    ctx.preferenceForm.value = {
      preferredDeparture: profile?.preferredDeparture || "",
      budgetPreference: profile?.budgetPreference || "",
      travelPreferences: profile?.travelPreferences || "",
      interestTags: Array.isArray(profile?.interestTags) ? profile.interestTags.join(", ") : "",
      memoryStrategy: profile?.memoryStrategy || "STANDARD",
    };
  }

  async function loadProfile() {
    if (!ctx.token.value) return;
    try {
      const profile = await requestJson("/api/users/me");
      ctx.user.value = profile;
      syncUserForms(profile);
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function saveProfile() {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    ctx.savingProfile.value = true;
    try {
      const profile = await requestJson("/api/users/me/profile", { method: "PUT", body: JSON.stringify(ctx.profileForm.value) });
      ctx.user.value = profile;
      syncUserForms(profile);
      ctx.showNotice("个人资料已更新。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.savingProfile.value = false;
    }
  }

  async function savePreferences() {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    ctx.savingPreferences.value = true;
    try {
      const profile = await requestJson("/api/users/me/preferences", {
        method: "PUT",
        body: JSON.stringify({
          preferredDeparture: ctx.preferenceForm.value.preferredDeparture,
          budgetPreference: ctx.preferenceForm.value.budgetPreference,
          travelPreferences: ctx.preferenceForm.value.travelPreferences,
          interestTags: ctx.preferenceForm.value.interestTags,
          memoryStrategy: ctx.preferenceForm.value.memoryStrategy,
        }),
      });
      ctx.user.value = profile;
      syncUserForms(profile);
      ctx.showNotice("旅行偏好已保存。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.savingPreferences.value = false;
    }
  }

  async function changePassword() {
    if (!ctx.passwordForm.value.currentPassword || !ctx.passwordForm.value.newPassword) {
      ctx.showNotice("请填写完整的密码信息。", "error");
      return;
    }
    if (ctx.passwordForm.value.newPassword !== ctx.passwordForm.value.confirmPassword) {
      ctx.showNotice("两次输入的新密码不一致。", "error");
      return;
    }
    ctx.changingPassword.value = true;
    try {
      await requestJson("/api/users/me/password", {
        method: "PUT",
        body: JSON.stringify({ currentPassword: ctx.passwordForm.value.currentPassword, newPassword: ctx.passwordForm.value.newPassword }),
      });
      ctx.passwordForm.value = { currentPassword: "", newPassword: "", confirmPassword: "" };
      ctx.showNotice("密码已更新。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.changingPassword.value = false;
    }
  }

  function setAuthTab(tab) {
    ctx.authTab.value = tab;
    ctx.authError.value = "";
  }

  async function login() {
    if (!ctx.loginForm.value.account || !ctx.loginForm.value.password) {
      ctx.authError.value = "请输入账号和密码。";
      return;
    }
    ctx.authLoading.value = true;
    ctx.authError.value = "";
    try {
      const data = await requestJson("/api/auth/login", { method: "POST", body: JSON.stringify(ctx.loginForm.value) }, false);
      ctx.token.value = data?.token || "";
      ctx.user.value = data?.user || null;
      syncUserForms(data?.user || {});
      ctx.loginForm.value = { account: "", password: "" };
      await ctx.loadConversations();
      await ctx.loadRoutePlans();
      ctx.finishAuthSuccess();
      ctx.showNotice("登录成功。", "success");
    } catch (error) {
      ctx.authError.value = error.message;
    } finally {
      ctx.authLoading.value = false;
    }
  }

  async function register() {
    if (!/^[A-Za-z][A-Za-z0-9_]{2,39}$/.test(ctx.registerForm.value.username)) {
      ctx.authError.value = "用户名需以字母开头，长度 3-40 位，可包含数字和下划线。";
      return;
    }
    if (!/^[a-zA-Z0-9]{6,}$/.test(ctx.registerForm.value.password)) {
      ctx.authError.value = "密码至少 6 位，且仅支持字母和数字。";
      return;
    }
    ctx.authLoading.value = true;
    ctx.authError.value = "";
    try {
      const data = await requestJson("/api/auth/register", { method: "POST", body: JSON.stringify(ctx.registerForm.value) }, false);
      ctx.token.value = data?.token || "";
      ctx.user.value = data?.user || null;
      syncUserForms(data?.user || {});
      ctx.registerForm.value = { username: "", displayName: "", email: "", password: "" };
      await ctx.loadConversations();
      await ctx.loadRoutePlans();
      ctx.finishAuthSuccess();
      ctx.showNotice("注册成功，已自动登录。", "success");
    } catch (error) {
      ctx.authError.value = error.message;
    } finally {
      ctx.authLoading.value = false;
    }
  }

  function clearSession() {
    ctx.stopStreamingResponse?.();
    ctx.token.value = "";
    ctx.user.value = null;
    ctx.conversations.value = [];
    ctx.explorePosts.value = [];
    ctx.exploreRoutePlanDetail.value = null;
    ctx.routePlans.value = [];
    ctx.sharingRoutePlanId.value = null;
    ctx.activeRoutePlanId.value = null;
    ctx.selectedConversationId.value = null;
    ctx.messages.value = [];
    syncUserForms({});
    ctx.resetRouteEditor?.();
    localStorage.removeItem(STORAGE_KEYS.token);
    localStorage.removeItem(STORAGE_KEYS.user);
  }

  function logout() {
    clearSession();
    ctx.closeAccountMenu();
    ctx.openAccountModal("account");
    ctx.showNotice("已退出登录。", "success");
  }

  return { syncUserForms, loadProfile, saveProfile, savePreferences, changePassword, setAuthTab, login, register, clearSession, logout };
}
