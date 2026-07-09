<template>
  <div
    class="explore-workspace"
    :class="{ 'explore-workspace--detail': viewMode === 'detail' }"
  >
    <template v-if="tab === 'map'">
      <div v-if="isMobileViewport" class="explore-frame-bar">
        <button
          type="button"
          class="icon-button mobile-nav-button explore-nav-button"
          data-tooltip="打开边栏"
          title="打开边栏"
          aria-label="打开边栏"
          @click="emit('open-mobile-sidebar')"
        >
          <AppIcon name="panel-open" />
        </button>
        <span class="explore-frame-title">地图选点</span>
      </div>

      <ExplorePointMap
        :map-config="mapConfig"
        :service-warning="mapServiceWarning"
        @start-chat="emit('start-point-chat', $event)"
      />
    </template>

    <template v-else-if="tab === 'routes' && viewMode === 'list'">
      <div v-if="isMobileViewport" class="explore-frame-bar">
        <button
          type="button"
          class="icon-button mobile-nav-button explore-nav-button"
          data-tooltip="打开边栏"
          title="打开边栏"
          aria-label="打开边栏"
          @click="emit('open-mobile-sidebar')"
        >
          <AppIcon name="panel-open" />
        </button>
        <span class="explore-frame-title">路线大厅</span>
      </div>

      <section class="explore-feed-board" @click="closeRoutePublishPanel">
        <div class="explore-list-toolbar">
          <div class="explore-list-heading">
            <span class="explore-list-eyebrow">路线大厅</span>
            <strong>路线探索</strong>
          </div>

          <label class="explore-search-box" aria-label="路线搜索">
            <AppIcon name="search" class="explore-search-icon" />
            <input
              v-model="searchQuery"
              type="text"
              maxlength="80"
              placeholder="搜索路线标题、摘要、目的地、作者"
            />
          </label>
        </div>

        <article v-if="loading" class="explore-state-card">
          <strong>正在加载共享路线</strong>
          <span>系统会同步社区里附带路线的帖子。</span>
        </article>

        <article
          v-else-if="!filteredRoutePosts.length"
          class="explore-state-card"
        >
          <strong>{{ routePostsEmptyTitle }}</strong>
          <span>{{ routePostsEmptyCopy }}</span>
        </article>

        <div v-else class="explore-post-grid">
          <article
            v-for="post in filteredRoutePosts"
            :key="`route-post-${post.id}`"
            class="explore-post-tile explore-route-post-tile"
            tabindex="0"
            role="button"
            @click="openSharedRouteDetail(post.id)"
            @keydown.enter.prevent="openSharedRouteDetail(post.id)"
            @keydown.space.prevent="openSharedRouteDetail(post.id)"
          >
            <div class="explore-post-cover">
              <img
                :src="post.coverImage"
                :alt="post.route.title || post.title"
              />
              <span class="explore-route-heat-badge">
                <AppIcon name="flame" />
                <strong>{{ formatCompactNumber(post.routeHeat) }}</strong>
              </span>
            </div>

            <div class="explore-post-copy">
              <h2>{{ post.route.title || post.title }}</h2>
              <p>
                {{
                  post.content ||
                  post.route.summary ||
                  "发布者没有填写正文。"
                }}
              </p>
            </div>

            <div class="explore-post-foot">
              <span>{{ post.route.destination || post.locationTag || "共享路线" }}</span>
              <span>{{ formatTime(post.createdAt) }}</span>
            </div>
          </article>
        </div>

        <transition name="fade-slide">
          <section
            v-if="routePublishPanelOpen"
            class="explore-route-float-panel"
            aria-label="我的路线"
            @click.stop
          >
            <div class="explore-route-float-head">
              <div>
                <span class="explore-list-eyebrow">我的路线</span>
              </div>
              <span class="explore-route-board-meta"
                >{{ routePanelPlans.length }} 条</span
              >
            </div>

            <label class="explore-search-box explore-route-float-search" aria-label="我的路线搜索">
              <AppIcon name="search" class="explore-search-icon" />
              <input
                v-model="routePanelSearchQuery"
                type="text"
                maxlength="80"
                placeholder="搜索路线标题、摘要、目的地"
              />
            </label>

            <article v-if="!isAuthenticated" class="explore-state-card">
              <strong>登录后可查看我的路线</strong>
              <span>保存过的路线会在这里集中展示。</span>
            </article>

            <article v-else-if="loadingRoutePlans" class="explore-state-card">
              <strong>正在加载我的路线</strong>
              <span>稍后会展示你已经保存的路线草稿和正式路线。</span>
            </article>

            <article
              v-else-if="!routePanelPlans.length"
              class="explore-state-card"
            >
              <strong>{{ routePanelEmptyTitle }}</strong>
              <span>{{ routePanelEmptyCopy }}</span>
            </article>

            <div v-else class="explore-route-float-list">
              <article
                v-for="item in routePanelPlans"
                :key="`own-route-${item.routePlanId}`"
                class="explore-own-route-card"
                tabindex="0"
                role="button"
                @click="openOwnRouteDetail(item.routePlanId)"
                @contextmenu.prevent="handleRouteContextMenu(item)"
                @keydown.enter.prevent="openOwnRouteDetail(item.routePlanId)"
                @keydown.space.prevent="openOwnRouteDetail(item.routePlanId)"
              >
                <div class="explore-own-route-copy">
                  <div class="explore-route-card-badges">
                    <span class="explore-detail-tag">
                      {{ item.destination || "未设置目的地" }}
                    </span>
                    <span
                      class="explore-route-days-pill date-range-pill"
                      :title="routeDateRangeLabel(item)"
                    >
                      {{ routeDateRangeLabel(item) }}
                    </span>
                  </div>

                  <h2>{{ item.title || `${item.destination} 路线` }}</h2>
                  <p>{{ item.summary || "这条路线还没有摘要。" }}</p>

                  <div class="explore-route-card-meta">
                    <span
                      >最近更新
                      {{ formatTime(item.updatedAt || item.createdAt) }}</span
                    >
                  </div>
                </div>

              </article>
            </div>
          </section>
        </transition>

        <button
          type="button"
          class="explore-publish-fab"
          data-tooltip="我的路线"
          title="我的路线"
          aria-label="我的路线"
          :aria-expanded="routePublishPanelOpen"
          @click.stop="toggleRoutePublishPanel"
        >
          <AppIcon name="square-pen" />
        </button>
      </section>
    </template>

    <template v-else-if="showRouteDetail">
      <div class="explore-frame-bar with-back">
        <button
          type="button"
          class="explore-back-button"
          @click="closeRouteDetail"
        >
          <AppIcon name="arrow-left" />
        </button>
      </div>

      <section class="explore-route-detail-layout">
        <section class="explore-route-detail-map">
          <div class="explore-route-detail-head">
            <div>
              <p>地图展示</p>
              <strong>{{ routeDetailTitle }}</strong>
            </div>
          </div>

          <RouteMap
            :points="selectedRoutePlanPoints"
            :map-config="mapConfig"
            :service-warning="mapServiceWarning"
          />
        </section>

        <section
          class="explore-route-detail-copy-card"
          @contextmenu.prevent="routeDetailCanDelete && handleRouteContextMenu(routeDetailContext)"
        >
          <article v-if="loadingRoutePlanDetail" class="explore-state-card">
            <strong>正在加载路线详情</strong>
            <span>地图点位和行程说明马上就绪。</span>
          </article>

          <article v-else-if="routeDetailData" class="explore-route-detail-copy">
            <header class="explore-route-detail-copy-head">
              <div>
                <span class="explore-list-eyebrow">路线说明</span>
                <h1>{{ routeDetailTitle }}</h1>
              </div>

              <button
                v-if="routeDetailCanShare"
                type="button"
                class="primary-button compact"
                :disabled="sharingRoutePlanId === routeDetailData.routePlanId"
                @click="handleShareRoute(routeDetailData.routePlanId)"
              >
                {{
                  sharingRoutePlanId === routeDetailData.routePlanId
                    ? "分享中..."
                    : "分享路线"
                }}
              </button>

              <button
                v-else-if="routeDetailCanImport"
                type="button"
                class="primary-button compact"
                @click="handleImportRoute(routeDetailPost)"
              >
                一键使用
              </button>
            </header>

            <div class="explore-route-card-badges">
              <span class="explore-detail-tag">
                {{ routeDetailData.destination || "未设置目的地" }}
              </span>
              <span class="explore-route-days-pill date-range-pill" :title="routeDateRangeLabel(routeDetailData)">
                {{ routeDateRangeLabel(routeDetailData) }}
              </span>
            </div>

            <p class="explore-route-detail-summary">
              {{ routeDetailData.summary || "这条路线还没有补充摘要。" }}
            </p>

            <div v-if="routeDetailPost" class="explore-route-detail-meta-strip">
              <span>{{ routeDetailPost.authorName }}</span>
              <span>{{ formatTime(routeDetailPost.createdAt) }}</span>
              <span>热度 {{ routeDetailPost.routeHeat }}</span>
            </div>

            <dl class="explore-route-detail-meta">
              <div>
                <dt>兴趣偏好</dt>
                <dd>{{ routeDetailData.interests || "未填写" }}</dd>
              </div>
              <div>
                <dt>预算</dt>
                <dd>{{ routeDetailData.budget || "未填写" }}</dd>
              </div>
              <div>
                <dt>出发地</dt>
                <dd>{{ routeDetailData.departure || "未填写" }}</dd>
              </div>
            </dl>

            <section v-if="routeDetailData.tips?.length" class="explore-route-detail-section">
              <h2>出行提醒</h2>
              <div class="explore-route-detail-tips">
                <span v-for="(tip, index) in routeDetailData.tips" :key="`route-tip-${index}`">
                  {{ tip }}
                </span>
              </div>
            </section>

            <section class="explore-route-detail-section">
              <h2>逐日介绍</h2>
              <div class="explore-route-detail-days">
                <article
                  v-for="group in routeDetailDayGroups"
                  :key="`route-detail-day-${group.day}`"
                  class="explore-route-detail-day"
                >
                  <header>
                    <strong>Day {{ group.day }}</strong>
                    <span>{{ group.points.length }} 个点位</span>
                  </header>

                  <div class="explore-route-detail-point-list">
                    <article
                      v-for="point in group.points"
                      :key="`route-detail-point-${group.day}-${point.order}-${point.name}`"
                      class="explore-route-detail-point"
                    >
                      <span class="explore-route-detail-point-order">{{
                        point.order
                      }}</span>
                      <div>
                        <strong>{{ point.name || "未命名点位" }}</strong>
                        <p>{{ point.description || "暂无点位介绍。" }}</p>
                      </div>
                    </article>
                  </div>
                </article>
              </div>
            </section>
          </article>

          <article v-else class="explore-state-card">
            <strong>路线详情不可用</strong>
            <span>这条路线可能已被删除，返回列表后重新选择即可。</span>
          </article>
        </section>
      </section>
    </template>

    <template v-else>
      <div
        v-if="showFrameBar"
        class="explore-frame-bar"
        :class="{ 'with-back': viewMode !== 'list' }"
      >
        <button
          v-if="viewMode === 'list' && isMobileViewport"
          type="button"
          class="icon-button mobile-nav-button explore-nav-button"
          data-tooltip="打开边栏"
          title="打开边栏"
          aria-label="打开边栏"
          @click="emit('open-mobile-sidebar')"
        >
          <AppIcon name="panel-open" />
        </button>

        <button
          v-else
          type="button"
          class="explore-back-button"
          @click="closeDetail()"
        >
          <AppIcon name="arrow-left" />
        </button>

        <span v-if="viewMode === 'list'" class="explore-frame-title">{{ frameTitle }}</span>
      </div>

      <section v-if="viewMode === 'list'" class="explore-feed-board">
        <div class="explore-list-toolbar">
          <div class="explore-list-heading">
            <span class="explore-list-eyebrow">{{ frameTitle }}</span>
            <strong>{{
              isMineRoutesMode
                ? "管理自己的路线"
                : isFavoritesMode
                  ? "筛选收藏内容"
                  : isMinePostsMode
                    ? "管理自己发布的帖子"
                    : "搜索社区帖子"
            }}</strong>
          </div>

          <label class="explore-search-box" :aria-label="`${frameTitle}搜索`">
            <AppIcon name="search" class="explore-search-icon" />
            <input
              v-model="searchQuery"
              type="text"
              maxlength="80"
              :placeholder="
                isMineRoutesMode
                  ? '搜索我的路线标题、摘要、目的地'
                  : isFavoritesMode
                  ? '搜索收藏标题、正文、作者'
                  : isMinePostsMode
                    ? '搜索我的帖子标题、正文、地点'
                  : '搜索标题、正文、作者、地点'
              "
            />
          </label>
        </div>

        <div v-if="isMineTab" class="explore-segment-toolbar">
          <div class="explore-segment-group">
            <button
              type="button"
              class="explore-segment-button"
              :class="{ active: mineView === 'mine' }"
              @click="emit('set-mine-view', 'mine')"
            >
              我的
            </button>
            <button
              type="button"
              class="explore-segment-button"
              :class="{ active: mineView === 'favorites' }"
              @click="emit('set-mine-view', 'favorites')"
            >
              收藏
            </button>
          </div>

          <div v-if="mineView === 'mine'" class="explore-segment-group secondary">
            <button
              type="button"
              class="explore-segment-button"
              :class="{ active: mineCategory === 'posts' }"
              @click="emit('set-mine-category', 'posts')"
            >
              帖子
            </button>
            <button
              type="button"
              class="explore-segment-button"
              :class="{ active: mineCategory === 'routes' }"
              @click="emit('set-mine-category', 'routes')"
            >
              路线
            </button>
          </div>
        </div>

        <article v-if="loading" class="explore-state-card">
          <strong>{{ isMineRoutesMode ? "正在加载路线" : "正在加载帖子" }}</strong>
          <span>{{
            isMineRoutesMode
              ? "你发布出去的路线会从探索社区同步回来。"
              : "帖子内容、点赞和收藏会从后端数据库同步。"
          }}</span>
        </article>

        <article
          v-else-if="isMineRoutesMode ? !filteredOwnRoutePosts.length : !activePosts.length"
          class="explore-state-card"
        >
          <strong>{{ emptyStateTitle }}</strong>
          <span>{{ emptyStateCopy }}</span>
        </article>

        <div v-else-if="isMineRoutesMode" class="explore-post-grid">
          <article
            v-for="post in filteredOwnRoutePosts"
            :key="`mine-route-${post.id}`"
            class="explore-post-tile"
            tabindex="0"
            role="button"
            @click="openOwnRoutePostDetail(post.id)"
            @contextmenu.prevent="handleRouteContextMenu(post)"
            @keydown.enter.prevent="openOwnRoutePostDetail(post.id)"
            @keydown.space.prevent="openOwnRoutePostDetail(post.id)"
          >
            <div class="explore-post-cover">
              <img
                :src="post.coverImage"
                :alt="post.route?.title || post.title"
              />
              <span class="explore-route-heat-badge">
                <AppIcon name="flame" />
                <strong>{{ formatCompactNumber(post.routeHeat) }}</strong>
              </span>
            </div>

            <div class="explore-post-copy">
              <h2>{{ post.route?.title || post.title || `${post.route?.destination} 路线` }}</h2>
              <p>
                {{
                  post.route?.summary ||
                  post.content ||
                  "发布者没有填写路线说明。"
                }}
              </p>
            </div>

            <div class="explore-post-foot">
              <span>{{ post.route?.destination || "未设置目的地" }}</span>
              <span>{{ formatTime(post.createdAt) }}</span>
              <span>右键可删除</span>
            </div>
          </article>
        </div>

        <div v-else class="explore-post-grid">
          <button
            v-for="post in activePosts"
            :key="post.id"
            type="button"
            class="explore-post-tile"
            @click="openDetail(post.id)"
            @contextmenu="handlePostContextMenu(post, $event)"
          >
            <div class="explore-post-cover">
              <img :src="post.coverImage" :alt="post.title" />
              <span class="explore-route-heat-badge">
                <AppIcon name="flame" />
                <strong>{{ formatCompactNumber((post.likeCount || 0) + (post.favoriteCount || 0)) }}</strong>
              </span>
            </div>

            <div class="explore-post-copy">
              <h2>{{ post.title }}</h2>
              <p>{{ post.previewText }}</p>
            </div>

            <div class="explore-post-foot">
              <span>{{ post.likeCount }} 点赞</span>
              <span>{{ post.favoriteCount }} 收藏</span>
              <span v-if="canDeletePost(post)">右键可删除</span>
            </div>
          </button>
        </div>

        <button
          v-if="tab === 'discover' || isMinePostsMode"
          type="button"
          class="explore-publish-fab"
          data-tooltip="发布帖子"
          title="发布帖子"
          aria-label="发布帖子"
          @click="toggleComposerPanel"
        >
          <AppIcon name="square-pen" />
        </button>
      </section>

      <section
        v-else-if="viewMode === 'detail' && selectedPost"
        class="explore-detail-layout"
      >
        <article
          class="explore-detail-main"
          @contextmenu="handlePostContextMenu(selectedPost, $event)"
        >
          <div class="explore-detail-cover">
            <div class="explore-carousel" v-if="detailImages.length > 1">
              <button
                type="button"
                class="explore-carousel-arrow prev"
                :disabled="detailImageIndex === 0"
                @click="detailImageIndex = Math.max(0, detailImageIndex - 1)"
              >
                <AppIcon name="arrow-left" />
              </button>
              <img
                :src="detailImages[detailImageIndex]"
                :alt="`${selectedPost.title} - ${detailImageIndex + 1}`"
              />
              <button
                type="button"
                class="explore-carousel-arrow next"
                :disabled="detailImageIndex >= detailImages.length - 1"
                @click="detailImageIndex = Math.min(detailImages.length - 1, detailImageIndex + 1)"
              >
                <AppIcon name="arrow-right" />
              </button>
              <div class="explore-carousel-dots">
                <button
                  v-for="(_, idx) in detailImages"
                  :key="idx"
                  type="button"
                  class="explore-carousel-dot"
                  :class="{ active: idx === detailImageIndex }"
                  :aria-label="`图片 ${idx + 1}`"
                  @click="detailImageIndex = idx"
                ></button>
              </div>
            </div>
            <img
              v-else
              :src="detailImages[0] || selectedPost.coverImage"
              :alt="selectedPost.title"
            />
          </div>

          <div class="explore-detail-copy">
            <span class="explore-detail-tag">
              {{ selectedPost.locationTag || "旅行帖子" }}
            </span>
            <h1>{{ selectedPost.title }}</h1>
            <div class="explore-detail-meta">
              <span>{{ selectedPost.authorName }}</span>
              <span>{{ formatTime(selectedPost.createdAt) }}</span>
              <span v-if="selectedPost.route">附带路线</span>
            </div>
            <p class="explore-detail-content">
              {{ selectedPost.content || selectedPost.previewText }}
            </p>
          </div>

          <section v-if="selectedPost.route" class="explore-route-panel">
            <div class="explore-route-panel-head">
              <div>
                <p>共享路线</p>
                <strong>
                  {{
                    selectedPost.route.title ||
                    `${selectedPost.route.destination} 路线`
                  }}
                </strong>
              </div>

              <button
                v-if="!selectedPost.isOwn"
                type="button"
                class="ghost-button compact"
                @click="handleImportRoute(selectedPost)"
              >
                导入路线
              </button>
            </div>

            <span class="explore-route-summary">
              {{ selectedPost.route.summary || "发布者没有填写路线摘要。" }}
            </span>

            <div class="explore-route-points">
              <span
                v-for="point in selectedPost.route.points.slice(0, 6)"
                :key="`${selectedPost.id}-${point.day}-${point.order}-${point.name}`"
              >
                Day {{ point.day }} · {{ point.name || "未命名点位" }}
              </span>
            </div>
          </section>
        </article>

        <aside class="explore-detail-side">
          <div class="explore-comments-head">
            <strong>评论 {{ selectedPost.commentCount }}</strong>
            <span>我的评论会置顶，其他评论按点赞数排序。可删除时支持右键删除。</span>
          </div>

          <div class="explore-comment-stack">
            <article
              v-for="comment in selectedPost.comments"
              :key="comment.id"
              class="explore-comment-item"
              :class="{ own: comment.own }"
              @contextmenu.prevent="handleCommentContextMenu(selectedPost, comment)"
            >
              <div class="explore-comment-meta">
                <div>
                  <strong>{{ comment.authorName }}</strong>
                  <span>
                    {{
                      comment.own
                        ? `我的评论 · ${formatTime(comment.createdAt)}`
                        : formatTime(comment.createdAt)
                    }}
                  </span>
                </div>

                <button
                  type="button"
                  class="explore-comment-like"
                  :class="{ active: comment.liked }"
                  @click="handleCommentLike(selectedPost.id, comment.id)"
                >
                  <AppIcon name="heart" />
                  <span>{{ comment.likeCount }}</span>
                </button>
              </div>

              <p>{{ comment.content }}</p>
            </article>

            <article
              v-if="!selectedPost.comments.length"
              class="explore-comment-empty"
            >
              <strong>还没有评论</strong>
              <span>先写下这条帖子的第一条评论。</span>
            </article>
          </div>

          <div class="explore-comment-composer">
            <textarea
              v-model="commentDrafts[selectedPost.id]"
              rows="3"
              maxlength="500"
              placeholder="写下你的评论"
            ></textarea>

            <button
              type="button"
              class="primary-button"
              @click="submitComment(selectedPost.id)"
            >
              发送评论
            </button>
          </div>

          <div class="explore-detail-actions">
            <button
              type="button"
              class="explore-action-chip"
              :class="{ active: selectedPost.liked }"
              @click="handleLike(selectedPost.id)"
            >
              <AppIcon name="heart" />
              <span>{{ selectedPost.likeCount }} 点赞</span>
            </button>

            <button
              type="button"
              class="explore-action-chip"
              :class="{ active: selectedPost.favorited }"
              @click="handleFavorite(selectedPost.id)"
            >
              <AppIcon name="bookmark" />
              <span>{{ selectedPost.favoriteCount }} 收藏</span>
            </button>
          </div>
        </aside>
      </section>

      <article v-else class="explore-state-card">
        <strong>帖子不存在</strong>
        <span>这条帖子可能已被移除，请返回帖子列表继续浏览。</span>
      </article>

      <transition name="fade-slide">
        <section
          v-if="composerPanelOpen"
          class="explore-composer-float-panel"
          aria-label="发布帖子"
          @click.stop
        >
          <div class="explore-route-float-head">
            <div>
              <span class="explore-list-eyebrow">发布帖子</span>
            </div>
          </div>

          <div class="explore-composer-float-body">
            <label class="explore-field">
              <span>标题</span>
              <input
                v-model="composer.title"
                type="text"
                maxlength="120"
                placeholder="输入帖子标题"
              />
            </label>

            <label class="explore-field">
              <span>正文</span>
              <textarea
                v-model="composer.content"
                rows="8"
                maxlength="4000"
                placeholder="输入帖子正文"
              ></textarea>
            </label>

            <section class="explore-upload-panel">
              <div class="explore-upload-head">
                <span>插入图片</span>
                <div class="explore-upload-actions">
                  <button
                    type="button"
                    class="ghost-button compact"
                    @click="triggerImageSelect"
                  >
                    选择图片
                  </button>
                  <button
                    v-if="composer.imageUrls && composer.imageUrls.length > 0"
                    type="button"
                    class="ghost-button compact"
                    @click="composer.imageUrls = []"
                  >
                    移除全部
                  </button>
                </div>
              </div>

              <input
                ref="imageInputRef"
                type="file"
                accept="image/*"
                multiple
                class="explore-file-input"
                @change="handleImageChange"
              />

              <div v-if="composerPreviewImages.length === 1" class="explore-upload-preview">
                <img
                  :src="composerPreviewImages[0]"
                  :alt="composer.title || '帖子预览图'"
                />
              </div>
              <div v-else class="explore-upload-grid">
                <div
                  v-for="(url, idx) in composerPreviewImages"
                  :key="idx"
                  class="explore-upload-grid-item"
                >
                  <img :src="url" :alt="`图片 ${idx + 1}`" />
                  <button
                    type="button"
                    class="explore-upload-remove-btn"
                    title="移除这张图片"
                    @click="removeImage(idx)"
                  >
                    &times;
                  </button>
                </div>
              </div>
            </section>

            <button
              type="button"
              class="primary-button explore-submit-button"
              @click="submitPost"
            >
              发布
            </button>
          </div>
        </section>
      </transition>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

