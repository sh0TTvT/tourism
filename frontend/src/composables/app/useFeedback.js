export function createFeedbackActions(ctx) {
  let noticeTimerId = 0;
  let confirmResolver = null;

  function showNotice(message, type = "info") {
    ctx.notice.value = { message, type };
    window.clearTimeout(noticeTimerId);
    noticeTimerId = window.setTimeout(() => {
      ctx.notice.value = null;
    }, 3200);
  }

  function requestConfirm(options = {}) {
    if (confirmResolver) {
      confirmResolver(false);
      confirmResolver = null;
    }
    ctx.confirmDialog.value = {
      eyebrow: options.eyebrow || "删除确认",
      title: options.title || "确定继续吗？",
      message: options.message || "此操作不可恢复。",
      confirmLabel: options.confirmLabel || "确认",
    };
    ctx.closeAccountMenu?.();
    ctx.closeModelMenu?.();
    return new Promise((resolve) => {
      confirmResolver = resolve;
    });
  }

  function closeConfirmDialog(confirmed = false) {
    ctx.confirmDialog.value = null;
    if (confirmResolver) {
      const resolve = confirmResolver;
      confirmResolver = null;
      resolve(confirmed);
    }
  }

  function cleanupFeedback() {
    window.clearTimeout(noticeTimerId);
  }

  return { showNotice, requestConfirm, closeConfirmDialog, cleanupFeedback };
}
