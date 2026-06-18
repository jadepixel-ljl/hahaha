<script setup lang="ts">
import { computed, defineComponent, h, onMounted, reactive, ref, type PropType } from 'vue';
import heroImage from './assets/lazy-sheep-happy.jpg';
import villageGateImage from './assets/village-gate.png';
import villageHouseImage from './assets/village-house.png';
import { api, getToken, resolveAssetUrl, setToken, type Captcha, type Comment, type Post, type User } from './lib/api';

type AuthMode = 'login' | 'register';
type WorkspaceTab = 'feed' | 'profile' | 'admin';
type ReplyTarget = { postId: number; commentId: number; nickname: string };

const CommentTree = defineComponent({
  name: 'CommentTree',
  props: {
    comment: { type: Object as PropType<Comment>, required: true },
    postId: { type: Number, required: true },
    replyToId: { type: Number as PropType<number | null>, default: null },
    drafts: { type: Object as PropType<Record<string, string>>, required: true },
  },
  emits: {
    reply: (_target: ReplyTarget) => true,
    submit: (_postId: number) => true,
    delete: (_postId: number, _comment: Comment) => true,
  },
  setup(props, { emit }) {
    function avatarText(comment: Comment) {
      return (comment.author.nickname || comment.author.username || '?').slice(0, 1).toUpperCase();
    }

    function formatCommentTime(value: string) {
      return new Intl.DateTimeFormat('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(value));
    }

    function renderNode(comment: Comment): ReturnType<typeof h> {
      const draftKey = `reply-${comment.id}`;
      const replying = props.replyToId === comment.id;
      return h('div', { class: 'comment-node' }, [
        h('div', { class: 'comment-main' }, [
          h('div', { class: 'avatar-sm' }, [
            comment.author.avatarUrl
              ? h('img', { src: resolveAssetUrl(comment.author.avatarUrl), alt: '评论者头像' })
              : h('span', avatarText(comment)),
          ]),
          h('div', { class: 'comment-body' }, [
            h('div', { class: 'comment-meta' }, [
              h('strong', comment.author.nickname),
              h('span', formatCommentTime(comment.createdAt)),
            ]),
            h('p', { class: 'comment-text' }, comment.content),
            h('div', { class: 'comment-actions' }, [
              h(
                'button',
                {
                  type: 'button',
                  onClick: () =>
                    emit('reply', {
                      postId: props.postId,
                      commentId: comment.id,
                      nickname: comment.author.nickname,
                    }),
                },
                '回复',
              ),
              comment.canDelete
                ? h(
                    'button',
                    { type: 'button', class: 'danger', onClick: () => emit('delete', props.postId, comment) },
                    '删除',
                  )
                : null,
            ]),
            replying
              ? h('div', { class: 'comment-form nested' }, [
                  h('input', {
                    value: props.drafts[draftKey] || '',
                    placeholder: `回复 ${comment.author.nickname}`,
                    onInput: (event: Event) => {
                      props.drafts[draftKey] = (event.target as HTMLInputElement).value;
                    },
                  }),
                  h('button', { type: 'button', onClick: () => emit('submit', props.postId) }, '发送'),
                ])
              : null,
          ]),
        ]),
        comment.replies?.length
          ? h('div', { class: 'comment-children' }, comment.replies.map((reply) => renderNode(reply)))
          : null,
      ]);
    }

    return () => renderNode(props.comment);
  },
});

const openWebUiUrl = import.meta.env.VITE_OPENWEBUI_URL?.trim() || 'http://127.0.0.1:3000';

const user = ref<User | null>(null);
const posts = ref<Post[]>([]);
const commentsByPost = reactive<Record<number, Comment[]>>({});
const commentDrafts = reactive<Record<string, string>>({});
const replyTo = ref<ReplyTarget | null>(null);
const activeTab = ref<WorkspaceTab>('feed');
const authMode = ref<AuthMode>('login');
const authPanelOpen = ref(false);
const captcha = ref<Captcha | null>(null);
const errorMessage = ref('');
const noticeMessage = ref('');
const loading = ref(false);
const uploadBusy = ref(false);
const adminUsers = ref<User[]>([]);
const adminPosts = ref<Post[]>([]);
const adminComments = ref<Comment[]>([]);

const loginForm = reactive({
  username: '',
  password: '',
});

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  avatarUrl: '',
  captchaAnswer: '',
});