import { STORAGE_KEYS } from "../constants/uiOptions";
import { buildFallbackImage } from "../utils/exploreFeed";
import AppIcon from "./AppIcon.vue";
import ExplorePointMap from "./ExplorePointMap.vue";
import RouteMap from "./RouteMap.vue";

const props = defineProps({
  tab: {
    type: String,
    required: true,
  },
  mineView: {
    type: String,
    default: "mine",
  },
  mineCategory: {
    type: String,
    default: "posts",
  },
  posts: {
    type: Array,
    default: () => [],
  },
  ownPosts: {
    type: Array,
    default: () => [],
  },
  favoritePosts: {
    type: Array,
    default: () => [],
  },
  routePlans: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  loadingRoutePlans: {
    type: Boolean,
    default: false,
  },
  loadingRoutePlanDetail: {
    type: Boolean,
    default: false,
  },
  isAuthenticated: {
    type: Boolean,
    default: false,
  },
  isAdmin: {
    type: Boolean,
    default: false,
  },
  isMobileViewport: {
    type: Boolean,
    default: false,
  },
  mapConfig: {
    type: Object,
    default: () => ({}),
  },
  mapServiceWarning: {
    type: String,
    default: "",
  },
  publishedMarker: {
    type: Number,
    default: 0,
  },
  lastPublishedPostId: {
    type: Number,
    default: null,
  },
  navigationMarker: {
    type: Number,
    default: 0,
  },
  sharingRoutePlanId: {
    type: Number,
    default: null,
  },
  selectedRoutePlanDetail: {
    type: Object,
    default: null,
  },
  requestConfirm: {
    type: Function,
    default: null,
  },
});

