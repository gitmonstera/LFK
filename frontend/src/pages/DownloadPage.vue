<template>
  <q-page class="download-page">
    <!-- Анимированный градиентный фон -->
    <div class="gradient-bg">
      <div class="gradient-sphere sphere-1"></div>
      <div class="gradient-sphere sphere-2"></div>
      <div class="gradient-sphere sphere-3"></div>
      <div class="gradient-sphere sphere-4"></div>
      <div class="noise-overlay"></div>
    </div>

    <!-- Hero секция с параллаксом -->
    <div class="hero-section">
      <q-parallax :height="400" :speed="0.5">
        <template v-slot:media>
          <div class="hero-bg"></div>
        </template>

        <template v-slot:content="scope">
          <div class="hero-content absolute column flex-center"
               :style="{
                 opacity: 0.5 + scope.percentScrolled * 0.5,
               }">
            <q-icon name="download" size="64px" class="q-mb-md floating" />
            <h1 class="text-h2 text-white text-center">Скачать приложение</h1>
            <p class="text-h6 text-white text-center">Выберите удобный способ</p>
          </div>
        </template>
      </q-parallax>
    </div>

    <!-- Детектор устройства -->
    <div class="device-detector q-pa-md text-center" v-if="showDeviceInfo">
      <q-banner :class="['device-banner', deviceClass]" rounded>
        <template v-slot:avatar>
          <q-icon :name="deviceIcon" size="md" />
        </template>
        {{ deviceMessage }}
        <template v-slot:action>
          <q-btn flat :color="deviceActionColor" :label="deviceActionLabel" @click="handleDeviceAction" />
        </template>
      </q-banner>
    </div>

    <!-- Основной контент -->
    <div class="main-content">
      <div class="container">
        <!-- Для мобильных устройств -->
        <div v-if="isMobile" class="mobile-section">
          <div class="text-center q-mb-xl">
            <h2 class="text-h4 gradient-text">📱 Мобильное приложение</h2>
            <p class="text-white text-opacity-80">Оптимизировано для вашего устройства</p>
          </div>

          <div class="row justify-center q-col-gutter-lg">
            <!-- Android -->
            <div class="col-12 col-md-6">
              <q-card flat class="download-card android-card glass-card">
                <q-card-section class="text-center">
                  <div class="platform-icon android">
                    <q-icon name="fab fa-android" size="64px" color="white" />
                  </div>
                  <h3 class="text-h4 q-mt-md text-white">Android</h3>
                  <p class="text-grey-3">Версия 1.0.0 • 25 MB</p>

                  <div class="requirements q-mt-md">
                    <q-chip size="sm" icon="android" class="chip-light" text-color="white">
                      Android 8.0+
                    </q-chip>
                    <q-chip size="sm" icon="memory" class="chip-light" text-color="white">
                      2 GB RAM
                    </q-chip>
                    <q-chip size="sm" icon="camera" class="chip-light" text-color="white">
                      Камера 5 MP
                    </q-chip>
                  </div>

                  <q-list dense class="features-list q-mt-md">
                    <q-item v-for="feature in androidFeatures" :key="feature">
                      <q-item-section avatar>
                        <q-icon name="check_circle" color="positive" size="20px" />
                      </q-item-section>
                      <q-item-section class="text-white">{{ feature }}</q-item-section>
                    </q-item>
                  </q-list>

                  <q-badge color="green" class="q-mt-md badge-glow">
                    <q-icon name="security" class="q-mr-xs" /> Проверено
                  </q-badge>
                </q-card-section>

                <q-card-actions align="center" class="q-pa-lg">
                  <q-btn
                    color="green"
                    size="xl"
                    label="Скачать APK"
                    icon="download"
                    :href="downloadUrl"
                    download
                    unelevated
                    class="download-btn full-width glossy"
                  />
                </q-card-actions>

                <q-card-section class="text-center q-pt-none">
                  <p class="text-caption text-grey-4">
                    <q-icon name="info" size="xs" class="q-mr-xs" />
                    SHA256: a1b2c3d4e5f6...
                  </p>
                </q-card-section>
              </q-card>
            </div>

            <!-- iOS (скоро) -->
            <div class="col-12 col-md-6">
              <q-card flat class="download-card ios-card glass-card">
                <q-card-section class="text-center">
                  <div class="platform-icon ios">
                    <q-icon name="fab fa-apple" size="64px" color="white" />
                  </div>
                  <h3 class="text-h4 q-mt-md text-white">iOS</h3>
                  <p class="text-grey-3">Скоро в App Store</p>

                  <div class="requirements q-mt-md">
                    <q-chip size="sm" icon="apple" class="chip-light" text-color="white">
                      iOS 14.0+
                    </q-chip>
                    <q-chip size="sm" icon="memory" class="chip-light" text-color="white">
                      2 GB RAM
                    </q-chip>
                    <q-chip size="sm" icon="camera" class="chip-light" text-color="white">
                      Камера 12 MP
                    </q-chip>
                  </div>

                  <q-list dense class="features-list q-mt-md">
                    <q-item v-for="feature in iosFeatures" :key="feature">
                      <q-item-section avatar>
                        <q-icon name="schedule" color="orange" size="20px" />
                      </q-item-section>
                      <q-item-section class="text-white">{{ feature }}</q-item-section>
                    </q-item>
                  </q-list>

                  <q-badge color="orange" class="q-mt-md badge-glow">
                    <q-icon name="new_releases" class="q-mr-xs" /> Скоро
                  </q-badge>
                </q-card-section>

                <q-card-actions align="center" class="q-pa-lg">
                  <q-btn
                    color="white"
                    size="xl"
                    label="Уведомить о выходе"
                    icon="notifications"
                    outline
                    class="download-btn full-width"
                    @click="notifyMe"
                  />
                </q-card-actions>
              </q-card>
            </div>
          </div>
        </div>

        <!-- Для десктопа -->
        <div v-else class="desktop-section">
          <div class="text-center q-mb-xl">
            <h2 class="text-h3 gradient-text">💻 Веб-версия</h2>
            <p class="text-white text-opacity-80">Используйте LFK прямо в браузере</p>
          </div>

          <div class="row justify-center q-mb-xl">
            <div class="col-12 col-md-8">
              <q-card flat class="web-card glass-card">
                <q-card-section class="text-center">
                  <q-icon name="web" size="64px" color="white" class="q-mb-md" />
                  <h3 class="text-h4 text-white">Веб-приложение</h3>
                  <p class="text-grey-3">Полная версия LFK в вашем браузере</p>

                  <div class="features-grid row q-col-gutter-md q-mt-lg">
                    <div class="col-6">
                      <div class="feature-item">
                        <q-icon name="videocam" color="white" size="32px" />
                        <span class="text-white">Компьютерное зрение</span>
                      </div>
                    </div>
                    <div class="col-6">
                      <div class="feature-item">
                        <q-icon name="flash_on" color="white" size="32px" />
                        <span class="text-white">Реальное время</span>
                      </div>
                    </div>
                    <div class="col-6">
                      <div class="feature-item">
                        <q-icon name="insights" color="white" size="32px" />
                        <span class="text-white">Статистика</span>
                      </div>
                    </div>
                    <div class="col-6">
                      <div class="feature-item">
                        <q-icon name="history" color="white" size="32px" />
                        <span class="text-white">История тренировок</span>
                      </div>
                    </div>
                  </div>

                  <div class="browser-support q-mt-lg">
                    <p class="text-caption text-grey-4">Поддерживаемые браузеры:</p>
                    <div class="row justify-center q-gutter-sm">
                      <!-- Браузеры с неоном -->
                      <div class="row justify-center q-gutter-sm">
                        <q-chip
                          icon="fab fa-chrome"
                          style="background: #4285F4; color: white; border: 2px solid #4285F4; box-shadow: 0 0 15px #4285F4;"
                          class="neon-chip"
                        >
                          Chrome
                        </q-chip>

                        <q-chip
                          icon="fab fa-firefox"
                          style="background: #FF7139; color: white; border: 2px solid #FF7139; box-shadow: 0 0 15px #FF7139;"
                          class="neon-chip"
                        >
                          Firefox
                        </q-chip>

                        <q-chip
                          icon="fab fa-safari"
                          style="background: #006CFF; color: white; border: 2px solid #006CFF; box-shadow: 0 0 15px #006CFF;"
                          class="neon-chip"
                        >
                          Safari
                        </q-chip>

                        <q-chip
                          icon="fab fa-edge"
                          style="background: #0078D7; color: white; border: 2px solid #0078D7; box-shadow: 0 0 15px #0078D7;"
                          class="neon-chip"
                        >
                          Edge
                        </q-chip>
                      </div>
                    </div>
                  </div>
                </q-card-section>

                <q-card-actions align="center" class="q-pa-lg">
                  <q-btn
                    color="primary"
                    size="xl"
                    label="Перейти к веб-версии"
                    icon="arrow_forward"
                    to="/"
                    unelevated
                    class="web-btn glossy"
                  />
                </q-card-actions>
              </q-card>
            </div>
          </div>

          <!-- QR код для мобильных (на десктопе) -->
          <div class="text-center q-mt-xl">
            <p class="text-h6 text-white text-opacity-80">Или отсканируйте QR-код для установки на телефон</p>
            <div class="qr-container glass-card">
              <img src="/qr-code.png" alt="QR Code" class="qr-code">
            </div>
          </div>
        </div>

        <!-- Системные требования (для всех) -->
        <div class="requirements-section q-mt-xl">
          <h3 class="text-h4 text-center q-mb-lg gradient-text">Системные требования</h3>

          <div class="row justify-center">
            <div class="col-12 col-md-10">
              <q-tabs
                v-model="requirementsTab"
                dense
                class="text-white"
                active-color="white"
                indicator-color="white"
                align="justify"
                narrow-indicator
              >
                <q-tab name="android" label="Android" icon="fab fa-android" />
                <q-tab name="ios" label="iOS" icon="fab fa-apple" />
                <q-tab name="web" label="Web" icon="web" />
              </q-tabs>

              <q-separator dark />

              <q-tab-panels v-model="requirementsTab" animated dark>
                <!-- Android требования -->
                <q-tab-panel name="android">
                  <div class="row q-col-gutter-md">
                    <div class="col-12 col-sm-6" v-for="req in androidReqs" :key="req.label">
                      <q-item dark>
                        <q-item-section avatar>
                          <q-icon :name="req.icon" color="green" size="24px" />
                        </q-item-section>
                        <q-item-section>
                          <q-item-label class="text-weight-bold text-white">{{ req.label }}</q-item-label>
                          <q-item-label caption class="text-grey-4">{{ req.value }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </div>
                  </div>
                </q-tab-panel>

                <!-- iOS требования -->
                <q-tab-panel name="ios">
                  <div class="row q-col-gutter-md">
                    <div class="col-12 col-sm-6" v-for="req in iosReqs" :key="req.label">
                      <q-item dark>
                        <q-item-section avatar>
                          <q-icon :name="req.icon" color="grey-4" size="24px" />
                        </q-item-section>
                        <q-item-section>
                          <q-item-label class="text-weight-bold text-white">{{ req.label }}</q-item-label>
                          <q-item-label caption class="text-grey-4">{{ req.value }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </div>
                  </div>
                </q-tab-panel>

                <!-- Web требования -->
                <q-tab-panel name="web">
                  <div class="row q-col-gutter-md">
                    <div class="col-12 col-sm-6" v-for="req in webReqs" :key="req.label">
                      <q-item dark>
                        <q-item-section avatar>
                          <q-icon :name="req.icon" color="white" size="24px" />
                        </q-item-section>
                        <q-item-section>
                          <q-item-label class="text-weight-bold text-white">{{ req.label }}</q-item-label>
                          <q-item-label caption class="text-grey-4">{{ req.value }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </div>
                  </div>
                </q-tab-panel>
              </q-tab-panels>
            </div>
          </div>
        </div>

        <!-- Инструкция по установке -->
        <div class="install-section q-mt-xl">
          <h3 class="text-h4 text-center q-mb-lg gradient-text">Как установить</h3>

          <div class="row justify-center q-col-gutter-lg">
            <div v-for="(step, index) in installSteps" :key="index" class="col-12 col-md-3">
              <div class="step-card glass-card text-center">
                <div class="step-circle">{{ index + 1 }}</div>
                <q-icon :name="step.icon" size="32px" color="white" class="q-my-md" />
                <h4 class="text-h6 text-white">{{ step.title }}</h4>
                <p class="text-caption text-grey-3">{{ step.description }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- FAQ -->
        <div class="faq-section q-mt-xl q-mb-xl">
          <h3 class="text-h4 text-center q-mb-lg gradient-text">Часто задаваемые вопросы</h3>

          <div class="row justify-center">
            <div class="col-12 col-md-8">
              <q-expansion-item
                v-for="(faq, index) in faqs"
                :key="index"
                :label="faq.question"
                :icon="faq.icon"
                header-class="text-white"
                expand-icon-class="text-white"
                class="q-mb-sm glass-card"
                dark
              >
                <q-card dark>
                  <q-card-section class="text-grey-3">
                    {{ faq.answer }}
                  </q-card-section>
                </q-card>
              </q-expansion-item>
            </div>
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter } from 'vue-router'

const $q = useQuasar()
const router = useRouter()
const requirementsTab = ref('android')
const showDeviceInfo = ref(true)

// Детектор устройства
const isMobile = computed(() => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
})

const isIOS = computed(() => {
  return /iPad|iPhone|iPod/.test(navigator.userAgent)
})

const isAndroid = computed(() => {
  return /Android/.test(navigator.userAgent)
})

// Сообщения для разных устройств
const deviceIcon = computed(() => {
  if (isIOS.value) return 'fab fa-apple'
  if (isAndroid.value) return 'fab fa-android'
  return 'computer'
})

const deviceClass = computed(() => {
  if (isIOS.value) return 'ios-banner'
  if (isAndroid.value) return 'android-banner'
  return 'web-banner'
})

const deviceActionColor = computed(() => {
  if (isIOS.value) return 'grey-8'
  if (isAndroid.value) return 'green'
  return 'primary'
})

const deviceMessage = computed(() => {
  if (isIOS.value) {
    return 'Обнаружено iOS устройство. Скоро появится приложение в App Store!'
  }
  if (isAndroid.value) {
    return 'Обнаружено Android устройство. Скачайте приложение для лучшего опыта!'
  }
  return 'Вы используете компьютер. Попробуйте веб-версию или отсканируйте QR код для установки на телефон!'
})

const deviceActionLabel = computed(() => {
  if (isIOS.value) return 'Уведомить'
  if (isAndroid.value) return 'Скачать APK'
  return 'Веб-версия'
})

const handleDeviceAction = () => {
  if (isIOS.value) {
    notifyMe()
  } else if (isAndroid.value) {
    window.location.href = downloadUrl
  } else {
    router.push('/')
  }
}

// Данные
const downloadUrl = '/downloads/lfk-android.apk'

const androidFeatures = [
  'Полностью бесплатно',
  'Без рекламы',
  'Работает офлайн',
  'Автообновления',
  'Компьютерное зрение',
  'Голосовые подсказки'
]

const iosFeatures = [
  'В разработке',
  'Следите за новостями',
  'Будет доступно скоро',
  'Подпишитесь на уведомления'
]

const androidReqs = [
  { icon: 'android', label: 'ОС', value: 'Android 8.0 (Oreo) и выше' },
  { icon: 'memory', label: 'RAM', value: '2 GB и выше' },
  { icon: 'camera', label: 'Камера', value: '5 MP и выше' },
  { icon: 'storage', label: 'Память', value: '50 MB свободно' },
  { icon: 'gpu', label: 'Графика', value: 'OpenGL ES 3.0+' },
  { icon: 'camera', label: 'Камера', value: 'Поддержка камеры' }
]

const iosReqs = [
  { icon: 'apple', label: 'ОС', value: 'iOS 14.0 и выше' },
  { icon: 'memory', label: 'RAM', value: '2 GB и выше' },
  { icon: 'camera', label: 'Камера', value: '12 MP и выше' },
  { icon: 'storage', label: 'Память', value: '100 MB свободно' },
  { icon: 'apple', label: 'Устройства', value: 'iPhone 8 и новее' },
  { icon: 'camera', label: 'TrueDepth', value: 'Для Face ID' }
]

const webReqs = [
  { icon: 'web', label: 'Браузеры', value: 'Chrome, Firefox, Safari, Edge' },
  { icon: 'videocam', label: 'Камера', value: 'Доступ к веб-камере' },
  { icon: 'speed', label: 'Интернет', value: 'Стабильное соединение' },
  { icon: 'memory', label: 'Процессор', value: 'Современный браузер' },
  { icon: 'wifi', label: 'WebSocket', value: 'Поддержка WebSocket' },
  { icon: 'javascript', label: 'JavaScript', value: 'Включен' }
]

const installSteps = [
  {
    icon: 'download',
    title: 'Скачайте',
    description: 'Скачайте APK файл на устройство'
  },
  {
    icon: 'security',
    title: 'Разрешите установку',
    description: 'В настройках разрешите установку из неизвестных источников'
  },
  {
    icon: 'folder',
    title: 'Откройте файл',
    description: 'Найдите скачанный файл в папке Загрузки'
  },
  {
    icon: 'install',
    title: 'Установите',
    description: 'Нажмите "Установить" и дождитесь завершения'
  }
]

const faqs = [
  {
    icon: 'help',
    question: 'Почему нет в Google Play / App Store?',
    answer: 'Мы распространяем приложение бесплатно и независимо. Скачивание APK файла абсолютно безопасно - все сборки проверяются.'
  },
  {
    icon: 'security',
    question: 'Безопасно ли скачивать APK?',
    answer: 'Да, мы проверяем каждую сборку. Вы также можете проверить SHA256 хеш файла для дополнительной безопасности.'
  },
  {
    icon: 'update',
    question: 'Как обновлять приложение?',
    answer: 'Приложение автоматически проверяет обновления при запуске. Вы также можете скачать новую версию с этой страницы.'
  },
  {
    icon: 'camera',
    question: 'Нужна ли камера для веб-версии?',
    answer: 'Да, для работы компьютерного зрения необходим доступ к камере. Без камеры вы сможете только просматривать статистику.'
  }
]

const notifyMe = () => {
  $q.dialog({
    title: 'Уведомление о выходе',
    message: 'Оставьте email, и мы сообщим когда приложение появится в App Store',
    prompt: {
      model: '',
      type: 'email',
      placeholder: 'your@email.com'
    },
    cancel: true,
    persistent: true
  }).onOk(email => {
    $q.notify({
      type: 'positive',
      message: 'Спасибо! Мы сообщим о выходе приложения',
      position: 'top'
    })
  })
}

onMounted(() => {
  console.log('Device detected:', {
    isMobile: isMobile.value,
    isIOS: isIOS.value,
    isAndroid: isAndroid.value
  })
})
</script>

<style lang="scss" scoped>
.download-page {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

// Анимированный градиентный фон
.gradient-bg {
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

    &.sphere-4 {
      width: 700px;
      height: 700px;
      background: radial-gradient(circle at 80% 20%, #f093fb, #f5576c);
      top: 20%;
      right: -100px;
      animation-delay: -7s;
    }
  }

  .noise-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMzAwIj48ZmlsdGVyIGlkPSJmIj48ZmVUdXJidWxlbmNlIHR5cGU9ImZyYWN0YWxOb2lzZSIgYmFzZUZyZXF1ZW5jeT0iLjc0IiBudW1PY3RhdmVzPSIzIiAvPjwvZmlsdGVyPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbHRlcj0idXJsKCNmKSIgb3BhY2l0eT0iMC4xIiAvPjwvc3ZnPg==');
    opacity: 0.15;
    pointer-events: none;
  }
}

.hero-bg {
  background: transparent;
  width: 100%;
  height: 100%;
}

.hero-content {
  text-shadow: 0 2px 10px rgba(0,0,0,0.3);
}

.main-content {
  margin-top: -60px;
  position: relative;
  z-index: 2;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

// Стеклянные карточки
.glass-card {
  background: rgba(255, 255, 255, 0.1) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  transition: all 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.15) !important;
    border-color: rgba(255, 255, 255, 0.3);
    transform: translateY(-5px);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  }
}

.download-card {
  height: 100%;
  transition: all 0.3s;
  overflow: hidden;

  .platform-icon {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 20px;
    box-shadow: 0 10px 20px rgba(0,0,0,0.3);

    &.android {
      background: linear-gradient(135deg, #4caf50, #2e7d32);
    }

    &.ios {
      background: linear-gradient(135deg, #333, #000);
    }
  }

  .chip-light {
    background: rgba(255, 255, 255, 0.2) !important;
    backdrop-filter: blur(5px);
  }

  .features-list {
    .q-item {
      padding: 4px 0;
    }
  }
}

.badge-glow {
  box-shadow: 0 0 15px currentColor;
}

.web-card {
  .features-grid {
    .feature-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 16px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 16px;
      backdrop-filter: blur(5px);

      span {
        font-size: 14px;
        font-weight: 500;
      }
    }
  }
}

.download-btn, .web-btn {
  height: 64px;
  font-size: 1.2rem;
  transition: all 0.3s;

  &:hover {
    transform: scale(1.02);
    box-shadow: 0 20px 40px rgba(0,0,0,0.3);
  }
}

.glossy {
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0));
}

