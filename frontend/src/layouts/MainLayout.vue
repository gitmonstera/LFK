<template>
  <q-layout view="hHh Lpr lFf" class="main-layout">
    <!-- Анимированный фон -->
    <div class="animated-bg">
      <div class="gradient-sphere sphere-1"></div>
      <div class="gradient-sphere sphere-2"></div>
      <div class="gradient-sphere sphere-3"></div>
    </div>

    <!-- Навбар -->
    <q-header :class="['glass-navbar', { 'scrolled': isScrolled }]">
      <q-toolbar class="toolbar-content">
        <q-btn
          flat
          round
          dense
          icon="menu"
          class="menu-btn text-white"
          @click="leftDrawerOpen = !leftDrawerOpen"
        />

        <q-toolbar-title class="row items-center">
          <q-avatar size="40px" class="logo-avatar">
            <img src="/logo/logo.svg" alt="LFK">
          </q-avatar>
          <span class="logo-text q-ml-sm">LFK</span>
        </q-toolbar-title>

        <!-- Десктопное меню -->
        <div class="desktop-menu row items-center q-gutter-md">
          <q-btn to="/" label="Главная" flat class="nav-btn" />
          <q-btn to="/download" label="Скачать" flat class="nav-btn" />
          <q-btn
            href="https://github.com/gitmonstera/lfk"
            label="GitHub"
            flat
            icon="fab fa-github"
            target="_blank"
            class="nav-btn"
          />
          <q-btn
            label="Начать тренировку"
            icon="fitness_center"
            color="primary"
            class="cta-btn q-ml-md"
            unelevated
            to="/download"
          />
        </div>

        <!-- Мобильное меню -->
        <div class="mobile-menu">
          <q-btn flat round dense icon="more_vert" class="text-white">
            <q-menu>
              <q-list style="min-width: 200px">
                <q-item clickable v-close-popup to="/">
                  <q-item-section avatar><q-icon name="home" /></q-item-section>
                  <q-item-section>Главная</q-item-section>
                </q-item>
                <q-item clickable v-close-popup to="/download">
                  <q-item-section avatar><q-icon name="download" /></q-item-section>
                  <q-item-section>Скачать</q-item-section>
                </q-item>
                <q-item clickable v-close-popup href="https://github.com/gitmonstera/lfk" target="_blank">
                  <q-item-section avatar><q-icon name="fab fa-github" /></q-item-section>
                  <q-item-section>GitHub</q-item-section>
                </q-item>
                <q-separator />
                <q-item clickable v-close-popup to="/download">
                  <q-item-section avatar><q-icon name="fitness_center" color="primary" /></q-item-section>
                  <q-item-section class="text-primary">Начать тренировку</q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
        </div>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      bordered
      :width="300"
      :breakpoint="500"
      class="glass-drawer"
    >
      <q-scroll-area class="fit">
        <q-list padding class="drawer-list">
          <div class="drawer-header text-center q-pa-md">
            <q-avatar size="80px" class="q-mb-md floating">
              <img src="/logo/logo.svg">
            </q-avatar>
            <div class="text-h6">LFK</div>
            <div class="text-caption text-grey-7">v1.0.0</div>
          </div>

          <q-item to="/" exact clickable v-ripple class="drawer-item">
            <q-item-section avatar><q-icon name="home" /></q-item-section>
            <q-item-section>
              <q-item-label>Главная</q-item-label>
              <q-item-label caption>Красивый лендинг</q-item-label>
            </q-item-section>
          </q-item>

          <q-item to="/download" clickable v-ripple class="drawer-item">
            <q-item-section avatar><q-icon name="download" /></q-item-section>
            <q-item-section>
              <q-item-label>Скачать</q-item-label>
              <q-item-label caption>Получить приложение</q-item-label>
            </q-item-section>
          </q-item>

          <q-separator class="q-my-md" />

          <q-item clickable v-ripple tag="a" href="https://github.com/gitmonstera/lfk" target="_blank" class="drawer-item">
            <q-item-section avatar><q-icon name="fab fa-github" /></q-item-section>
            <q-item-section>
              <q-item-label>GitHub</q-item-label>
              <q-item-label caption>Открытый исходный код</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-scroll-area>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>

    <!-- Кнопка наверх -->
    <q-page-sticky position="bottom-right" :offset="[18, 18]">
      <q-btn
        fab
        icon="arrow_upward"
        color="primary"
        @click="scrollToTop"
        v-show="showScrollButton"
        class="scroll-top-btn"
      />
    </q-page-sticky>
  </q-layout>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const leftDrawerOpen = ref(false)
const showScrollButton = ref(false)
const isScrolled = ref(false)

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleScroll = () => {
  showScrollButton.value = window.scrollY > 300
  isScrolled.value = window.scrollY > 50
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style lang="scss" scoped>
.main-layout {
  background: #0a0a1f;
  min-height: 100vh;
}

// Анимированный фон
.animated-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  overflow: hidden;

  .gradient-sphere {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.5;
    animation: float 20s infinite;

    &.sphere-1 {
      width: 600px;
      height: 600px;
      background: radial-gradient(circle at 30% 30%, #667eea, #764ba2);
      top: -200px;
      left: -200px;
      animation-delay: 0s;
    }

    &.sphere-2 {
      width: 800px;
      height: 800px;
      background: radial-gradient(circle at 70% 70%, #ff6b6b, #556270);
      bottom: -400px;
      right: -200px;
      animation-delay: -5s;
    }

    &.sphere-3 {
      width: 500px;
      height: 500px;
      background: radial-gradient(circle at 50% 50%, #4facfe, #00f2fe);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      animation-delay: -10s;
    }
  }
}

// Стеклянный навбар
.glass-navbar {
  background: rgba(10, 10, 31, 0.8) !important;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s;
  z-index: 1000;

  &.scrolled {
    background: rgba(10, 10, 31, 0.95) !important;
    box-shadow: 0 4px 30px rgba(0, 0, 0, 0.3);
  }

  .toolbar-content {
    max-width: 1400px;
    margin: 0 auto;
    width: 100%;
  }

  .logo-avatar {
    transition: transform 0.3s;
    &:hover { transform: rotate(360deg); }
  }

  .logo-text {
    font-size: 1.5rem;
    font-weight: 700;
    background: linear-gradient(135deg, #fff, #e0e0ff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .nav-btn {
    color: rgba(255, 255, 255, 0.8);
    font-weight: 500;
    transition: all 0.3s;

    &:hover {
      color: white;
      background: rgba(255, 255, 255, 0.1);
      transform: translateY(-2px);
    }
  }

  .cta-btn {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
      transition: left 0.5s;
    }

    &:hover::before { left: 100%; }
  }

  .desktop-menu {
    display: flex;
    @media (max-width: 768px) { display: none; }
  }

  .mobile-menu {
    display: none;
    @media (max-width: 768px) { display: block; }
  }
}

// Стеклянный drawer
.glass-drawer {
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(10px);

  .drawer-header {
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  }

  .drawer-item {
    border-radius: 8px;
    margin: 4px 8px;
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
      transform: translateX(5px);
    }
  }
}

.scroll-top-btn {
  animation: pulse 2s infinite;
  &:hover { animation: none; }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-30px, 20px) scale(0.9); }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7); }
  70% { box-shadow: 0 0 0 15px rgba(102, 126, 234, 0); }
  100% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0); }
}
</style>