const emit = defineEmits([
  "add-comment",
  "delete-comment",
  "delete-post",
  "delete-route",
  "import-route",
  "notify",
  "open-mobile-sidebar",
  "publish-post",
  "record-route-click",
  "require-auth",
  "select-route-plan-detail",
  "set-mine-category",
  "set-mine-view",
  "share-route",
  "start-point-chat",
  "toggle-comment-like",
  "toggle-favorite",
  "toggle-like",
]);

const viewMode = ref("list");
const selectedPostId = ref(null);
const selectedRoutePlanId = ref(null);
const commentDrafts = ref({});
const composer = ref(createEmptyComposer());
const storedDraft = ref(createEmptyComposer());
const imageInputRef = ref(null);
const searchQuery = ref("");
const routePublishPanelOpen = ref(false);
const composerPanelOpen = ref(false);
const detailImageIndex = ref(0);
const routePanelSearchQuery = ref("");

const isMineTab = computed(() => props.tab === "mine");
const isFavoritesMode = computed(
  () => isMineTab.value && props.mineView === "favorites",
);
const isMinePostsMode = computed(
  () =>
    isMineTab.value &&
    props.mineView === "mine" &&
    props.mineCategory === "posts",
);
const isMineRoutesMode = computed(
  () =>
    isMineTab.value &&
    props.mineView === "mine" &&
    props.mineCategory === "routes",
);
const ownPlainPosts = computed(() =>
  props.ownPosts.filter((post) => !post.route),
);
const showRouteDetail = computed(
  () => viewMode.value === "route-detail" && (props.tab === "routes" || isMineRoutesMode.value),
);