.qr-container {
  display: inline-block;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 24px;

  .qr-code {
    width: 200px;
    height: 200px;
    border-radius: 12px;
  }
}

.step-card {
  padding: 30px 20px;
  border-radius: 20px;

  .step-circle {
    width: 50px;
    height: 50px;
    line-height: 50px;
    text-align: center;
    font-size: 24px;
    font-weight: bold;
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: white;
    border-radius: 50%;
    margin: 0 auto;
    box-shadow: 0 10px 20px rgba(0,0,0,0.3);
  }
}

.faq-section {
  .q-expansion-item {
    border-radius: 12px;
    overflow: hidden;

    :deep(.q-expansion-item__container) {
      border-radius: 12px;
    }
  }
}

.gradient-text {
  background: linear-gradient(135deg, #fff, #e0e0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 10px rgba(0,0,0,0.3);
}

// Баннеры для устройств
.device-banner {
  backdrop-filter: blur(10px);

  &.ios-banner {
    background: rgba(0, 0, 0, 0.3) !important;
    color: white;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }

  &.android-banner {
    background: rgba(76, 175, 80, 0.2) !important;
    color: white;
    border: 1px solid #4caf50;
  }

  &.web-banner {
    background: rgba(102, 126, 234, 0.2) !important;
    color: white;
    border: 1px solid #667eea;
  }
}

// Анимации
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-30px, 20px) scale(0.9); }
}

.floating {
  animation: float 6s ease-in-out infinite;
}

// Утилиты
.text-opacity-80 {
  opacity: 0.8;
}

.text-opacity-90 {
  opacity: 0.9;
}
</style>
