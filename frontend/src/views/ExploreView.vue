<template>
  <ExploreWorkspace
    :tab="tab"
    :mine-view="mineView"
    :mine-category="mineCategory"
    :loading="loading"
    :loading-route-plans="loadingRoutePlans"
    :loading-route-plan-detail="loadingRoutePlanDetail"
    :posts="posts"
    :own-posts="ownPosts"
    :favorite-posts="favoritePosts"
    :route-plans="routePlans"
    :selected-route-plan-detail="selectedRoutePlanDetail"
    :is-authenticated="isAuthenticated"
    :is-admin="isAdmin"
    :is-mobile-viewport="isMobileViewport"
    :published-marker="publishedMarker"
    :last-published-post-id="lastPublishedPostId"
    :navigation-marker="navigationMarker"
    :sharing-route-plan-id="sharingRoutePlanId"
    :map-config="mapConfig"
    :map-service-warning="mapServiceWarning"
    :request-confirm="requestConfirm"
    @add-comment="$emit('addComment', $event)"
    @delete-comment="$emit('deleteComment', $event)"
    @delete-post="$emit('deletePost', $event)"
    @delete-route="$emit('deleteRoute', $event)"
    @import-route="$emit('importRoute', $event)"
    @notify="$emit('notify', $event)"
    @open-mobile-sidebar="$emit('openMobileSidebar')"
    @publish-post="$emit('publishPost', $event)"
    @record-route-click="$emit('recordRouteClick', $event)"
    @require-auth="$emit('requireAuth')"
    @select-route-plan-detail="$emit('selectRoutePlanDetail', $event)"
    @set-mine-category="$emit('setMineCategory', $event)"
    @set-mine-view="$emit('setMineView', $event)"
    @share-route="$emit('shareRoute', $event)"
    @start-point-chat="$emit('startPointChat', $event)"
    @toggle-comment-like="$emit('toggleCommentLike', $event)"
    @toggle-favorite="$emit('toggleFavorite', $event)"
    @toggle-like="$emit('toggleLike', $event)"
  />
</template>

<script setup>
import ExploreWorkspace from "../components/ExploreWorkspace.vue";

defineProps({
  tab: { type: String, default: "discover" },
  mineView: { type: String, default: "mine" },
  mineCategory: { type: String, default: "posts" },
  loading: { type: Boolean, default: false },
  loadingRoutePlans: { type: Boolean, default: false },
  loadingRoutePlanDetail: { type: Boolean, default: false },
  posts: { type: Array, default: () => [] },
  ownPosts: { type: Array, default: () => [] },
  favoritePosts: { type: Array, default: () => [] },
  routePlans: { type: Array, default: () => [] },
  selectedRoutePlanDetail: { type: Object, default: null },
  isAuthenticated: { type: Boolean, default: false },
  isAdmin: { type: Boolean, default: false },
  isMobileViewport: { type: Boolean, default: false },
  publishedMarker: { type: Number, default: 0 },
  lastPublishedPostId: { type: [String, Number], default: null },
  navigationMarker: { type: Number, default: 0 },
  sharingRoutePlanId: { type: [String, Number], default: null },
  mapConfig: { type: Object, default: () => ({}) },
  mapServiceWarning: { type: String, default: "" },
  requestConfirm: { type: Function, default: null },
});

defineEmits([
  "addComment",
  "deleteComment",
  "deletePost",
  "deleteRoute",
  "importRoute",
  "notify",
  "openMobileSidebar",
  "publishPost",
  "recordRouteClick",
  "requireAuth",
  "selectRoutePlanDetail",
  "setMineCategory",
  "setMineView",
  "shareRoute",
  "startPointChat",
  "toggleCommentLike",
  "toggleFavorite",
  "toggleLike",
]);
</script>
