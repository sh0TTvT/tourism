import { clamp, normalizeRoutePoints, normalizeTips } from "./routePlanner";

function normalizeSharedRoute(route = {}) {
  const days = clamp(Number(route.days) || 3, 1, 14);
  return {
    title: String(route.title || "").trim(),
    summary: String(route.summary || "").trim(),
    destination: String(route.destination || "").trim(),
    days,
    interests: String(route.interests || "").trim(),
    budget: String(route.budget || "").trim(),
    departure: String(route.departure || "").trim(),
    tips: normalizeTips(route.tips || []),
    points: normalizeRoutePoints(route.points || [], days),
  };
}

function cloneSharedRoute(route) {
  if (!route) {
    return null;
  }
  return normalizeSharedRoute(JSON.parse(JSON.stringify(route)));
}

function buildFallbackImage() {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 420">
      <defs>
        <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#f97316"/>
          <stop offset="55%" stop-color="#fb923c"/>
          <stop offset="100%" stop-color="#fdba74"/>
        </linearGradient>
      </defs>
      <rect width="640" height="420" fill="url(#g)"/>
      <circle cx="122" cy="96" r="56" fill="rgba(255,255,255,0.18)"/>
      <path d="M0 324 152 208l112 94 98-76 94 84 184-152v258H0Z" fill="rgba(255,255,255,0.22)"/>
      <path d="M0 356 130 252l112 76 94-80 122 108 182-122v186H0Z" fill="rgba(255,255,255,0.3)"/>
    </svg>
  `)}`;
}

function normalizeExploreComment(comment = {}) {
  return {
    ...comment,
    own: Boolean(comment.own),
    liked: Boolean(comment.liked),
    likeCount: Number(comment.likeCount) || 0,
  };
}

function sortExploreComments(comments) {
  return [...comments].sort((left, right) => {
    if (left.own !== right.own) {
      return left.own ? -1 : 1;
    }

    if (left.own && right.own) {
      return new Date(right.createdAt || 0) - new Date(left.createdAt || 0);
    }

    if (left.likeCount !== right.likeCount) {
      return right.likeCount - left.likeCount;
    }

    return new Date(right.createdAt || 0) - new Date(left.createdAt || 0);
  });
}

function decorateExplorePosts(posts) {
  return (Array.isArray(posts) ? posts : []).map((post) => ({
    ...post,
    title:
      String(post.title || "").trim() ||
      String(post.route?.title || "").trim() ||
      String(post.route?.destination || "").trim() ||
      "未命名帖子",
    content: String(post.content || "").trim(),
    imageUrls: Array.isArray(post.imageUrls) ? post.imageUrls.filter(Boolean) : [],
    isOwn: Boolean(post.own),
    clickCount: Number(post.clickCount) || 0,
    applyCount: Number(post.applyCount) || 0,
    comments: sortExploreComments(
      (Array.isArray(post.comments) ? post.comments : []).map(
        normalizeExploreComment,
      ),
    ),
    authorInitial: String(post.authorName || "旅")
      .trim()
      .slice(0, 1)
      .toUpperCase(),
    route: post.route ? normalizeSharedRoute(post.route) : null,
  })).map((post) => ({
    ...post,
    commentCount: post.comments.length,
    routeHeat: post.clickCount + post.applyCount,
    previewText:
      post.content ||
      String(post.route?.summary || "").trim() ||
      String(post.route?.destination || "").trim() ||
      "发布者没有填写正文。",
    coverImage: (Array.isArray(post.imageUrls) && post.imageUrls[0]) || buildFallbackImage(post.title),
  }));
}

export {
  buildFallbackImage,
  cloneSharedRoute,
  decorateExplorePosts,
  normalizeSharedRoute,
  sortExploreComments,
};
