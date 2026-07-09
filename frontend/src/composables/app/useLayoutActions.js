import { nextTick } from "vue";
import { resolveElement, resolveViewFromHash } from "./helpers";

export function createLayoutActions(ctx) {
  function syncViewport() {
    ctx.isMobileViewport.value = window.innerWidth < 960;
    if (!ctx.isMobileViewport.value) {
      ctx.isMobileSidebarOpen.value = false;
    } else {
      ctx.isSidebarCollapsed.value = false;
    }
  }

  function closeMobileSidebar() {
    ctx.isMobileSidebarOpen.value = false;
  }

  function closeModelMenu() {
    ctx.isModelMenuOpen.value = false;
  }

  function closeAccountMenu() {
    ctx.isAccountMenuOpen.value = false;
  }

  function closeAccountModal(options = {}) {
    ctx.isAccountModalOpen.value = false;
    if (!options.preserveHash && window.location.hash !== "#/chat") {
      window.history.replaceState(null, "", "#/chat");
    }
  }

  function syncHashView() {
    const hashView = resolveViewFromHash();
    ctx.currentView.value = hashView === "profile" ? "chat" : hashView;
    if (ctx.currentView.value !== "explore") {
      ctx.previousPrimaryView.value = ctx.currentView.value;
    }
    ctx.isAccountModalOpen.value = hashView === "profile";
    closeAccountMenu();
    closeModelMenu();
    if (hashView !== "profile") {
      closeMobileSidebar();
    }
  }

  function switchView(view) {
    const nextView = view === "profile" ? "chat" : view;
    if (nextView === "explore") {
      if (ctx.currentView.value !== "explore") {
        ctx.previousPrimaryView.value = ctx.currentView.value;
      }
      ctx.isSearchOpen.value = false;
    } else if (nextView !== "profile") {
      ctx.previousPrimaryView.value = nextView;
    }

    ctx.currentView.value = nextView;
    closeModelMenu();
    closeAccountMenu();
    if (view !== "profile") {
      closeAccountModal({ preserveHash: true });
    }
    const nextHash =
      view === "profile"
        ? "#/profile"
        : nextView === "routes"
          ? "#/routes"
          : nextView === "explore"
            ? "#/explore"
            : "#/chat";
    if (window.location.hash !== nextHash) {
      window.history.replaceState(null, "", nextHash);
    }
    if (ctx.isMobileViewport.value) {
      closeMobileSidebar();
    }
  }

  function toggleSidebar() {
    closeModelMenu();
    closeAccountMenu();
    if (ctx.isMobileViewport.value) {
      ctx.isMobileSidebarOpen.value = !ctx.isMobileSidebarOpen.value;
      return;
    }
    ctx.isSidebarCollapsed.value = !ctx.isSidebarCollapsed.value;
    if (ctx.isSidebarCollapsed.value) {
      ctx.isSearchOpen.value = false;
    }
  }

  function openMobileSidebar() {
    if (ctx.isMobileViewport.value) {
      closeModelMenu();
      closeAccountMenu();
      ctx.isMobileSidebarOpen.value = true;
    }
  }

  function toggleModelMenu() {
    if (!ctx.models.value.length) return;
    ctx.isModelMenuOpen.value = !ctx.isModelMenuOpen.value;
  }

  function toggleAccountMenu() {
    closeModelMenu();
    ctx.isAccountMenuOpen.value = !ctx.isAccountMenuOpen.value;
  }

  function openAccountModal(section = "account") {
    ctx.accountModalSection.value = section;
    ctx.isAccountModalOpen.value = true;
    closeAccountMenu();
    closeModelMenu();
    if (window.location.hash !== "#/profile") {
      window.history.replaceState(null, "", "#/profile");
    }
    if (ctx.isMobileViewport.value) {
      closeMobileSidebar();
    }
  }

  function finishAuthSuccess() {
    closeAccountModal();
    closeAccountMenu();
    closeModelMenu();
    closeMobileSidebar();
    switchView("chat");
  }

  async function toggleSearch() {
    if (ctx.isSidebarCollapsed.value && !ctx.isMobileViewport.value) {
      ctx.isSidebarCollapsed.value = false;
      await nextTick();
    }
    ctx.isSearchOpen.value = !ctx.isSearchOpen.value;
    if (!ctx.isSearchOpen.value) {
      ctx.historyQuery.value = "";
    }
  }

  function handleDocumentPointerDown(event) {
    const modelMenuElement = resolveElement(ctx.chatWorkspaceRef.value?.modelMenuRoot);
    if (ctx.isModelMenuOpen.value && modelMenuElement && !modelMenuElement.contains(event.target)) {
      closeModelMenu();
    }

    const accountMenuElement = resolveElement(ctx.sidebarRef.value?.accountMenuRoot);
    if (ctx.isAccountMenuOpen.value && accountMenuElement && !accountMenuElement.contains(event.target)) {
      closeAccountMenu();
    }
  }

  function handleWindowKeydown(event) {
    if (event.key === "Escape") {
      if (ctx.confirmDialog.value) {
        ctx.closeConfirmDialog(false);
        return;
      }
      closeModelMenu();
      closeAccountMenu();
      closeAccountModal();
    }
  }

  return {
    syncViewport,
    syncHashView,
    switchView,
    toggleSidebar,
    openMobileSidebar,
    closeMobileSidebar,
    toggleModelMenu,
    closeModelMenu,
    toggleAccountMenu,
    closeAccountMenu,
    openAccountModal,
    closeAccountModal,
    finishAuthSuccess,
    toggleSearch,
    handleDocumentPointerDown,
    handleWindowKeydown,
  };
}