const sourcePosts = computed(() => {
  if (isFavoritesMode.value) {
    return props.favoritePosts;
  }
  if (isMinePostsMode.value) {
    return ownPlainPosts.value;
  }
  if (props.tab === "discover") {
    return props.posts.filter((post) => !post.route);
  }
  return props.posts;
});
const normalizedSearchQuery = computed(() =>
  String(searchQuery.value || "")
    .trim()
    .toLowerCase(),
);
const normalizedRoutePanelSearchQuery = computed(() =>
  String(routePanelSearchQuery.value || "")
    .trim()
    .toLowerCase(),
);
const activePosts = computed(() =>
  sourcePosts.value
    .filter((post) => matchesSearch(post, normalizedSearchQuery.value))
    .sort(compareActivePosts),
);
const filteredRoutePosts = computed(() =>
  props.posts
    .filter((post) => post.route)
    .filter((post) => matchesRoutePost(post, normalizedSearchQuery.value))
    .sort(compareRoutePosts),
);
const filteredOwnRoutePosts = computed(() =>
  props.ownPosts
    .filter((post) => post.route)
    .filter((post) => matchesRoutePost(post, normalizedSearchQuery.value))
    .sort(compareRoutePosts),
);
const routePanelPlans = computed(() =>
  [...props.routePlans]
    .filter((plan) =>
      matchesRoutePlan(plan, normalizedRoutePanelSearchQuery.value),
    )
    .sort(compareRoutePlanTimeDesc),
);
const selectedPost = computed(
  () => props.posts.find((post) => post.id === selectedPostId.value) || null,
);
const detailImages = computed(() => {
  if (!selectedPost.value) return [];
  const urls = selectedPost.value.imageUrls;
  if (Array.isArray(urls) && urls.length > 0) return urls;
  return selectedPost.value.coverImage ? [selectedPost.value.coverImage] : [];
});
const routeDetailPost = computed(() =>
  (props.tab === "routes" || isMineRoutesMode.value) && selectedPost.value?.route
    ? selectedPost.value
    : null,
);
const selectedRoutePlanDetail = computed(() =>
  props.selectedRoutePlanDetail?.routePlanId === selectedRoutePlanId.value
    ? props.selectedRoutePlanDetail
    : null,
);
const routeDetailData = computed(() =>
  selectedRoutePlanDetail.value || routeDetailPost.value?.route || null,
);
const routeDetailContext = computed(() =>
  routeDetailPost.value || selectedRoutePlanDetail.value || null,
);
const routeDetailIsOwn = computed(() =>
  Boolean(
    selectedRoutePlanDetail.value ||
      routeDetailPost.value?.isOwn ||
      routeDetailPost.value?.own,
  ),
);
const routeDetailCanShare = computed(() => Boolean(selectedRoutePlanDetail.value));
const routeDetailCanImport = computed(
  () => Boolean(routeDetailPost.value && !routeDetailIsOwn.value),
);
const routeDetailCanDelete = computed(() => Boolean(routeDetailIsOwn.value));
const selectedRoutePlanPoints = computed(() =>
  Array.isArray(routeDetailData.value?.points)
    ? [...routeDetailData.value.points].sort(
        (left, right) => left.day - right.day || left.order - right.order,
      )
    : [],
);
const routeDetailTitle = computed(() => {
  const route = routeDetailData.value;
  if (!route) {
    const fallback = props.routePlans.find(
      (item) => item.routePlanId === selectedRoutePlanId.value,
    );
    const postFallback = routeDetailPost.value;
    return (
      fallback?.title ||
      postFallback?.route?.title ||
      (fallback?.destination
        ? `${fallback.destination} 路线`
        : postFallback?.route?.destination
          ? `${postFallback.route.destination} 路线`
          : "路线详情")
    );
  }
  return (
    route.title ||
    (route.destination ? `${route.destination} 路线` : "路线详情")
  );
});
const routeDetailFrameTitle = computed(() =>
  props.loadingRoutePlanDetail ? "路线详情" : routeDetailTitle.value,
);
const routeDetailDayGroups = computed(() => {
  const grouped = new Map();
  selectedRoutePlanPoints.value.forEach((point) => {
    if (!grouped.has(point.day)) {
      grouped.set(point.day, []);
    }
    grouped.get(point.day).push(point);
  });
  return [...grouped.entries()].map(([day, points]) => ({ day, points }));
});
const emptyStateTitle = computed(() => {
  if (normalizedSearchQuery.value) {
    return isMineRoutesMode.value ? "没有找到匹配的路线" : "没有找到匹配的帖子";
  }
  if (isFavoritesMode.value) {
    return "你还没有收藏帖子";
  }
  if (isMinePostsMode.value) {
    return ownPlainPosts.value.length ? "没有匹配的我的帖子" : "你还没有发布帖子";
  }
  if (isMineRoutesMode.value) {
    return filteredOwnRoutePosts.value.length || props.ownPosts.some((post) => post.route)
      ? "没有匹配的我的路线"
      : "你还没有发布路线";
  }
  return "还没有可展示的帖子";
});
const emptyStateCopy = computed(() => {
  if (normalizedSearchQuery.value) {
    return isMineRoutesMode.value
      ? "试试搜索路线标题、摘要或目的地。"
      : "试试换个关键词，搜索标题、正文、作者或地点标签。";
  }
  if (isFavoritesMode.value) {
    return "在详情页点点赞或收藏后，这里会自动汇总你想回看的内容。";
  }
  if (isMinePostsMode.value) {
    return "点击右下角按钮发布第一条帖子。";
  }
  if (isMineRoutesMode.value) {
    return "先把一条路线分享到探索社区，这里会自动汇总你已经发布出去的路线。";
  }
  return "点击右下角按钮发布第一条帖子。";
});
const routePostsEmptyTitle = computed(() => {
  if (normalizedSearchQuery.value) {
    return "没有找到匹配的共享路线";
  }
  return "还没有用户发布共享路线";
});
const routePostsEmptyCopy = computed(() => {
  if (normalizedSearchQuery.value) {
    return "试试搜索路线标题、摘要、目的地或作者。";
  }
  return "当社区里有带路线的帖子后，会优先汇总到这里。";
});
const routePanelEmptyTitle = computed(() => {
  if (normalizedRoutePanelSearchQuery.value) {
    return "没有找到匹配的我的路线";
  }
  return "你还没有保存过路线";
});
const routePanelEmptyCopy = computed(() => {
  if (normalizedRoutePanelSearchQuery.value) {
    return "可以换个关键词，搜索路线标题、摘要或目的地。";
  }
  return "先在“我的路线”里生成或保存一条路线，这里就能直接分享。";
});
const hasStoredDraft = computed(() => hasComposerContent(storedDraft.value));
const composerPreviewImages = computed(() => {
  const urls = composer.value.imageUrls;
  if (Array.isArray(urls) && urls.length > 0) {
    return urls;
  }
  return [buildFallbackImage(composer.value.title || "旅途帖子")];
});
const showFrameBar = computed(
  () => props.isMobileViewport || viewMode.value !== "list",
);
const frameTitle = computed(() => {
  if (showRouteDetail.value) {
    return "路线详情";
  }
  if (viewMode.value === "detail") {
    return "帖子详情";
  }
  if (isFavoritesMode.value) {
    return "收藏";
  }
  if (isMinePostsMode.value) {
    return "我的帖子";
  }
  if (isMineRoutesMode.value) {
    return "我的路线";
  }
  return "发现";
});

