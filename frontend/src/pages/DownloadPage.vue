<template>
  <q-page class="download-page">
    <!-- Hero -->
    <div class="hero-section bg-gradient text-white text-center">
      <q-icon name="download" size="64px" class="q-mb-md floating" />
      <h1 class="text-h2 q-mb-md">Скачать приложение</h1>
      <p class="text-h6">Выберите вашу платформу</p>
    </div>

    <!-- Скачивание -->
    <div class="download-content">
      <div class="container">
        <div class="row justify-center q-col-gutter-xl">
          <div class="col-12 col-md-6">
            <q-card flat bordered class="download-card">
              <q-card-section class="text-center">
                <q-icon name="fab fa-android" size="80px" color="green" />
                <h2 class="text-h3 q-mt-md">Android</h2>
                <p class="text-grey-7">Версия 1.0.0 • 25 MB • Android 8.0+</p>

                <q-badge color="green" class="q-mb-md">
                  <q-icon name="security" /> Проверено
                </q-badge>

                <q-list dense>
                  <q-item v-for="feature in androidFeatures" :key="feature">
                    <q-item-section avatar>
                      <q-icon name="check_circle" color="positive" />
                    </q-item-section>
                    <q-item-section>{{ feature }}</q-item-section>
                  </q-item>
                </q-list>
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
                  class="full-width download-btn"
                />
              </q-card-actions>
            </q-card>
          </div>

          <div class="col-12 col-md-6">
            <q-card flat bordered class="download-card">
              <q-card-section class="text-center">
                <q-icon name="fab fa-apple" size="80px" color="black" />
                <h2 class="text-h3 q-mt-md">iOS</h2>
                <q-badge color="warning" size="xl" class="q-mt-md">Скоро в App Store</q-badge>

                <q-list dense class="q-mt-md">
                  <q-item v-for="feature in iosFeatures" :key="feature">
                    <q-item-section avatar>
                      <q-icon name="schedule" color="orange" />
                    </q-item-section>
                    <q-item-section>{{ feature }}</q-item-section>
                  </q-item>
                </q-list>
              </q-card-section>

              <q-card-actions align="center" class="q-pa-lg">
                <q-btn
                  color="black"
                  size="xl"
                  label="Уведомить о выходе"
                  icon="notifications"
                  disable
                  outline
                  class="full-width"
                />
              </q-card-actions>
            </q-card>
          </div>
        </div>
      </div>
    </div>

    <!-- Системные требования -->
    <div class="requirements-section">
      <div class="container">
        <h2 class="text-h3 text-center q-mb-xl">Системные требования</h2>

        <div class="row justify-center">
          <div class="col-12 col-md-8">
            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6" v-for="req in requirements" :key="req.label">
                <q-item>
                  <q-item-section avatar>
                    <q-icon :name="req.icon" color="primary" size="24px" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label class="text-weight-bold">{{ req.label }}</q-item-label>
                    <q-item-label caption>{{ req.value }}</q-item-label>
                  </q-item-section>
                </q-item>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Инструкция по установке -->
    <div class="install-section">
      <div class="container">
        <h2 class="text-h3 text-center q-mb-xl">Как установить</h2>

        <div class="row justify-center q-col-gutter-lg">
          <div v-for="(step, index) in installSteps" :key="index" class="col-12 col-md-3">
            <div class="step-card text-center">
              <div class="step-circle">{{ index + 1 }}</div>
              <p class="q-mt-md">{{ step }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
const downloadUrl = '/downloads/lfk-android.apk'

const androidFeatures = [
  'Полностью бесплатно',
  'Без рекламы',
  'Подробная статистика',
  'Автообновления'
]

const iosFeatures = [
  'В разработке',
  'Следите за новостями',
  'Будет доступно скоро',
  'Подпишитесь на уведомления'
]

const requirements = [
  { icon: 'android', label: 'Операционная система', value: 'Android 8.0 (Oreo) и выше' },
  { icon: 'memory', label: 'Оперативная память', value: '2 GB и выше' },
  { icon: 'camera', label: 'Камера', value: '5 MP и выше' },
  { icon: 'storage', label: 'Свободное место', value: '50 MB' },
  { icon: 'wifi', label: 'Интернет', value: 'Стабильная сеть' },
  { icon: 'gpu', label: 'Графика', value: 'OpenGL ES 3.0+' }
]

const installSteps = [
  'Скачайте APK файл',
  'Разрешите установку из неизвестных источников',
  'Откройте скачанный файл',
  'Нажмите "Установить"'
]
</script>

<style lang="scss" scoped>
.download-page {
  min-height: 100vh;
}

.hero-section {
  padding: 80px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.download-content {
  padding: 80px 20px;
  background: #f8f9fa;
}

.download-card {
  height: 100%;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-10px);
    box-shadow: 0 20px 40px rgba(102, 126, 234, 0.2);
  }

  .download-btn {
    height: 64px;
    font-size: 1.2rem;

    &:hover {
      transform: scale(1.02);
    }
  }
}

.requirements-section {
  padding: 80px 20px;
  background: white;

  .q-item {
    border-radius: 10px;
    transition: all 0.3s;

    &:hover {
      background: rgba(102, 126, 234, 0.05);
      transform: translateX(5px);
    }
  }
}

.install-section {
  padding: 80px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;

  .step-card {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border-radius: 20px;
    padding: 30px 20px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-10px);
      background: rgba(255, 255, 255, 0.2);
    }

    .step-circle {
      width: 50px;
      height: 50px;
      line-height: 50px;
      text-align: center;
      font-size: 24px;
      font-weight: bold;
      background: white;
      color: #667eea;
      border-radius: 50%;
      margin: 0 auto;
    }
  }
}
</style>