const profileForm = reactive({
  nickname: '',
  avatarUrl: '',
});

const postForm = reactive({
  content: '',
  imageUrls: [] as string[],
});

const isAdmin = computed(() => user.value?.role === 'ADMIN');
const canUseSpace = computed(() => !!user.value && user.value.status !== 'BANNED');
const greetingName = computed(() => user.value?.nickname || user.value?.username || '访客');
const postCount = computed(() => posts.value.length);
const totalLikes = computed(() => posts.value.reduce((sum, post) => sum + post.likeCount, 0));
const totalComments = computed(() => posts.value.reduce((sum, post) => sum + post.commentCount, 0));

onMounted(async () => {
  try {
    await refreshCaptcha();
  } catch {
    captcha.value = null;
  }

  if (getToken()) {
    try {
      user.value = await api.me();
      syncProfileForm();
      await refreshFeed();
    } catch {
      setToken('');
    }
  }
});

function clearMessage() {
  errorMessage.value = '';
  noticeMessage.value = '';
}

function showError(error: unknown) {
  errorMessage.value = error instanceof Error ? error.message : '操作失败，请稍后再试';
}

async function refreshCaptcha() {
  captcha.value = await api.captcha();
  registerForm.captchaAnswer = '';
}

function scrollToVillage() {
  document.getElementById('village-space')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function goToFeed() {
  activeTab.value = 'feed';
  authPanelOpen.value = false;
  scrollToVillage();
}

function goToProfile() {
  if (user.value) {
    activeTab.value = 'profile';
    authPanelOpen.value = false;
    scrollToVillage();
  } else {
    openAuthPanel('register');
  }
}

function openAuthPanel(mode: AuthMode = 'login') {
  authMode.value = mode;
  authPanelOpen.value = true;
}

function toggleAuthPanel(mode: AuthMode = 'login') {
  if (authPanelOpen.value && authMode.value === mode) {
    authPanelOpen.value = false;
    return;
  }
  openAuthPanel(mode);
}

async function handleLogin() {
  clearMessage();
  loading.value = true;
  try {
    const response = await api.login(loginForm);
    setToken(response.token);
    user.value = response.user;
    syncProfileForm();
    await refreshFeed();
    authPanelOpen.value = false;
    scrollToVillage();
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

async function handleRegister() {
  clearMessage();
  if (!captcha.value) {
    try {
      await refreshCaptcha();
    } catch (error) {
      showError(error);
      return;
    }
  }
  loading.value = true;
  try {
    const response = await api.register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname,
      avatarUrl: registerForm.avatarUrl,
      captchaToken: captcha.value?.token || '',
      captchaAnswer: registerForm.captchaAnswer,
    });
    setToken(response.token);
    user.value = response.user;
    syncProfileForm();
    await refreshFeed();
    authPanelOpen.value = false;
    noticeMessage.value = '注册成功，欢迎来到小羊村';
    scrollToVillage();
  } catch (error) {
    showError(error);
    await refreshCaptcha();
  } finally {
    loading.value = false;
  }
}

function logout() {
  setToken('');
  user.value = null;
  posts.value = [];
  adminUsers.value = [];
  adminPosts.value = [];
  adminComments.value = [];
  Object.keys(commentsByPost).forEach((key) => delete commentsByPost[Number(key)]);
  Object.keys(commentDrafts).forEach((key) => delete commentDrafts[key]);
  replyTo.value = null;
  activeTab.value = 'feed';
  authMode.value = 'login';
  authPanelOpen.value = false;
  noticeMessage.value = '已退出登录';
}

function syncProfileForm() {
  profileForm.nickname = user.value?.nickname || '';
  profileForm.avatarUrl = user.value?.avatarUrl || '';
}

async function refreshFeed() {
  if (!canUseSpace.value) {
    return;
  }
  posts.value = await api.posts();
}

async function publishPost() {
  clearMessage();
  if (!postForm.content.trim() && postForm.imageUrls.length === 0) {
    errorMessage.value = '写点内容或者上传一张图片吧';
    return;
  }
  loading.value = true;
  try {
    const post = await api.createPost({
      content: postForm.content.trim() || '分享了一组图片',
      imageUrls: postForm.imageUrls,
    });
    posts.value.unshift(post);
    postForm.content = '';
    postForm.imageUrls = [];
    noticeMessage.value = '动态已发布';
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

async function uploadPostImage(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }
  clearMessage();
  uploadBusy.value = true;
  try {
    const response = await api.uploadImage(file);
    postForm.imageUrls.push(response.url);
  } catch (error) {
    showError(error);
  } finally {
    uploadBusy.value = false;
  }
}

async function uploadAvatar(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }
  clearMessage();
  uploadBusy.value = true;
  try {
    const response = await api.uploadAvatar(file);
    if (authMode.value === 'register' && !user.value) {
      registerForm.avatarUrl = response.url;
    } else {
      profileForm.avatarUrl = response.url;
    }
  } catch (error) {
    showError(error);
  } finally {
    uploadBusy.value = false;
  }
}

async function saveProfile() {
  clearMessage();
  loading.value = true;
  try {
    user.value = await api.updateProfile({
      nickname: profileForm.nickname,
      avatarUrl: profileForm.avatarUrl,
    });
    syncProfileForm();
    await refreshFeed();
    noticeMessage.value = '资料已更新';
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

async function toggleLike(post: Post) {
  clearMessage();
  try {
    const response = post.likedByMe ? await api.unlikePost(post.id) : await api.likePost(post.id);
    post.likedByMe = response.liked;
    post.likeCount = response.likeCount;
  } catch (error) {
    showError(error);
  }
}

async function deletePost(post: Post) {
  clearMessage();
  try {
    await api.deletePost(post.id);
    posts.value = posts.value.filter((item) => item.id !== post.id);
    adminPosts.value = adminPosts.value.filter((item) => item.id !== post.id);
    noticeMessage.value = '动态已删除';
  } catch (error) {
    showError(error);
  }
}

async function loadComments(postId: number) {
  clearMessage();
  try {
    commentsByPost[postId] = await api.comments(postId);
  } catch (error) {
    showError(error);
  }
}

async function submitComment(postId: number) {
  clearMessage();
  const isReply = replyTo.value?.postId === postId;
  const key = isReply ? `reply-${replyTo.value?.commentId}` : `post-${postId}`;
  const content = (commentDrafts[key] || '').trim();
  if (!content) {
    errorMessage.value = '评论不能为空';
    return;
  }
  try {
    await api.createComment(postId, {
      content,
      parentId: isReply ? replyTo.value?.commentId : null,
    });
    commentDrafts[key] = '';
    replyTo.value = null;
    await loadComments(postId);
    const post = posts.value.find((item) => item.id === postId);
    if (post) {
      post.commentCount = countReplies(commentsByPost[postId] || []);
    }
  } catch (error) {
    showError(error);
  }
}

async function deleteComment(postId: number, comment: Comment) {
  clearMessage();
  try {
    await api.deleteComment(postId, comment.id);
    await loadComments(postId);
    const post = posts.value.find((item) => item.id === postId);
    if (post) {
      post.commentCount = countReplies(commentsByPost[postId] || []);
    }
    noticeMessage.value = '评论已删除';
  } catch (error) {
    showError(error);
  }
}

async function openAdmin() {
  activeTab.value = 'admin';
  scrollToVillage();
  await refreshAdmin();
}

async function refreshAdmin() {
  if (!isAdmin.value) {
    return;
  }
  clearMessage();
  try {
    const [users, allPosts, allComments] = await Promise.all([api.adminUsers(), api.adminPosts(), api.adminComments()]);
    adminUsers.value = users;
    adminPosts.value = allPosts;
    adminComments.value = allComments;
  } catch (error) {
    showError(error);
  }
}

async function updateUserStatus(target: User, status: 'ACTIVE' | 'BANNED') {
  clearMessage();
  try {
    const updated = await api.adminUpdateUserStatus(target.id, status);
    adminUsers.value = adminUsers.value.map((item) => (item.id === updated.id ? updated : item));
    noticeMessage.value = status === 'BANNED' ? '账号已封禁' : '账号已恢复';
  } catch (error) {
    showError(error);
  }
}

async function adminDeleteUser(target: User) {
  clearMessage();
  try {
    await api.adminDeleteUser(target.id);
    adminUsers.value = adminUsers.value.filter((item) => item.id !== target.id);
    await refreshAdmin();
    noticeMessage.value = '账号已删除';
  } catch (error) {
    showError(error);
  }
}

async function adminDeletePost(post: Post) {
  clearMessage();
  try {
    await api.adminDeletePost(post.id);
    adminPosts.value = adminPosts.value.filter((item) => item.id !== post.id);
    posts.value = posts.value.filter((item) => item.id !== post.id);
    noticeMessage.value = '动态已删除';
  } catch (error) {
    showError(error);
  }
}

async function adminDeleteComment(comment: Comment) {
  clearMessage();
  try {
    await api.adminDeleteComment(comment.id);
    adminComments.value = adminComments.value.filter((item) => item.id !== comment.id);
    if (commentsByPost[comment.postId]) {
      await loadComments(comment.postId);
    }
    await refreshFeed();
    noticeMessage.value = '评论已删除';
  } catch (error) {
    showError(error);
  }
}

function avatarText(target?: User | null) {
  return (target?.nickname || target?.username || '?').slice(0, 1).toUpperCase();
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function countReplies(comments: Comment[]): number {
  return comments.reduce((total, item) => total + 1 + countReplies(item.replies || []), 0);
}
</script>

<template>
  <div class="site-shell">
    <header class="site-header" aria-label="主导航">
      <button class="brand" type="button" @click="goToFeed">
        <span class="brand__mark" aria-hidden="true"></span>
        <span>小羊云朵里的个人空间</span>
      </button>
      <div class="header-actions">
        <nav class="nav-links">
          <button type="button" :class="{ active: activeTab === 'feed' }" @click="goToFeed">动态</button>
          <button v-if="isAdmin" type="button" :class="{ active: activeTab === 'admin' }" @click="openAdmin">后台</button>
          <a class="nav-link nav-link--ai" :href="openWebUiUrl" target="_blank" rel="noreferrer">AI</a>
        </nav>

        <div class="account-slot">
          <button
            v-if="!user"
            class="account-button"
            type="button"
            :class="{ active: authPanelOpen }"
            aria-haspopup="dialog"
            :aria-expanded="authPanelOpen"
            @click="toggleAuthPanel('login')"
          >
            登录
          </button>
          <button
            v-else
            class="account-avatar-button"
            type="button"
            :aria-label="`编辑 ${greetingName} 的个人信息`"
            @click="goToProfile"
          >
            <span class="account-avatar">
              <img v-if="user.avatarUrl" :src="resolveAssetUrl(user.avatarUrl)" alt="" />
              <span v-else>{{ avatarText(user) }}</span>
            </span>
          </button>
        </div>
      </div>
    </header>

    <main class="space-page">
      <section class="space-hero" aria-labelledby="hero-title">
        <div class="hero-background" aria-hidden="true">
          <img class="hero-background__image" :src="heroImage" alt="" />
        </div>

        <div class="space-hero__content">
          <p class="eyebrow">Lazy Sheep Edition</p>
          <h1 id="hero-title">懒羊羊的个人空间</h1>
          <p class="hero-copy">
            欢迎来到亮亮的草地小村。这里可以写动态、放照片，也可以随时去 AI 小屋聊聊天。
          </p>
          <div class="hero-actions">
            <button class="button button--primary" type="button" @click="goToFeed">进入牧场</button>
            <a class="button button--secondary" :href="openWebUiUrl" target="_blank" rel="noreferrer">去 AI 小屋</a>
          </div>
          <div class="hero-metrics">
            <span>{{ postCount }} 条动态</span>
            <span>{{ totalLikes }} 次点赞</span>
            <span>{{ user ? `欢迎回来，${greetingName}` : '先登录，再出发' }}</span>
          </div>
        </div>
      </section>

      <section class="feature-trail" aria-label="村庄入口">
        <button class="trail-card trail-card--feed" type="button" @click="goToFeed">
          <span class="trail-card__icon" aria-hidden="true">
            <img :src="heroImage" alt="" />
          </span>
          <strong>个人动态</strong>
          <span>像村里公告板一样发帖、留言、贴图。</span>
        </button>
        <a class="trail-card trail-card--ai" :href="openWebUiUrl" target="_blank" rel="noreferrer">
          <span class="trail-card__icon" aria-hidden="true">
            <img :src="villageHouseImage" alt="" />
          </span>
          <strong>AI 小屋</strong>
          <span>把聊天入口放进一座温暖的小房子里。</span>
        </a>
        <button class="trail-card trail-card--profile" type="button" @click="goToProfile">
          <span class="trail-card__icon" aria-hidden="true">
            <img :src="villageGateImage" alt="" />
          </span>
          <strong>资料设置</strong>
          <span>{{ user ? '整理你的头像和昵称' : '把头像、昵称和状态做成小羊村门牌。' }}</span>
        </button>
      </section>

      <section id="village-space" class="workspace">
        <section class="content-panel">
          <div class="notice-stack">
            <div v-if="errorMessage" class="alert alert--error">{{ errorMessage }}</div>
            <div v-if="noticeMessage" class="alert alert--success">{{ noticeMessage }}</div>
          </div>

          <template v-if="!user">
            <section class="welcome-panel panel">
              <div class="panel-ribbon">牧场公告板</div>
              <h2>欢迎来到牧场公告板</h2>
              <p>先逛逛这里，登录后就能发动态、换头像、留下今天的小心情。</p>
              <div class="welcome-grid">
                <article>
                  <strong>草地动态</strong>
                  <span>像村里公告板一样发帖、留言和贴图。</span>
                </article>
                <article>
                  <strong>小羊名片</strong>
                  <span>登录后把头像、昵称和状态整理成一张卡片。</span>
                </article>
                <article>
                  <strong>AI 小屋</strong>
                  <span>想聊天的时候，去小屋坐一会儿。</span>
                </article>
              </div>
            </section>
          </template>

          <template v-else-if="activeTab === 'feed'">
            <form class="composer panel" @submit.prevent="publishPost">
              <div class="panel-ribbon">牧场公告板</div>
              <textarea v-model="postForm.content" rows="4" placeholder="今天在草地上发生了什么？"></textarea>
              <div v-if="postForm.imageUrls.length" class="image-preview-grid">
                <figure v-for="image in postForm.imageUrls" :key="image">
                  <img :src="resolveAssetUrl(image)" alt="动态图片预览" />
                  <button type="button" @click="postForm.imageUrls = postForm.imageUrls.filter((item) => item !== image)">
                    移除
                  </button>
                </figure>
              </div>
              <div class="composer-actions">
                <label class="file-button">
                  <input type="file" accept="image/*" @change="uploadPostImage" />
                  上传图片
                </label>
                <button class="button button--primary" type="submit" :disabled="loading || uploadBusy">发布动态</button>
              </div>
            </form>

            <article v-for="post in posts" :key="post.id" class="post-card panel">
              <header class="post-header">
                <div class="avatar-md">
                  <img v-if="post.author.avatarUrl" :src="resolveAssetUrl(post.author.avatarUrl)" alt="动态作者头像" />
                  <span v-else>{{ avatarText(post.author) }}</span>
                </div>
                <div class="post-header__meta">
                  <strong>{{ post.author.nickname }}</strong>
                  <p>@{{ post.author.username }} · {{ formatTime(post.createdAt) }}</p>
                </div>
                <button v-if="post.canDelete" class="text-button danger" type="button" @click="deletePost(post)">删除</button>
              </header>
              <p class="post-content">{{ post.content }}</p>
              <div v-if="post.imageUrls.length" class="post-images">
                <img v-for="image in post.imageUrls" :key="image" :src="resolveAssetUrl(image)" alt="动态图片" />
              </div>
              <footer class="post-actions">
                <button type="button" :class="{ active: post.likedByMe }" @click="toggleLike(post)">
                  {{ post.likedByMe ? '已赞' : '点赞' }} · {{ post.likeCount }}
                </button>
                <button type="button" @click="loadComments(post.id)">评论 · {{ post.commentCount }}</button>
              </footer>

              <section class="comments">
                <div class="comment-form">
                  <input v-model="commentDrafts[`post-${post.id}`]" placeholder="写一条评论" />
                  <button type="button" @click="submitComment(post.id)">发送</button>
                </div>
                <template v-if="commentsByPost[post.id]">
                  <CommentTree
                    v-for="comment in commentsByPost[post.id]"
                    :key="comment.id"
                    :comment="comment"
                    :post-id="post.id"
                    :reply-to-id="replyTo?.commentId || null"
                    :drafts="commentDrafts"
                    @reply="replyTo = $event"
                    @submit="submitComment"
                    @delete="deleteComment"
                  />
                </template>
              </section>
            </article>
          </template>

          <template v-else-if="activeTab === 'profile'">
            <form class="profile-editor panel" @submit.prevent="saveProfile">
              <div class="panel-ribbon">资料小屋</div>
              <div class="profile-editor__header">
                <div class="avatar-xl">
                  <img v-if="user.avatarUrl" :src="resolveAssetUrl(user.avatarUrl)" alt="当前头像" />
                  <span v-else>{{ avatarText(user) }}</span>
                </div>
                <div class="profile-editor__meta">
                  <h2>{{ greetingName }}</h2>
                  <p class="muted">@{{ user.username }} · {{ user.status === 'ACTIVE' ? '正常' : '已封禁' }}</p>
                </div>
              </div>
              <label>
                <span>昵称</span>
                <input v-model="profileForm.nickname" required />
              </label>
              <label>
                <span>头像</span>
                <input type="file" accept="image/*" @change="uploadAvatar" />
              </label>
              <div v-if="profileForm.avatarUrl" class="avatar-preview">
                <img :src="resolveAssetUrl(profileForm.avatarUrl)" alt="头像预览" />
                <button type="button" @click="profileForm.avatarUrl = ''">清除头像</button>
              </div>
              <div class="profile-actions">
                <button class="button button--primary" type="submit" :disabled="loading || uploadBusy">保存资料</button>
                <button class="button button--secondary" type="button" @click="logout">退出登录</button>
              </div>
            </form>
          </template>

          <template v-else-if="activeTab === 'admin' && isAdmin">
            <div class="admin-panel panel">
              <div class="panel-ribbon">村务板</div>
              <div class="table-list">
                <div v-for="target in adminUsers" :key="target.id" class="table-row">
                  <div>
                    <strong>{{ target.nickname }}</strong>
                    <p>@{{ target.username }}</p>
                  </div>
                  <div class="row-actions">
                    <button
                      v-if="target.status === 'ACTIVE'"
                      type="button"
                      :disabled="target.role === 'ADMIN'"
                      @click="updateUserStatus(target, 'BANNED')"
                    >
                      封禁
                    </button>
                    <button v-else type="button" @click="updateUserStatus(target, 'ACTIVE')">恢复</button>
                    <button type="button" :disabled="target.role === 'ADMIN'" @click="adminDeleteUser(target)">删除账号</button>
                  </div>
                </div>
              </div>
            </div>

            <div class="admin-panel panel">
              <div class="panel-ribbon">动态板</div>
              <div class="table-list">
                <div v-for="post in adminPosts" :key="post.id" class="table-row">
                  <div>
                    <strong>{{ post.author.nickname }}</strong>
                    <p>{{ post.content }}</p>
                    <small>{{ formatTime(post.createdAt) }} · {{ post.likeCount }} 赞 · {{ post.commentCount }} 评论</small>
                  </div>
                  <div class="row-actions">
                    <button type="button" @click="adminDeletePost(post)">删除动态</button>
                  </div>
                </div>
              </div>
            </div>

            <div class="admin-panel panel">
              <div class="panel-ribbon">评论板</div>
              <div class="table-list">
                <div v-for="comment in adminComments" :key="comment.id" class="table-row">
                  <div>
                    <strong>{{ comment.author.nickname }}</strong>
                    <p>{{ comment.content }}</p>
                    <small>
                      动态 #{{ comment.postId }} ·
                      {{ comment.parentId ? `回复 #${comment.parentId}` : '主评论' }} ·
                      {{ formatTime(comment.createdAt) }}
                    </small>
                  </div>
                  <div class="row-actions">
                    <button type="button" @click="adminDeleteComment(comment)">删除评论</button>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </section>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="!user && authPanelOpen" class="auth-overlay" @click.self="authPanelOpen = false">
        <section class="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-modal-title">
          <div class="auth-modal__header">
            <div>
              <p class="auth-modal__eyebrow">小屋门牌</p>
              <h2 id="auth-modal-title">{{ authMode === 'login' ? '登录' : '注册' }}</h2>
            </div>
            <button type="button" class="auth-modal__close" aria-label="关闭登录界面" @click="authPanelOpen = false">
              ×
            </button>
          </div>

          <p class="auth-modal__lead">登录后就能发动态、整理头像和昵称。</p>

          <div class="auth-tabs">
            <button type="button" :class="{ active: authMode === 'login' }" @click="authMode = 'login'">登录</button>
            <button type="button" :class="{ active: authMode === 'register' }" @click="authMode = 'register'">注册</button>
          </div>

          <form v-if="authMode === 'login'" class="auth-form" @submit.prevent="handleLogin">
            <label>
              <span>账号</span>
              <input v-model="loginForm.username" autocomplete="username" required />
            </label>
            <label>
              <span>密码</span>
              <input v-model="loginForm.password" type="password" autocomplete="current-password" required />
            </label>
            <button class="button button--primary" type="submit" :disabled="loading">进入小羊村</button>
          </form>

          <form v-else class="auth-form" @submit.prevent="handleRegister">
            <label>
              <span>账号</span>
              <input v-model="registerForm.username" autocomplete="username" minlength="3" required />
            </label>
            <label>
              <span>密码</span>
              <input v-model="registerForm.password" type="password" autocomplete="new-password" minlength="6" required />
            </label>
            <label>
              <span>昵称</span>
              <input v-model="registerForm.nickname" />
            </label>
            <label>
              <span>头像</span>
              <input type="file" accept="image/*" @change="uploadAvatar" />
            </label>
            <div v-if="registerForm.avatarUrl" class="avatar-preview">
              <img :src="resolveAssetUrl(registerForm.avatarUrl)" alt="注册头像预览" />
              <span>头像已选择</span>
            </div>
            <label>
              <span>验证码 {{ captcha?.question }}</span>
              <input v-model="registerForm.captchaAnswer" inputmode="numeric" required />
            </label>
            <button class="button button--primary" type="submit" :disabled="loading || uploadBusy">注册并进入</button>
          </form>
        </section>
      </div>
    </Teleport>
  </div>
</template>