watch(
  () => props.publishedMarker,
  (value, previousValue) => {
    if (!value || value === previousValue) {
      return;
    }
    clearDraft(false);
    composer.value = createEmptyComposer();
    composerPanelOpen.value = false;
    selectedPostId.value = props.lastPublishedPostId || null;
    viewMode.value = props.lastPublishedPostId ? "detail" : "list";
  },
);

watch(selectedPost, (post) => {
  if (
    (viewMode.value === "detail" || viewMode.value === "route-detail") &&
    selectedPostId.value &&
    !post
  ) {
    closeDetail();
  }
});

watch(
  () => props.tab,
  () => {
    selectedPostId.value = null;
    selectedRoutePlanId.value = null;
    searchQuery.value = "";
    routePanelSearchQuery.value = "";
    routePublishPanelOpen.value = false;
    composerPanelOpen.value = false;
    viewMode.value = "list";
  },
);

watch(
  () => props.navigationMarker,
  (value, previousValue) => {
    if (value === previousValue) {
      return;
    }
    selectedPostId.value = null;
    selectedRoutePlanId.value = null;
    routePublishPanelOpen.value = false;
    composerPanelOpen.value = false;
    viewMode.value = "list";
  },
);

onMounted(() => {
  storedDraft.value = readDraft();
  document.addEventListener("mousedown", handleFloatPanelsOutsidePointerDown);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleFloatPanelsOutsidePointerDown);
});

function createEmptyComposer() {
  return {
    title: "",
    content: "",
    imageUrls: [],
  };
}

function matchesSearch(post, keyword) {
  if (!keyword) {
    return true;
  }

  const searchText = [
    post?.title,
    post?.content,
    post?.previewText,
    post?.authorName,
    post?.locationTag,
    post?.route?.title,
    post?.route?.destination,
    post?.route?.summary,
  ]
    .map((value) =>
      String(value || "")
        .trim()
        .toLowerCase(),
    )
    .join("\n");

  return searchText.includes(keyword);
}

function compareActivePosts(left, right) {
  if (props.tab !== "discover") {
    return 0;
  }

  const leftLikeCount = Number(left?.likeCount) || 0;
  const rightLikeCount = Number(right?.likeCount) || 0;
  const leftFavoriteCount = Number(left?.favoriteCount) || 0;
  const rightFavoriteCount = Number(right?.favoriteCount) || 0;
  const leftHeat = leftLikeCount + leftFavoriteCount;
  const rightHeat = rightLikeCount + rightFavoriteCount;

  if (leftHeat !== rightHeat) {
    return rightHeat - leftHeat;
  }
  if (leftFavoriteCount !== rightFavoriteCount) {
    return rightFavoriteCount - leftFavoriteCount;
  }
  if (leftLikeCount !== rightLikeCount) {
    return rightLikeCount - leftLikeCount;
  }

  return new Date(right?.createdAt || 0) - new Date(left?.createdAt || 0);
}

function compareRoutePosts(left, right) {
  const leftHeat = Number(left?.routeHeat) || 0;
  const rightHeat = Number(right?.routeHeat) || 0;

  if (leftHeat !== rightHeat) {
    return rightHeat - leftHeat;
  }
  if ((Number(left?.clickCount) || 0) !== (Number(right?.clickCount) || 0)) {
    return (Number(right?.clickCount) || 0) - (Number(left?.clickCount) || 0);
  }
  if ((Number(left?.applyCount) || 0) !== (Number(right?.applyCount) || 0)) {
    return (Number(right?.applyCount) || 0) - (Number(left?.applyCount) || 0);
  }
  return new Date(right?.createdAt || 0) - new Date(left?.createdAt || 0);
}

function compareRoutePlanTimeDesc(left, right) {
  return (
    new Date(right?.updatedAt || right?.createdAt || 0) -
    new Date(left?.updatedAt || left?.createdAt || 0)
  );
}

function matchesRoutePost(post, keyword) {
  if (!keyword) {
    return true;
  }

  const searchText = [
    post?.title,
    post?.authorName,
    post?.locationTag,
    post?.route?.title,
    post?.route?.destination,
    post?.route?.summary,
    ...(Array.isArray(post?.route?.points)
      ? post.route.points.map((point) => point?.name)
      : []),
  ]
    .map((value) =>
      String(value || "")
        .trim()
        .toLowerCase(),
    )
    .join("\n");

  return searchText.includes(keyword);
}

function matchesRoutePlan(plan, keyword) {
  if (!keyword) {
    return true;
  }

  const searchText = [plan?.title, plan?.summary, plan?.destination]
    .map((value) =>
      String(value || "")
        .trim()
        .toLowerCase(),
    )
    .join("\n");

  return searchText.includes(keyword);
}

function hasComposerContent(target) {
  if (!target) {
    return false;
  }
  return Boolean(
    String(target.title || "").trim() ||
    String(target.content || "").trim() ||
    (Array.isArray(target.imageUrls) && target.imageUrls.length > 0),
  );
}

function readDraft() {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.exploreDraft);
    if (!raw) {
      return createEmptyComposer();
    }
    const parsed = JSON.parse(raw);
    return {
      title: String(parsed?.title || "").trim(),
      content: String(parsed?.content || "").trim(),
      imageUrls: Array.isArray(parsed?.imageUrls) ? parsed.imageUrls.filter(Boolean) : [],
    };
  } catch {
    return createEmptyComposer();
  }
}

function persistDraft(nextDraft) {
  storedDraft.value = {
    title: String(nextDraft?.title || "").trim(),
    content: String(nextDraft?.content || "").trim(),
    imageUrls: Array.isArray(nextDraft?.imageUrls) ? nextDraft.imageUrls.filter(Boolean) : [],
  };
  localStorage.setItem(
    STORAGE_KEYS.exploreDraft,
    JSON.stringify(storedDraft.value),
  );
}

