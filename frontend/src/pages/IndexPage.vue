<template>
  <q-page class="index-page">
    <!-- Hero секция -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="container">
          <div class="row items-center justify-between">
            <div class="col-12 col-md-6">
              <div class="hero-text fade-in">
                <div class="badge-glow q-mb-md">
                  <q-badge color="primary" class="q-pa-sm">
                    <q-icon name="bolt" /> AI-Powered
                  </q-badge>
                </div>

                <h1 class="hero-title">
                  <span class="gradient-text">Лечебная</span><br>
                  <span class="gradient-text">Физическая</span><br>
                  <span class="gradient-text">Культура</span>
                </h1>

                <p class="hero-subtitle">
                  Умный помощник с компьютерным зрением для правильного выполнения упражнений
                </p>

                <div class="hero-actions">
                  <q-btn
                    to="/download"
                    label="Скачать приложение"
                    icon="download"
                    size="lg"
                    class="hero-btn primary full-width-mobile"
                    unelevated
                    no-caps
                  />
                  <q-btn
                    href="#features"
                    label="Узнать больше"
                    icon="arrow_downward"
                    size="lg"
                    class="hero-btn outline q-ml-md full-width-mobile"
                    flat
                    no-caps
                  />
                </div>

              </div>
            </div>

            <div class="col-12 col-md-6">
              <div class="hero-visual">
                <div class="phone-mockup floating">
                  <div class="phone-screen">
                    <div class="app-preview">
                      <div class="exercise-animation">
                        <div class="skeleton-hand"></div>
                        <div class="skeleton-points"></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="hero-wave">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 320" preserveAspectRatio="none">
          <path fill="white" fill-opacity="1" d="M0,96L48,112C96,128,192,160,288,160C384,160,480,128,576,122.7C672,117,768,139,864,154.7C960,171,1056,181,1152,170.7C1248,160,1344,128,1392,112L1440,96L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path>
        </svg>
      </div>
    </section>

    <!-- Секция с преимуществами -->
    <section id="features" class="features-section">
      <div class="container">
        <div class="section-header text-center q-mb-xl">
          <h2 class="section-title gradient-text">Почему выбирают LFK?</h2>
          <p class="section-subtitle">Современные технологии для вашего здоровья</p>
        </div>

        <div class="row q-col-gutter-lg q-col-gutter-xl-md">
          <div v-for="(feature, index) in features" :key="index" class="col-12 col-sm-6 col-md-4">
            <div class="feature-card-wrapper">
              <q-card flat class="feature-card">
                <q-card-section>
                  <div class="feature-icon-wrapper">
                    <q-icon :name="feature.icon" class="feature-icon" />
                  </div>
                  <h3 class="feature-title">{{ feature.title }}</h3>
                  <p class="feature-description">{{ feature.description }}</p>
                </q-card-section>
              </q-card>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Секция "Как это работает" -->
    <section class="how-it-works">
      <div class="container">
        <div class="section-header text-center q-mb-xl">
          <h2 class="section-title gradient-text">Как это работает</h2>
          <p class="section-subtitle">Три простых шага к правильным упражнениям</p>
        </div>

        <div class="row q-col-gutter-lg">
          <div v-for="(step, index) in steps" :key="index" class="col-12 col-md-4">
            <div class="step-item text-center">
              <div class="step-number">{{ index + 1 }}</div>
              <q-icon :name="step.icon" size="48px" color="primary" class="q-my-md" />
              <h3 class="text-h5">{{ step.title }}</h3>
              <p class="text-grey-7">{{ step.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA секция -->
    <section class="cta-section">
      <div class="container">
        <div class="cta-content text-center">
          <h2 class="cta-title">Готовы начать?</h2>
          <p class="cta-subtitle">Скачайте приложение и начните заниматься правильно уже сегодня</p>
          <q-btn
            to="/download"
            label="Скачать бесплатно"
            icon="download"
            size="xl"
            color="white"
            text-color="primary"
            class="cta-btn-glow"
            unelevated
            no-caps
          />
        </div>
      </div>
    </section>

    <!-- Футер -->
    <footer class="site-footer">
      <div class="container">
        <div class="row q-col-gutter-lg">
          <div class="col-12 col-md-4">
            <div class="footer-logo">
              <q-avatar size="50px" class="q-mb-sm">
                <img src="/logo/logo.svg" alt="LFK">
              </q-avatar>
              <h4>LFK</h4>
              <p>Умный помощник для лечебной физкультуры с компьютерным зрением</p>
            </div>
          </div>

          <div class="col-6 col-md-2">
            <h5>Продукт</h5>
            <ul>
              <li><a href="#features">Возможности</a></li>
              <li><a href="#how-it-works">Как работает</a></li>
              <li><a href="/download">Скачать</a></li>
            </ul>
          </div>

          <div class="col-6 col-md-2">
            <h5>Ресурсы</h5>
            <ul>
              <li><a href="https://github.com/gitmonstera/lfk" target="_blank" rel="noopener">GitHub</a></li>
              <li><a href="#">Документация</a></li>
              <li><a href="#">API</a></li>
            </ul>
          </div>

          <div class="col-12 col-md-4">
            <h5>Соцсети</h5>
            <div class="social-links">
              <a href="#" class="social-link" aria-label="Telegram"><q-icon name="fab fa-telegram" /></a>
              <a href="#" class="social-link" aria-label="VK"><q-icon name="fab fa-vk" /></a>
              <a href="https://github.com/gitmonstera/lfk" class="social-link" aria-label="GitHub" target="_blank" rel="noopener"><q-icon name="fab fa-github" /></a>
            </div>
          </div>
        </div>

        <div class="footer-bottom">
          <div class="row items-center justify-between">
            <div class="col-12 col-md-6 text-center text-md-left">
              <p class="copyright">© 2026 LFK. Лицензия MIT</p>
            </div>
            <div class="col-12 col-md-6 text-center text-md-right">
              <a href="#" class="footer-link">Политика конфиденциальности</a>
            </div>
          </div>
        </div>
      </div>
    </footer>
  </q-page>
</template>

<script setup>
const features = [
  {
    icon: 'videocam',
    title: 'Компьютерное зрение',
    description: 'Анализ движений в реальном времени с точностью 98%'
  },
  {
    icon: 'insights',
    title: 'Прогресс и аналитика',
    description: 'Детальная статистика ваших тренировок'
  },
  {
    icon: 'security',
    title: 'Защита данных',
    description: 'Видео не хранится на сервере, только обрабатываются'
  },
  {
    icon: 'update',
    title: 'Регулярные обновления',
    description: 'Новые упражнения каждый месяц'
  }
]

const steps = [
  {
    icon: 'smartphone',
    title: 'Установите приложение',
    description: 'Скачайте LFK с нашего сайта'
  },
  {
    icon: 'videocam',
    title: 'Начните тренировку',
    description: 'Направьте камеру на себя'
  },
  {
    icon: 'emoji_events',
    title: 'Достигайте целей',
    description: 'Следите за прогрессом'
  }
]
</script>

<style lang="scss" scoped>
.index-page {
  overflow-x: hidden;
}

// Hero секция
.hero-section {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  color: white;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .hero-content {
    position: relative;
    z-index: 2;
    padding: 120px 0 80px;
    width: 100%;
  }

  .badge-glow .q-badge {
    background: rgba(255, 255, 255, 0.15) !important;
    backdrop-filter: blur(5px);
    font-size: 1rem;
    border-radius: 20px;
  }

  .hero-title {
    font-size: clamp(2rem, 6vw, 4.5rem);
    font-weight: 800;
    line-height: 1.1;
    margin-bottom: 1.5rem;

    .gradient-text {
      background: linear-gradient(135deg, #fff, #f0f0ff);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  .hero-subtitle {
    font-size: clamp(1rem, 2vw, 1.25rem);
    opacity: 0.95;
    max-width: 550px;
    line-height: 1.6;
    margin-bottom: 2rem;
  }

  .hero-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-top: 2rem;

    @media (max-width: 600px) {
      flex-direction: column;
      gap: 12px;

      .full-width-mobile {
        width: 100%;
        margin-left: 0 !important;
      }
    }
  }

  .hero-btn {
    height: 56px;
    padding: 0 24px;
    font-weight: 600;
    letter-spacing: 0.5px;

    @media (min-width: 601px) {
      min-width: 200px;
    }

    &.primary {
      background: white;
      color: #667eea;
      border: none;

      &:hover {
        background: rgba(255, 255, 255, 0.9);
        transform: translateY(-2px);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
      }
    }

    &.outline {
      color: white;
      border: 2px solid rgba(255, 255, 255, 0.5);
      background: transparent;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        border-color: white;
        transform: translateY(-2px);
      }
    }
  }

  .stat-item {
    .stat-value {
      font-size: clamp(1.5rem, 3vw, 2rem);
      font-weight: 700;
      line-height: 1.2;
    }

    .stat-label {
      font-size: clamp(0.8rem, 1.5vw, 0.9rem);
      opacity: 0.8;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
  }

  .hero-wave {
    position: absolute;
    bottom: -60px;
    left: 0;
    width: 100%;
    line-height: 0;

    svg {
      display: block;
      width: 100%;
      height: auto;
      min-height: 50px;

      @media (max-width: 768px) {
        min-height: 30px;
      }
    }
  }
}

// Телефон
.phone-mockup {
  width: min(300px, 80%);
  height: auto;
  aspect-ratio: 1/2;
  background: #1a1a2e;
  border-radius: 40px;
  padding: 10px;
  margin: 0 auto;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.3);

  @media (max-width: 768px) {
    margin-top: 40px;
  }

  .phone-screen {
    width: 100%;
    height: 100%;
    background: #0a0a1f;
    border-radius: 32px;
    overflow: hidden;
  }

  .app-preview {
    padding: 15px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    .exercise-animation {
      background: linear-gradient(135deg, #667eea, #764ba2);
      width: 100%;
      height: 200px;
      border-radius: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 15px;
    }

    .skeleton-hand {
      width: 60px;
      height: 60px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 50%;
      animation: pulse 2s infinite;
    }

    .skeleton-points {
      width: 30px;
      height: 30px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 50%;
      animation: pulse 2s infinite 0.3s;
    }
  }
}

// Секция с преимуществами
.features-section {
  padding: 80px 20px;
  background: white;

  @media (max-width: 768px) {
    padding: 60px 16px;
  }

  .feature-card {
    height: 100%;
    padding: 30px 20px;
    border-radius: 24px;
    transition: all 0.3s ease;
    text-align: center;
    border: 1px solid rgba(0, 0, 0, 0.05);

    @media (max-width: 768px) {
      padding: 24px 16px;
    }

    &:hover {
      transform: translateY(-8px);
      box-shadow: 0 20px 40px rgba(102, 126, 234, 0.15);
      border-color: transparent;
    }

    .feature-icon-wrapper {
      width: 70px;
      height: 70px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 20px;

      @media (max-width: 768px) {
        width: 60px;
        height: 60px;

        .feature-icon {
          font-size: 30px !important;
        }
      }

      .feature-icon {
        font-size: 35px;
        color: white;
      }
    }

    .feature-title {
      font-size: 1.3rem;
      font-weight: 600;
      margin-bottom: 12px;

      @media (max-width: 768px) {
        font-size: 1.2rem;
      }
    }

    .feature-description {
      color: #666;
      line-height: 1.6;
      font-size: 0.95rem;
    }
  }
}

// Секция "Как это работает"
.how-it-works {
  padding: 80px 20px;
  background: #f8faff;

  @media (max-width: 768px) {
    padding: 60px 16px;
  }

  .step-item {
    padding: 30px 20px;
    background: white;
    border-radius: 24px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;
    height: 100%;

    @media (max-width: 768px) {
      padding: 24px 16px;
    }

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 20px 40px rgba(102, 126, 234, 0.1);
    }

    .step-number {
      width: 50px;
      height: 50px;
      line-height: 50px;
      text-align: center;
      font-size: 24px;
      font-weight: bold;
      color: white;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 50%;
      margin: 0 auto 20px;
    }

    .text-h5 {
      font-size: 1.2rem;
      font-weight: 600;
      margin-bottom: 10px;
    }

    .text-grey-7 {
      font-size: 0.95rem;
      line-height: 1.5;
    }
  }
}

// CTA секция
.cta-section {
  padding: 80px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;

  @media (max-width: 768px) {
    padding: 60px 16px;
  }

  .cta-content {
    max-width: 800px;
    margin: 0 auto;
  }

  .cta-title {
    font-size: clamp(2rem, 5vw, 3rem);
    font-weight: 700;
    margin-bottom: 20px;
  }

  .cta-subtitle {
    font-size: clamp(1rem, 2vw, 1.2rem);
    margin-bottom: 40px;
    opacity: 0.95;
    line-height: 1.6;
  }

  .cta-btn-glow {
    background: white;
    color: #667eea;
    font-size: 1.2rem;
    padding: 0 40px;
    height: 64px;
    font-weight: 600;
    letter-spacing: 0.5px;
    animation: pulse 2s infinite;

    @media (max-width: 600px) {
      width: 100%;
      padding: 0 20px;
      font-size: 1rem;
    }

    &:hover {
      transform: scale(1.05);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
    }
  }
}

// Футер
.site-footer {
  background: #0a0a1f;
  color: white;
  padding: 60px 20px 20px;

  @media (max-width: 768px) {
    padding: 40px 16px 20px;
  }

  .footer-logo {
    margin-bottom: 20px;

    h4 {
      font-size: 1.5rem;
      margin-bottom: 10px;
      color: white;
    }

    p {
      color: rgba(255, 255, 255, 0.7);
      line-height: 1.6;
      font-size: 0.95rem;
      max-width: 300px;
    }
  }

  h5 {
    font-size: 1.1rem;
    margin-bottom: 20px;
    color: white;
    font-weight: 600;
  }

  ul {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      margin-bottom: 12px;

      a {
        color: rgba(255, 255, 255, 0.7);
        text-decoration: none;
        transition: all 0.3s;
        font-size: 0.95rem;

        &:hover {
          color: white;
          padding-left: 5px;
        }
      }
    }
  }

  .social-links {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;

    .social-link {
      width: 44px;
      height: 44px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      transition: all 0.3s;
      font-size: 1.2rem;

      @media (max-width: 768px) {
        width: 40px;
        height: 40px;
      }

      &:hover {
        background: linear-gradient(135deg, #667eea, #764ba2);
        transform: translateY(-3px);
        box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
      }
    }
  }

  .footer-bottom {
    margin-top: 60px;
    padding-top: 30px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    @media (max-width: 768px) {
      margin-top: 40px;
      padding-top: 20px;
    }

    .copyright {
      color: rgba(255, 255, 255, 0.5);
      font-size: 0.9rem;
      margin: 0;
    }

    .footer-link {
      color: rgba(255, 255, 255, 0.5);
      text-decoration: none;
      font-size: 0.9rem;
      transition: color 0.3s;

      &:hover {
        color: white;
      }
    }
  }
}

// Общие стили для секций
.section-header {
  margin-bottom: 3rem;

  .section-title {
    font-size: clamp(1.8rem, 4vw, 2.5rem);
    font-weight: 700;
    margin-bottom: 15px;
  }

  .section-subtitle {
    font-size: clamp(1rem, 1.5vw, 1.1rem);
    color: #666;
    max-width: 600px;
    margin: 0 auto;
  }
}

// Анимации
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7); }
  70% { box-shadow: 0 0 0 15px rgba(102, 126, 234, 0); }
  100% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.floating {
  animation: float 6s ease-in-out infinite;
}

.fade-in {
  animation: fadeIn 1s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Утилиты
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
}

.full-width-mobile {
  @media (max-width: 600px) {
    width: 100%;
  }
}
</style>
