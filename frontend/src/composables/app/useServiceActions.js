import { requestJson } from "../../api/request";
import { createDefaultMapConfig } from "./helpers";

export function createServiceActions(ctx) {
  let publicServiceStatusTimerId = 0;

  function selectModelOption(item) {
    if (!item?.available) return;
    ctx.modelSelectionKey.value = `${item.provider}::${item.id}`;
    ctx.closeModelMenu();
  }

  async function loadModels() {
    try {
      const data = await requestJson("/api/models", {}, false);
      ctx.models.value = Array.isArray(data?.models) ? data.models : [];
      ctx.defaultProvider.value = data?.defaultProvider || "";
      ctx.defaultModel.value = data?.defaultModel || "";
      const stored = ctx.models.value.find((item) => item.provider === ctx.provider.value && item.id === ctx.model.value && item.available);
      const fallback =
        stored ||
        ctx.models.value.find((item) => item.provider === data?.defaultProvider && item.id === data?.defaultModel && item.available) ||
        ctx.models.value.find((item) => item.available) ||
        ctx.models.value[0];
      ctx.provider.value = fallback?.provider || data?.defaultProvider || "";
      ctx.model.value = fallback?.id || data?.defaultModel || "";
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function loadPublicServiceStatus(silent = false) {
    try {
      const data = await requestJson("/api/public/services/status", {}, false);
      ctx.publicServices.value = Array.isArray(data?.services) ? data.services : [];
      ctx.mapRuntimeConfig.value = { ...createDefaultMapConfig(), ...(data?.mapConfig || {}) };
    } catch (error) {
      if (!silent) ctx.showNotice(error.message, "error");
    }
  }

  function startPublicServicePolling() {
    publicServiceStatusTimerId = window.setInterval(() => {
      loadPublicServiceStatus(true);
    }, 300000);
  }

  function stopPublicServicePolling() {
    window.clearInterval(publicServiceStatusTimerId);
  }

  return { selectModelOption, loadModels, loadPublicServiceStatus, startPublicServicePolling, stopPublicServicePolling };
}