function clearDraft(shouldNotify = true) {
  storedDraft.value = createEmptyComposer();
  localStorage.removeItem(STORAGE_KEYS.exploreDraft);
  if (shouldNotify) {
    notify("草稿已清空。", "info");
  }
}

function notify(message, type = "info") {
  emit("notify", { message, type });
}

function formatTime(value) {
  if (!value) {
    return "刚刚";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatCompactNumber(value) {
  const normalized = Number(value) || 0;
  if (normalized >= 1_000_000) {
    return `${trimCompactDecimal(normalized / 1_000_000)}m`;
  }
  if (normalized >= 1_000) {
    return `${trimCompactDecimal(normalized / 1_000)}k`;
  }
  return String(normalized);
}

function trimCompactDecimal(value) {
  if (value >= 10) {
    return String(Math.round(value));
  }
  return String(Math.round(value * 10) / 10).replace(/\.0$/, "");
}

function requireAuth() {
  emit("require-auth");
}

function canDeletePost(post) {
  return Boolean(isMinePostsMode.value && (props.isAdmin || post?.isOwn));
}

function canDeleteComment(post, comment) {
  return Boolean(props.isAdmin || post?.isOwn || comment?.own);
}

function canDeleteRoute(routePlan) {
  if (routePlan?.route) {
    return Boolean(
      isMineRoutesMode.value &&
        (props.isAdmin || routePlan?.isOwn || routePlan?.own),
    );
  }
  return Boolean(props.isAdmin || routePlan?.routePlanId);
}

function handlePostContextMenu(post, event) {
  if (!canDeletePost(post)) {
    return;
  }
  event?.preventDefault();
  emit("delete-post", post);
}

function handleCommentContextMenu(post, comment) {
  if (!canDeleteComment(post, comment)) {
    return;
  }
  emit("delete-comment", {
    postId: post.id,
    commentId: comment.id,
    content: comment.content,
  });
}

function handleRouteContextMenu(routePlan) {
  if (!canDeleteRoute(routePlan)) {
    return;
  }
  if (routePlan?.route) {
    emit("delete-route", routePlan);
    return;
  }
  emit("delete-route", {
    routePlanId: routePlan.routePlanId,
    title: routePlan.title,
  });
}

function openDetail(postId) {
  routePublishPanelOpen.value = false;
  composerPanelOpen.value = false;
  detailImageIndex.value = 0;
  selectedPostId.value = postId;
  viewMode.value = "detail";
}

function closeDetail() {
  selectedPostId.value = null;
  routePublishPanelOpen.value = false;
  composerPanelOpen.value = false;
  detailImageIndex.value = 0;
  viewMode.value = "list";
}

function openOwnRouteDetail(routePlanId) {
  const normalizedId = Number(routePlanId) || 0;
  if (!normalizedId) {
    return;
  }
  routePublishPanelOpen.value = false;
  composerPanelOpen.value = false;
  selectedPostId.value = null;
  selectedRoutePlanId.value = normalizedId;
  viewMode.value = "route-detail";
  emit("select-route-plan-detail", normalizedId);
}

function openOwnRoutePostDetail(postId) {
  const normalizedId = Number(postId) || 0;
  if (!normalizedId) {
    return;
  }
  routePublishPanelOpen.value = false;
  composerPanelOpen.value = false;
  selectedRoutePlanId.value = null;
  selectedPostId.value = normalizedId;
  viewMode.value = "route-detail";
}

function openSharedRouteDetail(postId) {
  const normalizedId = Number(postId) || 0;
  if (!normalizedId) {
    return;
  }
  routePublishPanelOpen.value = false;
  composerPanelOpen.value = false;
  selectedRoutePlanId.value = null;
  selectedPostId.value = normalizedId;
  viewMode.value = "route-detail";
  emit("record-route-click", normalizedId);
}

function closeRouteDetail() {
  selectedRoutePlanId.value = null;
  selectedPostId.value = null;
  routePublishPanelOpen.value = false;
  composerPanelOpen.value = false;
  viewMode.value = "list";
}

function toggleRoutePublishPanel() {
  routePublishPanelOpen.value = !routePublishPanelOpen.value;
}

function closeRoutePublishPanel() {
  routePublishPanelOpen.value = false;
}

function handleFloatPanelsOutsidePointerDown(event) {
  const target = event.target;
  if (composerPanelOpen.value) {
    if (
      target instanceof Element &&
      (target.closest(".explore-composer-float-panel") ||
        target.closest(".explore-publish-fab"))
    ) {
      return;
    }
    closeComposerPanel();
  }
  if (routePublishPanelOpen.value) {
    if (
      target instanceof Element &&
      (target.closest(".explore-route-float-panel") ||
        target.closest(".explore-publish-fab"))
    ) {
      return;
    }
    closeRoutePublishPanel();
  }
}

function toggleComposerPanel() {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }

  if (composerPanelOpen.value) {
    closeComposerPanel();
    return;
  }

  if (hasStoredDraft.value) {
    composer.value = {
      title: storedDraft.value.title,
      content: storedDraft.value.content,
      imageUrls: [...(Array.isArray(storedDraft.value.imageUrls) ? storedDraft.value.imageUrls : [])],
    };
  } else {
    composer.value = createEmptyComposer();
  }

  composerPanelOpen.value = true;
}

async function closeComposerPanel() {
  if (hasComposerContent(composer.value)) {
    const shouldExit = await props.requestConfirm({
      title: "退出编辑",
      message: "当前内容尚未发布，确认退出吗？退出后会自动保存到草稿箱。",
      confirmLabel: "退出编辑",
    });
    if (!shouldExit) return;
    persistDraft(composer.value);
  }
  composerPanelOpen.value = false;
}

function triggerImageSelect() {
  imageInputRef.value?.click();
}

function removeImage(index) {
  if (!Array.isArray(composer.value.imageUrls)) return;
  composer.value.imageUrls.splice(index, 1);
  if (imageInputRef.value) {
    imageInputRef.value.value = "";
  }
}

function handleImageChange(event) {
  const files = event.target?.files;
  if (!files || !files.length) return;

  const maxSize = 2 * 1024 * 1024;
  const remaining = 10 - (composer.value.imageUrls?.length || 0);
  if (remaining <= 0) {
    notify("最多上传10张图片。", "error");
    event.target.value = "";
    return;
  }

  const filesToRead = Array.from(files).slice(0, remaining);
  let readCount = 0;

  filesToRead.forEach((file) => {
    if (file.size > maxSize) {
      notify(`图片"${file.name}"不能超过 2MB。`, "error");
      readCount++;
      if (readCount === filesToRead.length && imageInputRef.value) {
        imageInputRef.value.value = "";
      }
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (!Array.isArray(composer.value.imageUrls)) {
        composer.value.imageUrls = [];
      }
      composer.value.imageUrls.push(String(reader.result || ""));
      readCount++;
      if (readCount >= filesToRead.length && imageInputRef.value) {
        imageInputRef.value.value = "";
      }
    };
    reader.onerror = () => {
      notify(`图片"${file.name}"读取失败。`, "error");
      readCount++;
      if (readCount >= filesToRead.length && imageInputRef.value) {
        imageInputRef.value.value = "";
      }
    };
    reader.readAsDataURL(file);
  });
}

function submitPost() {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }

  const title = String(composer.value.title || "").trim();
  const content = String(composer.value.content || "").trim();
  if (!title) {
    notify("请先填写帖子标题。", "error");
    return;
  }
  if (!content) {
    notify("请先填写帖子正文。", "error");
    return;
  }

  emit("publish-post", {
    title,
    content,
    imageUrls: Array.isArray(composer.value.imageUrls) ? composer.value.imageUrls.filter(Boolean) : [],
  });
}

function handleLike(postId) {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }
  emit("toggle-like", postId);
}

function handleFavorite(postId) {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }
  emit("toggle-favorite", postId);
}

function handleCommentLike(postId, commentId) {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }
  emit("toggle-comment-like", { postId, commentId });
}

function submitComment(postId) {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }
  const content = String(commentDrafts.value[postId] || "").trim();
  if (!content) {
    return;
  }
  emit("add-comment", {
    postId,
    content,
  });
  commentDrafts.value[postId] = "";
}


function routeDateRangeLabel(route) {
  if (route?.startDate && route?.endDate) {
    return `${String(route.startDate).replaceAll("-", "/")} - ${String(route.endDate).replaceAll("-", "/")}`;
  }
  return `${route?.days || 1} 天`;
}

