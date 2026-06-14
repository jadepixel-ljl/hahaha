<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import heroImage from './assets/sleepy-lamb-hero.png';

type Page = 'home' | 'ai';

type Project = {
  title: string;
  summary: string;
  meta: string;
  tone: 'pink' | 'mint' | 'butter';
};

const projects: Project[] = [
  {
    title: '个人作品集',
    summary: '整理阶段性的设计、开发与内容创作，让来访者能快速理解我的审美、表达和执行方式。',
    meta: 'Portfolio',
    tone: 'pink',
  },
  {
    title: '灵感札记',
    summary: '记录学习、观察和创作过程，把零散想法沉淀成可以反复回看的页面与文章。',
    meta: 'Journal',
    tone: 'mint',
  },
  {
    title: 'AI 对话小屋',
    summary: '接入独立部署的 Open WebUI，用统一配置的模型服务承载个人知识助手与日常问答。',
    meta: 'AI Space',
    tone: 'butter',
  },
];

const floatingLambs = Array.from({ length: 10 }, (_, index) => ({
  id: index,
  left: `${(index * 19 + 7) % 94}%`,
  top: `${(index * 29 + 12) % 78}%`,
  delay: `${(index % 5) * -1.15}s`,
  scale: 0.72 + (index % 3) * 0.1,
}));

const openWebUiUrl = import.meta.env.VITE_OPENWEBUI_URL?.trim() || 'http://127.0.0.1:3000';
const openWebUiFallbackUrl =
  import.meta.env.VITE_OPENWEBUI_FALLBACK_URL?.trim() || `${openWebUiUrl.replace(/\/$/, '')}/auth`;

function pageFromHash(): Page {
  return window.location.hash === '#/ai' || window.location.hash === '#ai' ? 'ai' : 'home';
}

const currentPage = ref<Page>(pageFromHash());

function navigate(page: Page) {
  currentPage.value = page;
  window.location.hash = page === 'ai' ? '#/ai' : '#/';
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleHashChange() {
  currentPage.value = pageFromHash();
}

onMounted(() => {
  window.addEventListener('hashchange', handleHashChange);
});

onUnmounted(() => {
  window.removeEventListener('hashchange', handleHashChange);
});
</script>

<template>
  <div class="site-shell">
    <div class="floating-lambs" aria-hidden="true">
      <span
        v-for="lamb in floatingLambs"
        :key="lamb.id"
        class="mini-lamb"
        :style="{
          left: lamb.left,
          top: lamb.top,
          animationDelay: lamb.delay,
          transform: `scale(${lamb.scale})`,
        }"
      >
        <span class="mini-lamb__wool"></span>
        <span class="mini-lamb__face"></span>
      </span>
    </div>

    <header class="site-header" aria-label="主导航">
      <button class="brand" type="button" @click="navigate('home')">
        <span class="brand__mark" aria-hidden="true"></span>
        <span>困困小羊</span>
      </button>
      <nav class="nav-links">
        <a v-if="currentPage === 'home'" href="#projects">作品</a>
        <a v-if="currentPage === 'home'" href="#contact">联系</a>
        <button type="button" :class="{ active: currentPage === 'ai' }" @click="navigate('ai')">
          AI 对话
        </button>
        <button v-if="currentPage === 'ai'" type="button" @click="navigate('home')">
          返回主页
        </button>
      </nav>
    </header>

    <main v-if="currentPage === 'home'">
      <section id="home" class="hero-section">
        <img class="hero-bg" :src="heroImage" alt="原创困困小羊在糖果草地和云朵中休息" />
        <div class="hero-overlay"></div>
        <div class="hero-content">
          <p class="eyebrow">Personal Site</p>
          <h1>把日常灵感，收进一间柔软的小屋</h1>
          <p class="hero-copy">
            这里收纳我的作品、记录和长期兴趣。页面保持轻盈、可爱，也更专注于真实内容和持续更新。
          </p>
          <div class="hero-actions" aria-label="主页操作">
            <a class="button button--primary" href="#projects">查看作品</a>
            <button class="button button--ghost" type="button" @click="navigate('ai')">进入 AI 小屋</button>
          </div>
        </div>
      </section>

      <section id="projects" class="section section--cream">
        <div class="section-heading">
          <p class="eyebrow">Featured</p>
          <h2>最近整理的内容</h2>
          <p>以清晰、耐看的方式呈现作品与记录，让每一次访问都有可停留的东西。</p>
        </div>
        <div class="project-grid">
          <article
            v-for="project in projects"
            :key="project.title"
            class="project-card"
            :class="`project-card--${project.tone}`"
          >
            <div class="project-card__top">
              <span class="project-card__cloud" aria-hidden="true"></span>
              <span class="project-card__tag">{{ project.meta }}</span>
            </div>
            <h3>{{ project.title }}</h3>
            <p>{{ project.summary }}</p>
          </article>
        </div>
      </section>

      <section class="section section--mint profile-section">
        <div class="profile-copy">
          <p class="eyebrow">About</p>
          <h2>温柔一点，也认真一点</h2>
          <p>
            我喜欢把复杂的事情整理得清楚，把普通的页面做得有一点记忆点。这个网站会持续沉淀个人项目、学习笔记和可公开分享的灵感。
          </p>
        </div>
        <div class="profile-note">
          <strong>现在开放</strong>
          <p>作品展示、留言联系、AI 对话小屋。</p>
        </div>
      </section>

      <section id="contact" class="section section--pink contact-section">
        <div class="contact-copy">
          <p class="eyebrow">Contact</p>
          <h2>给小羊留一颗星星</h2>
          <p>合作、交流、建议或只是路过打个招呼，都可以从这里开始。</p>
        </div>
        <form class="message-form">
          <label>
            <span>你的名字</span>
            <input type="text" placeholder="比如：云朵朋友" />
          </label>
          <label>
            <span>想说的话</span>
            <textarea rows="4" placeholder="写下一句想分享的话"></textarea>
          </label>
          <button type="button">发送留言</button>
        </form>
      </section>
    </main>

    <main v-else class="ai-page">
      <section class="ai-page-hero">
        <div>
          <p class="eyebrow">Open WebUI</p>
          <h1>AI 对话小屋</h1>
          <p class="hero-copy">
            这里是独立的 AI 对话页面。模型服务由站长统一配置，主页保持安静，聊天体验单独承载。
          </p>
        </div>
        <div class="ai-page-actions">
          <a class="button button--primary" :href="openWebUiUrl" target="_blank" rel="noreferrer">
            打开 AI 对话
          </a>
          <button class="button button--ghost" type="button" @click="navigate('home')">返回主页</button>
        </div>
      </section>

      <section class="ai-workspace" aria-label="AI 对话入口">
        <div class="ai-launch-card">
          <span class="ai-launch-card__mark" aria-hidden="true"></span>
          <p class="eyebrow">Chat Space</p>
          <h2>进入专属对话空间</h2>
          <p>
            Open WebUI 已作为独立服务部署，API Key 与模型连接都在服务端配置。访问者只需要登录授权账号，
            就能在完整界面里进行对话、管理历史和切换可用模型。
          </p>
          <div class="ai-launch-card__actions">
            <a class="button button--primary" :href="openWebUiUrl" target="_blank" rel="noreferrer">
              打开 Open WebUI
            </a>
            <a class="button button--soft" :href="openWebUiFallbackUrl" target="_blank" rel="noreferrer">
              备用入口
            </a>
          </div>
          <small>服务地址：{{ openWebUiUrl }}</small>
        </div>
      </section>
    </main>
  </div>
</template>