function handleImportRoute(post) {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }
  emit("import-route", post);
}

function handleShareRoute(routePlanId) {
  if (!props.isAuthenticated) {
    requireAuth();
    return;
  }
  routePublishPanelOpen.value = false;
  emit("share-route", routePlanId);
}
</script>

<style scoped>
.explore-workspace {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  padding: 20px 24px 28px;
  overflow: auto;
}

.explore-workspace--detail {
  overflow: hidden;
}

.explore-feed-board {
  display: grid;
  gap: 18px;
  width: 100%;
  align-content: start;
}

.explore-route-float-panel,
.explore-own-route-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  background:
    radial-gradient(
      circle at top right,
      rgba(14, 165, 233, 0.08),
      transparent 34%
    ),
    linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.96) 0%,
      rgba(248, 250, 252, 0.94) 100%
    );
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
}

.explore-route-float-head,
.explore-route-card-badges,
.explore-route-card-meta,
.explore-own-route-copy {
  display: grid;
  gap: 8px;
}

.explore-route-float-panel {
  position: fixed;
  right: 36px;
  bottom: 108px;
  z-index: 9;
  display: grid;
  gap: 14px;
  grid-template-rows: auto auto minmax(0, 1fr);
  width: min(420px, calc(100vw - 48px));
  height: min(720px, calc(100vh - 132px));
  padding: 16px;
  overflow: hidden;
}

.explore-route-float-head {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
}

.explore-route-float-search {
  width: 100%;
}

.explore-own-route-copy h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.2;
  letter-spacing: -0.04em;
}

.explore-route-board-meta,
.explore-route-card-meta {
  color: #64748b;
  font-size: 13px;
}

.explore-route-float-list {
  display: grid;
  gap: 18px;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}

.explore-route-float-list .explore-own-route-card {
  height: 236px;
  overflow: hidden;
}

.explore-route-float-list .explore-own-route-copy {
  min-height: 0;
  overflow: hidden;
}

.explore-route-float-list .explore-own-route-copy h2,
.explore-route-float-list .explore-own-route-copy p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
}

.explore-route-float-list .explore-own-route-copy h2 {
  -webkit-line-clamp: 2;
}

.explore-route-float-list .explore-own-route-copy p {
  max-height: calc(14px * 1.75 * 2);
  -webkit-line-clamp: 2;
}

.explore-own-route-copy p {
  margin: 0;
  color: #475569;
  line-height: 1.75;
}

.explore-composer-float-panel {
  position: fixed;
  right: 36px;
  bottom: 108px;
  z-index: 9;
  display: grid;
  gap: 14px;
  grid-template-rows: auto minmax(0, 1fr);
  width: min(420px, calc(100vw - 48px));
  height: min(720px, calc(100vh - 132px));
  padding: 16px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  background:
    radial-gradient(
      circle at top right,
      rgba(249, 115, 22, 0.1),
      transparent 34%
    ),
    linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.96) 0%,
      rgba(248, 250, 252, 0.94) 100%
    );
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
}

.explore-composer-float-body {
  display: grid;
  gap: 18px;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
  align-content: start;
}

.explore-composer-float-panel .explore-upload-panel {
  padding: 14px;
}

.explore-composer-float-panel .explore-upload-preview {
  aspect-ratio: 16 / 8;
}

.explore-composer-float-panel .explore-field textarea {
  min-height: 160px;
}

.explore-route-heat-badge {
  position: absolute;
  right: 10px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: 999px;
  color: #f8fafc;
  background: rgba(15, 23, 42, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(10px);
}

.explore-route-heat-badge svg {
  width: 15px;
  height: 15px;
  color: #fb923c;
}

.explore-route-heat-badge strong {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.explore-route-days-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  padding: 8px 12px;
  border-radius: 999px;
  color: #155e75;
  font-size: 12px;
  font-weight: 700;
  background: rgba(6, 182, 212, 0.12);
}

.date-range-pill {
  max-width: 150px;
  overflow: hidden;
  white-space: nowrap;
}

.date-range-pill:hover {
  animation: date-range-marquee 4s linear infinite alternate;
}

@keyframes date-range-marquee {
  from {
    text-indent: 0;
  }
  to {
    text-indent: -46px;
  }
}

.explore-own-route-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.explore-own-route-card:hover,
.explore-own-route-card:focus-visible {
  transform: translateY(-2px);
  border-color: rgba(14, 165, 233, 0.24);
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.12);
  outline: none;
}

.explore-own-route-copy {
  min-width: 0;
}

.explore-route-detail-layout {
  display: grid;
  gap: 20px;
  width: 100%;
  min-height: 0;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  align-items: stretch;
}

.explore-route-detail-map,
.explore-route-detail-copy-card {
  display: grid;
  gap: 16px;
  min-height: 0;
  padding: 20px;
  border-radius: 28px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background:
    radial-gradient(
      circle at top right,
      rgba(14, 165, 233, 0.08),
      transparent 34%
    ),
    linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.96) 0%,
      rgba(248, 250, 252, 0.94) 100%
    );
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
}

.explore-route-detail-map {
  align-content: stretch;
  grid-template-rows: auto minmax(0, 1fr);
}

.explore-route-detail-map :deep(.route-map-shell) {
  min-height: 520px;
  height: 100%;
}

.explore-route-detail-map :deep(.route-map-canvas) {
  min-height: 100%;
}

.explore-route-detail-head,
.explore-route-detail-copy-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.explore-route-detail-copy-head > div {
  min-width: 0;
  flex: 1;
}

.explore-route-detail-head p,
.explore-route-detail-copy-head h1,
.explore-route-detail-section h2,
.explore-route-detail-day header strong,
.explore-route-detail-point strong {
  margin: 0;
}

.explore-route-detail-head p {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.explore-route-detail-head strong,
.explore-route-detail-copy-head h1 {
  color: #0f172a;
  letter-spacing: -0.04em;
}

.explore-route-detail-copy-card {
  overflow: auto;
}

.explore-route-detail-copy {
  display: grid;
  gap: 18px;
  align-content: start;
}

.explore-route-detail-summary {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.explore-route-detail-meta-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.explore-route-detail-meta-strip span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.05);
}

.explore-route-detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.explore-route-detail-meta div,
.explore-route-detail-day,
.explore-route-detail-point {
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.86);
}

.explore-route-detail-meta div {
  display: grid;
  gap: 6px;
  padding: 14px;
}

.explore-route-detail-meta dt {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.explore-route-detail-meta dd {
  margin: 0;
  color: #0f172a;
  line-height: 1.6;
}

.explore-route-detail-section {
  display: grid;
  gap: 12px;
}

.explore-route-detail-section h2 {
  color: #0f172a;
  font-size: 18px;
}

.explore-route-detail-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.explore-route-detail-tips span {
  display: inline-flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 999px;
  color: #475569;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.explore-route-detail-days,
.explore-route-detail-point-list {
  display: grid;
  gap: 12px;
}

.explore-route-detail-day {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.explore-route-detail-day header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.explore-route-detail-day header span {
  color: #64748b;
  font-size: 13px;
}

.explore-route-detail-point {
  display: grid;
  gap: 10px;
  grid-template-columns: auto minmax(0, 1fr);
  padding: 14px;
}

.explore-route-detail-point-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 999px;
  color: #155e75;
  font-size: 13px;
  font-weight: 700;
  background: rgba(6, 182, 212, 0.12);
}

.explore-route-detail-point p {
  margin: 6px 0 0;
  color: #475569;
  line-height: 1.7;
}

.explore-list-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.explore-segment-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.explore-segment-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
}

.explore-segment-group.secondary {
  background: rgba(249, 115, 22, 0.08);
}

.explore-segment-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 10px 16px;
  border-radius: 999px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.74);
  transition:
    background 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.explore-segment-button.active {
  color: #f8fafc;
  background: #0f172a;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.18);
}

.explore-list-heading {
  display: grid;
  gap: 6px;
}

.explore-list-eyebrow {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.explore-list-heading strong {
  color: #0f172a;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.explore-search-box {
  position: relative;
  width: min(360px, 100%);
}

.explore-search-box input {
  padding-left: 44px;
  border-color: rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.96);
}

.explore-search-box input::placeholder {
  color: #94a3b8;
}

.explore-search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  width: 18px;
  height: 18px;
  color: #94a3b8;
  transform: translateY(-50%);
}

.explore-frame-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.explore-frame-title {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.explore-frame-bar.with-back {
  display: grid;
  justify-items: start;
  gap: 10px;
  align-content: start;
  margin-bottom: 22px;
}

.explore-nav-button {
  display: inline-flex;
}

.explore-back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border-radius: 999px;
  color: #0f172a;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  justify-self: start;
  align-self: start;
}

.explore-back-button svg,
.explore-publish-fab svg,
.explore-action-chip svg,
.explore-comment-like svg {
  width: 16px;
  height: 16px;
}

.explore-post-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr);
}

.explore-post-tile,
.explore-state-card,
.explore-detail-main,
.explore-detail-side,
.explore-upload-panel {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  background:
    radial-gradient(
      circle at top right,
      rgba(249, 115, 22, 0.1),
      transparent 34%
    ),
    linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.96) 0%,
      rgba(249, 250, 251, 0.94) 100%
    );
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
}

.explore-post-tile {
  display: grid;
  gap: 14px;
  grid-template-rows: 176px minmax(0, 1fr) auto;
  height: 386px;
  min-width: 0;
  padding: 14px;
  overflow: hidden;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.explore-post-tile:hover {
  transform: translateY(-3px);
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.12);
}

.explore-route-post-tile:focus-visible {
  outline: none;
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.12);
}

.explore-post-cover,
.explore-detail-cover,
.explore-upload-preview {
  overflow: hidden;
  border-radius: 22px;
  background: rgba(15, 23, 42, 0.04);
}

.explore-post-cover {
  position: relative;
  height: 176px;
}

.explore-post-cover img,
.explore-detail-cover img,
.explore-upload-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.explore-post-copy {
  display: grid;
  gap: 8px;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.explore-post-copy h2,
.explore-detail-copy h1 {
  margin: 0;
  color: #0f172a;
  letter-spacing: -0.04em;
}

.explore-post-copy h2 {
  display: -webkit-box;
  font-size: 20px;
  line-height: 1.2;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.explore-post-copy p,
.explore-detail-content,
.explore-route-summary,
.explore-comment-item p,
.explore-state-card span {
  margin: 0;
  color: #475569;
  line-height: 1.75;
}

.explore-post-copy p {
  display: -webkit-box;
  font-size: 14px;
  max-height: calc(14px * 1.75 * 3);
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.explore-post-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.explore-post-foot span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.explore-state-card {
  display: grid;
  gap: 8px;
  padding: 24px;
}

.explore-state-card strong,
.explore-comments-head strong,
.explore-route-panel-head strong {
  color: #0f172a;
}

.explore-publish-fab {
  position: fixed;
  right: 36px;
  bottom: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 999px;
  color: #ecfdf5;
  cursor: pointer;
  background: linear-gradient(135deg, #059669 0%, #34d399 100%);
  box-shadow: 0 22px 36px rgba(5, 150, 105, 0.28);
  z-index: 8;
}

.explore-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.85fr);
  gap: 20px;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.explore-detail-main,
.explore-detail-side {
  display: grid;
  gap: 18px;
  padding: 18px;
  min-height: 0;
}

.explore-detail-main {
  overflow-y: auto;
  align-content: start;
}

.explore-detail-cover {
  aspect-ratio: 16 / 9;
}

.explore-carousel {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.explore-carousel img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.explore-carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 999px;
  color: #0f172a;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.14);
  transition: opacity 0.2s ease;
}

.explore-carousel-arrow:disabled {
  opacity: 0.3;
  cursor: default;
}

.explore-carousel-arrow.prev {
  left: 10px;
}

.explore-carousel-arrow.next {
  right: 10px;
}

.explore-carousel-arrow svg {
  width: 14px;
  height: 14px;
}

.explore-carousel-dots {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 2;
}

.explore-carousel-dot {
  width: 8px;
  height: 8px;
  padding: 0;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.64);
  transition: background 0.2s ease, transform 0.2s ease;
}

.explore-carousel-dot.active {
  background: #fff;
  transform: scale(1.3);
}

.explore-detail-copy {
  display: grid;
  gap: 12px;
}

.explore-detail-tag,
.explore-route-points span {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.explore-detail-tag {
  color: #9a3412;
  background: rgba(249, 115, 22, 0.12);
}

.explore-detail-meta,
.explore-route-panel-head,
.explore-upload-head,
.explore-comment-meta,
.explore-detail-actions,
.explore-upload-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.explore-detail-meta {
  flex-wrap: wrap;
  color: #64748b;
  font-size: 13px;
}

.explore-detail-content {
  white-space: pre-wrap;
}

.explore-route-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.035);
}

.explore-route-panel-head p {
  margin: 0 0 6px;
  color: #64748b;
}

.explore-route-points {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.explore-route-points span {
  color: #475569;
  background: rgba(255, 255, 255, 0.88);
}

.explore-detail-side {
  grid-template-rows: auto minmax(0, 1fr) auto auto;
}

.explore-comments-head {
  display: grid;
  gap: 6px;
}

.explore-comments-head span,
.explore-comment-meta span {
  color: #64748b;
  font-size: 13px;
}

.explore-comment-stack {
  display: grid;
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
  align-content: start;
}

.explore-comment-item,
.explore-comment-empty {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.88);
}

.explore-comment-item.own {
  border-color: rgba(249, 115, 22, 0.2);
  background: rgba(255, 247, 237, 0.88);
}

.explore-comment-like,
.explore-action-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  background: rgba(15, 23, 42, 0.05);
}

.explore-comment-like.active,
.explore-action-chip.active {
  color: #9a3412;
  background: rgba(249, 115, 22, 0.14);
}

.explore-comment-composer {
  display: grid;
  gap: 12px;
}

.explore-field {
  display: grid;
  gap: 10px;
}

.explore-field span,
.explore-upload-head span {
  color: #334155;
  font-weight: 700;
}

.explore-field textarea {
  min-height: 220px;
  resize: vertical;
}

.explore-upload-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.explore-file-input {
  display: none;
}

.explore-upload-preview {
  aspect-ratio: 16 / 8;
}

.explore-upload-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
}

.explore-upload-grid-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.04);
}

.explore-upload-grid-item img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.explore-upload-remove-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 999px;
  color: #fff;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  background: rgba(15, 23, 42, 0.72);
  transition: background 0.2s ease;
}

.explore-upload-remove-btn:hover {
  background: rgba(239, 68, 68, 0.88);
}

.explore-submit-button {
  width: 100%;
  min-width: 0;
}

.compact {
  padding: 9px 14px;
  border-radius: 14px;
  font-size: 13px;
}

@media (min-width: 760px) {
  .explore-post-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1100px) {
  .explore-post-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1440px) {
  .explore-post-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .explore-detail-layout {
    grid-template-columns: minmax(0, 1fr);
    overflow-y: auto;
  }

  .explore-detail-main {
    overflow-y: visible;
  }

  .explore-detail-side {
    grid-template-rows: auto auto auto auto;
  }

  .explore-route-detail-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 959px) {
  .explore-workspace {
    padding: 14px 14px 92px;
  }

  .explore-list-toolbar {
    display: grid;
  }

  .explore-segment-toolbar,
  .explore-segment-group {
    width: 100%;
  }

  .explore-segment-group {
    justify-content: space-between;
  }

  .explore-segment-button {
    flex: 1;
  }

  .explore-list-heading strong {
    font-size: 21px;
  }

  .explore-publish-fab {
    right: 18px;
    bottom: 18px;
  }

  .explore-route-float-panel,
  .explore-composer-float-panel {
    right: 14px;
    bottom: 92px;
    width: calc(100vw - 28px);
    max-height: 62vh;
  }

  .explore-route-detail-map :deep(.route-map-shell) {
    min-height: 320px;
  }

  .explore-route-detail-head,
  .explore-route-detail-copy-head,
  .explore-route-detail-day header {
    display: grid;
    justify-content: stretch;
  }

  .explore-route-detail-meta {
    grid-template-columns: minmax(0, 1fr);
  }

  .explore-route-panel-head,
  .explore-upload-head,
  .explore-comment-meta,
  .explore-detail-actions,
  .explore-upload-actions {
    display: grid;
    justify-content: stretch;
  }
}
</style>
