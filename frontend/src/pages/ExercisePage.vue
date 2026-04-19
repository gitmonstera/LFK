<template>
  <q-page class="exercise-page">
    <!-- Заголовок -->
    <div class="header">
      <div class="container">
        <div class="row items-center q-pa-md">
          <q-btn flat round dense icon="arrow_back" @click="confirmExit" class="q-mr-sm text-white" />
          <div class="text-h5 text-white">{{ exerciseInfo?.name || exerciseName }}</div>
          <q-space />

          <q-chip v-if="connectionStatus === 'connected'" color="positive" text-color="white" icon="wifi" size="sm">
            Подключено
          </q-chip>
          <q-chip v-else-if="connectionStatus === 'connecting'" color="warning" text-color="white" icon="wifi" size="sm">
            Подключение...
          </q-chip>
          <q-chip v-else color="negative" text-color="white" icon="wifi_off" size="sm">
            Отключено
          </q-chip>

          <q-chip v-if="isProduction" color="primary" text-color="white" icon="cloud" class="q-ml-sm" size="sm">
            PROD
          </q-chip>
          <q-chip v-else color="secondary" text-color="white" icon="code" class="q-ml-sm" size="sm">
            DEV
          </q-chip>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="container">
        <div class="row">
          <!-- Левая колонка -->
          <div class="col-12 col-lg-7">
            <!-- Переключатель режимов -->
            <div class="mode-switch-wrapper q-mb-md" v-if="!isTrainingActive || exerciseCompleted">
              <div class="mode-switch">
                <button
                  v-for="option in modeOptions"
                  :key="option.value"
                  class="mode-btn"
                  :class="{ active: viewMode === option.value }"
                  @click="viewMode = option.value"
                >
                  <q-icon :name="option.icon" size="18px" />
                  <span>{{ option.label }}</span>
                </button>
              </div>
            </div>

            <!-- Режим камеры -->
            <div v-if="viewMode === 'camera'" class="video-container">
              <div v-if="!isTrainingActive && !exerciseCompleted" class="video-overlay">
                <div class="overlay-content">
                  <q-icon name="videocam_off" size="64px" color="white" />
                  <div class="text-h6 q-mt-md">Камера активна</div>
                  <div class="text-caption q-mb-md">Нажмите "Начать тренировку" чтобы начать</div>
                  <q-btn
                    color="positive"
                    label="Начать тренировку"
                    icon="play_arrow"
                    size="lg"
                    @click="startTraining"
                    :loading="startingTraining"
                  />
                </div>
              </div>

              <div v-if="!cameraReady" class="camera-loading">
                <q-spinner size="50px" color="white" />
                <p class="text-white q-mt-md">Запуск камеры...</p>
              </div>

              <img v-if="processedFrame" :src="processedFrame" class="processed-video" alt="Processed video" />
              <video ref="videoRef" autoplay playsinline muted class="local-video" :class="{ 'hidden': !showLocalVideo }"></video>

              <div class="detection-badge" v-if="bodyDetected !== null && isTrainingActive">
                <q-badge :color="bodyDetected ? 'positive' : 'negative'" class="detection-badge-inner">
                  <q-icon :name="bodyDetected ? (isPoseExercise ? 'face' : 'back_hand') : (isPoseExercise ? 'face_off' : 'pan_tool')" size="16px" />
                  {{ bodyDetected ? (isPoseExercise ? 'Тело в кадре' : 'Рука в кадре') : (isPoseExercise ? 'Тело не обнаружено' : 'Рука не обнаружена') }}
                </q-badge>
              </div>

              <div class="finger-info" v-if="!isPoseExercise && fingerStates.length > 0 && isTrainingActive">
                <div class="finger-row">
                  <div v-for="(state, index) in fingerStates" :key="index" class="finger-indicator" :class="{ active: state }">
                    <q-icon :name="getFingerIcon(index)" size="20px" />
                    <span>{{ getFingerName(index) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Режим инструкции -->
            <div v-if="viewMode === 'instructions'" class="instructions-container">
              <div class="instructions-card">
                <div class="instructions-header">
                  <div class="header-icon">
                    <q-icon name="menu_book" size="28px" color="primary" />
                  </div>
                  <div class="text-h6">Инструкция к упражнению</div>
                  <q-space />
                  <div class="header-badges">
                    <q-badge :color="getDifficultyColor(exerciseInfo?.difficulty_level)" class="q-mr-sm">
                      <q-icon name="trending_up" size="12px" class="q-mr-xs" />
                      {{ getDifficultyText(exerciseInfo?.difficulty_level) }}
                    </q-badge>
                    <q-badge :style="{ backgroundColor: exerciseInfo?.category_color || '#667eea' }" class="category-badge">
                      <q-icon :name="getCategoryIcon(exerciseInfo?.category_icon)" size="12px" class="q-mr-xs" />
                      {{ exerciseInfo?.category || 'Категория' }}
                    </q-badge>
                  </div>
                </div>

                <q-separator class="q-my-md" />

                <div class="exercise-description q-mb-md" v-if="exerciseInfo?.description">
                  <div class="text-subtitle2 text-primary">Описание</div>
                  <div class="text-body2">{{ exerciseInfo.description }}</div>
                </div>

                <div class="target-muscles q-mb-md" v-if="exerciseInfo?.target_muscles?.length">
                  <div class="text-subtitle2 text-primary">Задействованные мышцы</div>
                  <div class="muscle-chips">
                    <q-chip v-for="muscle in exerciseInfo.target_muscles" :key="muscle" size="sm" outline color="primary">
                      {{ muscle }}
                    </q-chip>
                  </div>
                </div>

                <div class="instruction-progress q-mb-md">
                  <div class="row items-center justify-between">
                    <span class="text-caption">Прогресс выполнения</span>
                    <span class="text-primary text-caption">{{ completedSteps }}/{{ totalSteps }}</span>
                  </div>
                  <q-linear-progress :value="completedSteps / totalSteps" color="primary" class="q-mt-sm" size="6px" rounded />
                </div>

                <div class="steps-list">
                  <div v-for="(step, index) in exerciseInstructions" :key="index" class="step-item" :class="{ completed: step.completed }">
                    <div class="step-checkbox">
                      <q-checkbox v-model="step.completed" size="sm" color="positive" @update:model-value="updateProgress" />
                    </div>
                    <div class="step-number">{{ index + 1 }}</div>
                    <div class="step-content">
                      <div class="step-title">{{ step.text }}</div>
                    </div>
                    <div class="step-duration" v-if="getStepDuration(step.text)">
                      <q-icon name="schedule" size="12px" />
                      {{ getStepDuration(step.text) }}
                    </div>
                  </div>
                </div>

                <div class="instruction-actions q-mt-md">
                  <q-btn flat color="primary" label="Сбросить" icon="refresh" size="sm" @click="resetInstructionProgress" />
                  <q-btn flat color="positive" label="Отметить все" icon="done_all" size="sm" @click="markAllSteps" />
                </div>
              </div>
            </div>

            <!-- Режим МКБ-10 -->
            <div v-if="viewMode === 'mkb'" class="mkb-container">
              <div class="mkb-card">
                <div class="mkb-header">
                  <div class="header-icon">
                    <q-icon name="medication" size="28px" color="primary" />
                  </div>
                  <div class="text-h6">Показания к применению (МКБ-10)</div>
                  <q-space />
                  <q-input v-model="mkbSearch" placeholder="Поиск..." dense outlined class="mkb-search" />
                </div>

                <q-separator class="q-my-md" />

                <div class="mkb-list">
                  <div v-for="(code, index) in filteredMkbCodes" :key="index" class="mkb-item">
                    <div class="mkb-code">{{ code.code }}</div>
                    <div class="mkb-description">{{ code.description }}</div>
                    <q-badge :color="getCategoryColor(code.category)" class="mkb-category-badge" size="sm">
                      {{ code.category }}
                    </q-badge>
                  </div>
                </div>

                <div v-if="filteredMkbCodes.length === 0" class="mkb-empty">
                  <q-icon name="search_off" size="40px" color="grey" />
                  <div class="text-grey-4 q-mt-sm">Ничего не найдено</div>
                </div>
              </div>
            </div>

            <!-- Режим информации -->
            <div v-if="viewMode === 'info'" class="info-container">
              <div class="info-card">
                <div class="info-header">
                  <div class="header-icon" :style="{ backgroundColor: (exerciseInfo?.category_color || '#667eea') + '20', color: exerciseInfo?.category_color || '#667eea' }">
                    <q-icon :name="getCategoryIcon(exerciseInfo?.category_icon)" size="32px" />
                  </div>
                  <div>
                    <div class="text-h6">{{ exerciseInfo?.name || exerciseName }}</div>
                    <div class="text-caption text-grey-4">{{ exerciseInfo?.category || 'Категория' }}</div>
                  </div>
                </div>

                <q-separator class="q-my-md" />

                <div class="info-description" v-if="exerciseInfo?.description">
                  <div class="text-subtitle2 text-primary">Описание</div>
                  <div class="text-body2">{{ exerciseInfo.description }}</div>
                </div>

                <div class="info-details">
                  <div class="detail-item">
                    <q-icon name="schedule" size="20px" color="primary" />
                    <div>
                      <div class="detail-label">Длительность</div>
                      <div class="detail-value">{{ formatDuration(exerciseInfo?.duration_seconds) }}</div>
                    </div>
                  </div>
                  <div class="detail-item">
                    <q-icon name="fitness_center" size="20px" color="primary" />
                    <div>
                      <div class="detail-label">Целевые мышцы</div>
                      <div class="detail-value">{{ exerciseInfo?.target_muscles?.join(', ') || 'Не указаны' }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Правая колонка -->
          <div class="col-12 col-lg-5">
            <q-card flat class="stats-card">
              <q-card-section>
                <div class="text-h6">Прогресс упражнения</div>
                <q-separator class="q-my-md" />

                <div class="training-status q-mb-md">
                  <div class="row items-center">
                    <q-icon :name="isTrainingActive ? 'play_circle' : (exerciseCompleted ? 'check_circle' : 'pause_circle')"
                            :color="isTrainingActive ? 'positive' : (exerciseCompleted ? 'positive' : 'warning')" size="24px" />
                    <span class="q-ml-sm">
                      {{ isTrainingActive ? 'Тренировка активна' : (exerciseCompleted ? 'Тренировка завершена' : 'Тренировка не начата') }}
                    </span>
                  </div>
                </div>

                <div class="quick-actions q-mb-md" v-if="!isTrainingActive && !exerciseCompleted">
                  <q-btn color="positive" label="Начать тренировку" icon="play_arrow" class="full-width" size="md" @click="startTraining" :loading="startingTraining" />
                </div>

                <div class="quick-actions q-mb-md" v-else>
                  <q-btn flat color="primary" label="Инструкция" icon="menu_book" size="sm" @click="viewMode = 'instructions'" />
                  <q-btn flat color="info" label="МКБ-10" icon="medication" size="sm" @click="viewMode = 'mkb'" class="q-ml-sm" />
                </div>

                <!-- Упражнение для шеи -->
                <template v-if="exerciseId === 'neck' && isTrainingActive">
                  <div class="progress-section">
                    <div class="progress-info">
                      <div class="row items-center justify-between">
                        <div class="row items-center">
                          <q-icon name="repeat" size="16px" color="primary" />
                          <span class="q-ml-sm text-caption">Круг {{ structuredData.current_cycle || 1 }}/{{ structuredData.total_cycles || 3 }}</span>
                        </div>
                        <div class="text-caption">{{ structuredData.current_move || 0 }}/{{ structuredData.total_moves || 4 }}</div>
                      </div>
                      <q-linear-progress :value="(structuredData.progress_percent || 0) / 100" color="primary" class="q-mt-sm q-mb-md" size="6px" rounded />
                    </div>

                    <div class="current-move-card">
                      <div class="move-icon"><q-icon :name="getMoveIcon(structuredData.state)" size="36px" color="primary" /></div>
                      <div class="move-info">
                        <div class="move-name">{{ structuredData.move_name || 'Подготовка' }}</div>
                        <div class="move-action">{{ structuredData.move_action || 'Смотрите в камеру' }}</div>
                      </div>
                    </div>

                    <div v-if="structuredData.is_holding && structuredData.countdown" class="hold-indicator">
                      <q-circular-progress :value="(structuredData.hold_progress || 0)" size="60px" :thickness="0.2" color="warning" track-color="grey-7" show-value font-size="16px">
                        {{ structuredData.countdown }}с
                      </q-circular-progress>
                      <div class="hold-text">Удерживайте</div>
                    </div>
                  </div>
                </template>

                <!-- Для обычных упражнений -->
                <template v-else-if="isTrainingActive && exerciseId !== 'neck'">
                  <div class="stats-grid">
                    <div class="stat-item">
                      <q-icon name="back_hand" size="28px" color="primary" />
                      <div class="stat-value">{{ raisedFingers }}/5</div>
                      <div class="stat-label">Пальцев поднято</div>
                    </div>
                  </div>
                  <div class="finger-grid">
                    <div v-for="i in 5" :key="i" class="finger-stat" :class="{ active: fingerStates[i-1] }">
                      <q-icon :name="getFingerIcon(i-1)" size="18px" />
                      <span>{{ getFingerName(i-1) }}</span>
                    </div>
                  </div>
                </template>

                <!-- Сообщение -->
                <div v-if="serverMessage && isTrainingActive" class="message-box q-mt-md" :class="messageClass">
                  <q-icon :name="messageIcon" size="16px" class="q-mr-sm" />
                  <div class="message-text">{{ serverMessage }}</div>
                </div>

                <!-- Ручное выполнение -->
                <div class="manual-complete q-mt-md" v-if="isTrainingActive && !exerciseCompleted">
                  <q-btn outline color="positive" label="✓ Отметить выполнение" icon="check_circle" class="full-width" size="md" @click="showManualCompleteDialog" />
                  <div class="text-caption text-grey-4 q-mt-xs text-center">
                    Если сложно выполнять перед камерой
                  </div>
                </div>

                <!-- Кнопки управления -->
                <div class="action-buttons q-mt-lg">
                  <q-btn v-if="isTrainingActive && !exerciseCompleted" :loading="savingStats" color="negative" label="Завершить" icon="stop" class="full-width" size="md" @click="endWorkoutEarly" />
                  <q-btn v-else-if="exerciseCompleted" color="positive" label="Выполнено!" icon="check_circle" class="full-width" size="md" @click="exitExercise" />
                </div>
              </q-card-section>
            </q-card>
          </div>
        </div>
      </div>
    </div>

    <!-- Диалоги -->
    <q-dialog v-model="showConnectionError" persistent>
      <q-card class="error-dialog">
        <q-card-section class="row items-center">
          <q-icon name="wifi_off" size="40px" color="negative" />
          <div class="text-h6 q-ml-md">Проблемы с подключением</div>
        </q-card-section>
        <q-card-section>
          <div class="text-body2">Не удалось подключиться к серверу.</div>
          <div class="text-caption text-grey-4 q-mt-sm">Проверьте интернет-соединение и попробуйте снова.</div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Выйти" color="negative" @click="exitExercise" />
          <q-btn flat label="Повторить" color="primary" @click="retryConnection" :loading="retrying" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <q-dialog v-model="showManualDialog">
      <q-card class="manual-dialog">
        <q-card-section class="row items-center">
          <q-icon name="check_circle" size="32px" color="positive" />
          <div class="text-h6 q-ml-md">Отметить выполнение</div>
        </q-card-section>
        <q-card-section>
          <div class="text-body2">Вы действительно выполнили упражнение "{{ exerciseInfo?.name || exerciseName }}"?</div>
          <div class="text-caption text-grey-4 q-mt-sm">Это действие сохранит упражнение как выполненное в вашу статистику.</div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Отмена" color="grey" v-close-popup />
          <q-btn flat label="Да, выполнил" color="positive" @click="manualComplete" :loading="manualCompleteLoading" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <q-dialog v-model="showExitDialog">
      <q-card>
        <q-card-section><div class="text-h6">Завершить тренировку?</div></q-card-section>
        <q-card-section class="text-body2">Прогресс будет сохранен. Вы уверены?</q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Отмена" color="primary" v-close-popup />
          <q-btn flat label="Завершить" color="negative" @click="exitExercise" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from 'src/boot/axios'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()

// Конфигурация
const WS_URL = process.env.WS_URL || 'ws://localhost:9000'
const isProduction = process.env.NODE_ENV === 'production'
const POSE_EXERCISES = ['neck', 'shoulder', 'back', 'pose']

// Опции режимов
const modeOptions = [
  { label: 'Камера', value: 'camera', icon: 'videocam' },
  { label: 'Инструкция', value: 'instructions', icon: 'menu_book' },
  { label: 'МКБ-10', value: 'mkb', icon: 'medication' },
  { label: 'Об упражнении', value: 'info', icon: 'info' }
]

// Состояние
const exerciseId = computed(() => route.params.id)
const isPoseExercise = computed(() => POSE_EXERCISES.includes(exerciseId.value))
const exerciseName = ref('')
const exerciseInfo = ref(null)

const viewMode = ref('camera')
const isTrainingActive = ref(false)
const startingTraining = ref(false)

// WebSocket
const ws = ref(null)
const connectionStatus = ref('disconnected')

// Камера
const videoRef = ref(null)
const cameraReady = ref(false)
const showLocalVideo = ref(true)
const processedFrame = ref(null)
const frameInterval = ref(null)

// Данные
const bodyDetected = ref(null)
const fingerStates = ref([])
const raisedFingers = ref(0)
const serverMessage = ref('')
const messageClass = ref('')
const structuredData = ref({})
const exerciseCompleted = ref(false)

// Тренировка
const sessionId = ref(null)
const savingStats = ref(false)

// UI
const showExitDialog = ref(false)
const showConnectionError = ref(false)
const showManualDialog = ref(false)
const retrying = ref(false)
const manualCompleteLoading = ref(false)

// Инструкция
const exerciseInstructions = ref([])
const completedSteps = ref(0)
const totalSteps = ref(0)

// МКБ-10
const mkbCodes = ref([])
const mkbSearch = ref('')

// Вычисляемые
const filteredMkbCodes = computed(() => {
  if (!mkbSearch.value) return mkbCodes.value
  const search = mkbSearch.value.toLowerCase()
  return mkbCodes.value.filter(code =>
    code.code.toLowerCase().includes(search) ||
    code.description.toLowerCase().includes(search) ||
    code.category.toLowerCase().includes(search)
  )
})

const messageIcon = computed(() => {
  const msg = serverMessage.value
  if (msg.includes('🎉') || msg.includes('поздравляю')) return 'check_circle'
  if (msg.includes('❌')) return 'error'
  if (msg.includes('🔧')) return 'settings'
  return 'info'
})

// Вспомогательные функции
const getFingerIcon = (index) => {
  const icons = ['thumb_up', 'index', 'middle', 'ring', 'mic']
  return icons[index] || 'fingers'
}

const getFingerName = (index) => {
  const names = ['Большой', 'Указательный', 'Средний', 'Безымянный', 'Мизинец']
  return names[index] || ''
}

const getMoveIcon = (state) => {
  const icons = { 'forward': 'arrow_downward', 'back': 'arrow_upward', 'left': 'chevron_left', 'right': 'chevron_right', 'neutral': 'radio_button_unchecked' }
  return icons[state] || 'fitness_center'
}

const getCategoryIcon = (icon) => {
  if (!icon) return 'fitness_center'
  if (icon === '👐') return 'pan_tool'
  if (icon === '🦒') return 'face'
  if (icon === '🤏') return 'pinch'
  return 'fitness_center'
}

const getDifficultyColor = (level) => {
  const colors = { 1: 'positive', 2: 'warning', 3: 'orange', 4: 'negative', 5: 'dark' }
  return colors[level] || 'grey'
}

const getDifficultyText = (level) => {
  const texts = { 1: 'Низкая', 2: 'Средняя', 3: 'Выше среднего', 4: 'Высокая', 5: 'Очень высокая' }
  return texts[level] || 'Не указана'
}

const getCategoryColor = (category) => {
  const colors = { 'Травмы': 'negative', 'Неврология': 'info', 'Заболевания позвоночника': 'warning', 'Реабилитация': 'positive', 'Заболевания суставов': 'primary', 'Болевые синдромы': 'orange' }
  return colors[category] || 'grey'
}

const formatDuration = (seconds) => {
  if (!seconds) return 'Не указано'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return mins > 0 ? `${mins} мин ${secs} сек` : `${secs} сек`
}

const getStepDuration = (text) => {
  const match = text.match(/(\d+)\s*сек|\s*(\d+)\s*с/i)
  if (match) return `${match[1] || match[2]}с`
  return null
}

// МКБ-10 данные
const getMkbCodes = (exerciseId) => {
  const mkbData = {
    neck: [
      { code: 'M47.1–M47.9', description: 'Спондилез шейного отдела, спондилез с миелопатией', category: 'Заболевания позвоночника' },
      { code: 'M50.0–M50.1', description: 'Поражение межпозвоночных дисков шейного отдела', category: 'Заболевания позвоночника' },
      { code: 'M53.0–M53.1', description: 'Цервикокраниальный синдром, шейно-плечевой синдром', category: 'Заболевания позвоночника' },
      { code: 'M54.0–M54.2', description: 'Плекситы, радикулопатии, цервикалгии', category: 'Болевые синдромы' },
      { code: 'G44.2–G44.4', description: 'Головные боли напряжения, посттравматические', category: 'Неврология' },
      { code: 'S13.0–S13.4', description: 'Вывихи, растяжения связок шейного отдела', category: 'Травмы' },
      { code: 'I63–I64', description: 'Последствия инсульта (постинсультная реабилитация)', category: 'Реабилитация' },
      { code: 'Z50.0', description: 'Лечебная физкультура (основной код назначения)', category: 'Реабилитация' }
    ],
    fist: [
      { code: 'S62.0–S62.9', description: 'Переломы запястья, пясти, пальцев кисти', category: 'Травмы' },
      { code: 'M19.0–M19.9', description: 'Артроз суставов кисти', category: 'Заболевания суставов' },
      { code: 'G81.0–G81.9', description: 'Гемиплегия (постинсультная)', category: 'Неврология' },
      { code: 'Z50.0', description: 'Лечебная физкультура', category: 'Реабилитация' }
    ]
  }
  return mkbData[exerciseId] || mkbData.fist
}

// Получение данных из query
const getExerciseDataFromQuery = () => {
  const query = route.query

  if (query.name) {
    exerciseInfo.value = {
      name: query.name,
      description: query.description || '',
      instructions: JSON.parse(query.instructions || '[]'),
      target_muscles: JSON.parse(query.target_muscles || '[]'),
      duration_seconds: parseInt(query.duration_seconds) || 0,
      category: query.category || '',
      category_icon: query.category_icon || '',
      category_color: query.category_color || '#667eea',
      difficulty_level: parseInt(query.difficulty_level) || 1
    }
    exerciseName.value = query.name

    // Загружаем инструкцию
    if (exerciseInfo.value.instructions && exerciseInfo.value.instructions.length > 0) {
      exerciseInstructions.value = exerciseInfo.value.instructions.map(text => ({ text, completed: false }))
      totalSteps.value = exerciseInfo.value.instructions.length
      completedSteps.value = 0

      const saved = localStorage.getItem(`instruction_progress_${exerciseId.value}`)
      if (saved) {
        try {
          const savedSteps = JSON.parse(saved)
          if (savedSteps.length === totalSteps.value) {
            exerciseInstructions.value = savedSteps
            completedSteps.value = savedSteps.filter(s => s.completed).length
          }
        } catch(e) {}
      }
    } else {
      exerciseInstructions.value = [{ text: 'Инструкция не загружена', completed: false }]
      totalSteps.value = 1
    }

    // Загружаем МКБ-10 коды
    mkbCodes.value = getMkbCodes(exerciseId.value)

    return true
  }
  return false
}

// Загрузка данных упражнения
const loadExerciseInfo = async () => {
  // Сначала пробуем получить данные из query
  if (getExerciseDataFromQuery()) {
    console.log('Данные загружены из query')
    return
  }

  // Если нет данных в query, загружаем из API
  try {
    const response = await api.get('/get_exercise_list')
    let exercisesList = []
    if (response.data && response.data.items) {
      exercisesList = response.data.items
    } else if (Array.isArray(response.data)) {
      exercisesList = response.data
    }

    const exercise = exercisesList.find(ex => ex.exercise_id === exerciseId.value)

    if (exercise) {
      exerciseInfo.value = exercise
      exerciseName.value = exercise.name

      if (exercise.instructions && exercise.instructions.length > 0) {
        exerciseInstructions.value = exercise.instructions.map(text => ({ text, completed: false }))
        totalSteps.value = exercise.instructions.length
        completedSteps.value = 0

        const saved = localStorage.getItem(`instruction_progress_${exerciseId.value}`)
        if (saved) {
          try {
            const savedSteps = JSON.parse(saved)
            if (savedSteps.length === totalSteps.value) {
              exerciseInstructions.value = savedSteps
              completedSteps.value = savedSteps.filter(s => s.completed).length
            }
          } catch(e) {}
        }
      } else {
        exerciseInstructions.value = [{ text: 'Инструкция не загружена', completed: false }]
        totalSteps.value = 1
      }

      mkbCodes.value = getMkbCodes(exerciseId.value)
    }
  } catch (error) {
    console.error('Error loading exercise:', error)
    $q.notify({ type: 'negative', message: 'Ошибка загрузки упражнения' })
  }
}

const updateProgress = () => {
  completedSteps.value = exerciseInstructions.value.filter(s => s.completed).length
  localStorage.setItem(`instruction_progress_${exerciseId.value}`, JSON.stringify(exerciseInstructions.value))
}

const resetInstructionProgress = () => {
  exerciseInstructions.value.forEach(step => step.completed = false)
  updateProgress()
  $q.notify({ type: 'info', message: 'Прогресс инструкции сброшен', position: 'top', timeout: 1500 })
}

const markAllSteps = () => {
  exerciseInstructions.value.forEach(step => step.completed = true)
  updateProgress()
  $q.notify({ type: 'positive', message: 'Все шаги отмечены', position: 'top', timeout: 1500 })
}

// Начало тренировки
const startTraining = async () => {
  startingTraining.value = true
  try {
    const response = await api.post('/workout/start')
    sessionId.value = response.data.id
    console.log('Тренировка начата, ID:', sessionId.value)

    isTrainingActive.value = true

    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      connectWebSocket()
    } else {
      startFrameCapture()
    }

    $q.notify({
      type: 'positive',
      message: 'Тренировка начата! Выполняйте упражнение.',
      position: 'top',
      timeout: 2000
    })
  } catch (error) {
    console.error('Error starting training:', error)
    $q.notify({
      type: 'negative',
      message: 'Ошибка начала тренировки',
      position: 'top'
    })
  } finally {
    startingTraining.value = false
  }
}

// Ручное выполнение
const showManualCompleteDialog = () => { showManualDialog.value = true }

const manualComplete = async () => {
  manualCompleteLoading.value = true
  try {
    if (!sessionId.value) {
      const response = await api.post('/workout/start')
      sessionId.value = response.data.id
    }

    await saveExerciseSet()
    await endWorkout()

    if (frameInterval.value) {
      clearInterval(frameInterval.value)
      frameInterval.value = null
    }

    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.close()
    }

    if (videoRef.value && videoRef.value.srcObject) {
      const tracks = videoRef.value.srcObject.getTracks()
      tracks.forEach(track => track.stop())
      videoRef.value.srcObject = null
    }

    exerciseCompleted.value = true
    isTrainingActive.value = false
    cameraReady.value = false

    $q.notify({
      type: 'positive',
      message: '✅ Упражнение отмечено как выполненное!',
      position: 'top',
      timeout: 2000
    })

    setTimeout(() => {
      router.push('/profile/exercises')
    }, 1500)

  } catch (error) {
    console.error('Manual complete error:', error)
    $q.notify({
      type: 'negative',
      message: 'Ошибка при сохранении',
      position: 'top'
    })
  } finally {
    manualCompleteLoading.value = false
  }
}

// Тренировка
const startWorkout = async () => {
  try {
    const response = await api.post('/workout/start')
    sessionId.value = response.data.id
  } catch (error) { console.error('Error starting workout:', error) }
}

const endWorkout = async () => {
  if (!sessionId.value) return
  try {
    await api.post('/workout/end', { session_id: sessionId.value })
  } catch (error) { console.error('Error ending workout:', error) }
}

const saveExerciseSet = async () => {
  savingStats.value = true
  try {
    await api.post('/workout/exercise', {
      session_id: sessionId.value,
      exercise_id: exerciseId.value,
      actual_repetitions: 1,
      actual_duration: 60,
      accuracy_score: 100
    })
  } catch (error) { console.error('Error saving stats:', error) }
  finally { savingStats.value = false }
}

// Камера
const initCamera = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
      audio: false
    })
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      videoRef.value.onloadedmetadata = () => { videoRef.value.play(); cameraReady.value = true }
    }
  } catch (error) { console.error('Camera error:', error) }
}

// WebSocket
const connectWebSocket = () => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) { router.push('/login'); return }

  connectionStatus.value = 'connecting'
  const wsUrl = `${WS_URL}/ws/exercise/${exerciseId.value}?token=${token}`

  try {
    ws.value = new WebSocket(wsUrl)
    ws.value.onopen = () => {
      connectionStatus.value = 'connected'
      startFrameCapture()
    }

    ws.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.processed_frame) processedFrame.value = `data:image/jpeg;base64,${data.processed_frame}`
        bodyDetected.value = data.hand_detected || data.body_detected || false
        fingerStates.value = data.finger_states || []
        raisedFingers.value = data.raised_fingers || 0
        serverMessage.value = data.message || ''

        const msg = serverMessage.value
        if (msg.includes('✅') || msg.includes('🎉')) messageClass.value = 'success'
        else if (msg.includes('❌')) messageClass.value = 'error'
        else if (msg.includes('🔧')) messageClass.value = 'calibration'
        else messageClass.value = 'info'

        if (data.structured) {
          structuredData.value = data.structured
          if (data.structured.completed && !exerciseCompleted.value) {
            exerciseCompleted.value = true
            isTrainingActive.value = false
            saveExerciseSet()
            endWorkout()
            $q.notify({ type: 'positive', message: '🎉 Упражнение выполнено!', position: 'top', timeout: 2000 })
          }
        }
      } catch (error) { console.error('Error parsing message:', error) }
    }

    ws.value.onerror = () => {
      connectionStatus.value = 'disconnected'
      showConnectionError.value = true
    }
    ws.value.onclose = () => { connectionStatus.value = 'disconnected' }
  } catch (error) {
    console.error('WebSocket error:', error)
    showConnectionError.value = true
  }
}

const startFrameCapture = () => {
  if (!videoRef.value || !ws.value || !isTrainingActive.value) return

  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 480
  const ctx = canvas.getContext('2d')

  frameInterval.value = setInterval(() => {
    if (!cameraReady.value || !ws.value || ws.value.readyState !== WebSocket.OPEN || !isTrainingActive.value) return
    try {
      ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
      canvas.toBlob((blob) => {
        const reader = new FileReader()
        reader.onloadend = () => ws.value.send(JSON.stringify({
          frame: reader.result.split(',')[1],
          exercise_type: exerciseId.value
        }))
        reader.readAsDataURL(blob)
      }, 'image/jpeg', 0.5)
    } catch (error) { console.error('Frame capture error:', error) }
  }, 200)
}

const retryConnection = () => {
  retrying.value = true
  showConnectionError.value = false
  setTimeout(() => { connectWebSocket(); retrying.value = false }, 1000)
}

const endWorkoutEarly = () => { showExitDialog.value = true }

const exitExercise = async () => {
  $q.loading.show({ message: 'Завершение тренировки...' })
  if (isTrainingActive.value) {
    await endWorkout()
  }
  if (ws.value) ws.value.close()
  if (frameInterval.value) clearInterval(frameInterval.value)
  if (videoRef.value?.srcObject) videoRef.value.srcObject.getTracks().forEach(track => track.stop())
  $q.loading.hide()
  router.push('/profile/exercises')
}

const confirmExit = () => {
  if (exerciseCompleted.value) {
    exitExercise()
  } else if (isTrainingActive.value) {
    showExitDialog.value = true
  } else {
    router.push('/profile/exercises')
  }
}

// Жизненный цикл
onMounted(async () => {
  await loadExerciseInfo()
  await initCamera()
  connectWebSocket()
})

onBeforeUnmount(() => {
  if (frameInterval.value) clearInterval(frameInterval.value)
  if (ws.value && ws.value.readyState === WebSocket.OPEN) ws.value.close()
  if (videoRef.value?.srcObject) {
    videoRef.value.srcObject.getTracks().forEach(track => track.stop())
    videoRef.value.srcObject = null
  }
  cameraReady.value = false
})
</script>

<style lang="scss" scoped>
.exercise-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a1f 0%, #0f0f2a 100%);

  .header {
    background: linear-gradient(135deg, #667eea, #764ba2);
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
  }

  .container { max-width: 1400px; margin: 0 auto; }
  .content { margin-top: 70px; padding: 20px; }

  .mode-switch-wrapper {
    display: flex;
    justify-content: center;
    .mode-switch {
      display: inline-flex;
      background: rgba(255, 255, 255, 0.08);
      border-radius: 40px;
      padding: 4px;
      backdrop-filter: blur(10px);
      .mode-btn {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 20px;
        border-radius: 32px;
        border: none;
        background: transparent;
        color: rgba(255, 255, 255, 0.6);
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.3s ease;
        &:hover { color: white; background: rgba(255, 255, 255, 0.1); }
        &.active {
          background: linear-gradient(135deg, #667eea, #764ba2);
          color: white;
          box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
        }
      }
    }
  }

  .video-container {
    position: relative;
    background: #000;
    border-radius: 20px;
    overflow: hidden;
    aspect-ratio: 4/3;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);

    .local-video, .processed-video { width: 100%; height: 100%; object-fit: cover; }
    .local-video.hidden { opacity: 0; }

    .video-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.8);
      z-index: 10;
      .overlay-content { text-align: center; color: white; }
    }

    .camera-loading {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.8);
    }

    .detection-badge {
      position: absolute;
      top: 16px;
      left: 16px;
      .detection-badge-inner { padding: 6px 12px; font-size: 12px; border-radius: 20px; backdrop-filter: blur(5px); }
    }

    .finger-info {
      position: absolute;
      bottom: 16px;
      left: 16px;
      right: 16px;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(5px);
      border-radius: 12px;
      padding: 10px;
      .finger-row { display: flex; justify-content: space-around; }
      .finger-indicator {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        padding: 6px;
        border-radius: 8px;
        opacity: 0.5;
        transition: all 0.3s;
        &.active { opacity: 1; background: rgba(102, 126, 234, 0.3); transform: scale(1.05); }
        span { font-size: 10px; }
      }
    }
  }

  .instructions-card, .mkb-card, .info-card {
    background: rgba(255, 255, 255, 0.08);
    backdrop-filter: blur(10px);
    border-radius: 20px;
    padding: 20px;
    color: white;
  }

  .stats-card {
    background: rgba(255, 255, 255, 0.08) !important;
    backdrop-filter: blur(10px);
    color: white;
    border-radius: 20px;

    .training-status {
      background: rgba(255, 255, 255, 0.05);
      padding: 12px;
      border-radius: 12px;
    }

    .current-move-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: rgba(102, 126, 234, 0.2);
      border-radius: 12px;
      margin: 12px 0;
    }

    .hold-indicator {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px;
      background: rgba(255, 193, 7, 0.1);
      border-radius: 12px;
      margin: 12px 0;
      .hold-text { margin-top: 8px; font-size: 12px; color: #ffd700; }
    }

    .finger-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 6px;
      margin: 16px 0;
      .finger-stat {
        text-align: center;
        padding: 6px;
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.1);
        opacity: 0.5;
        transition: all 0.3s;
        &.active { opacity: 1; background: rgba(102, 126, 234, 0.3); transform: scale(1.05); }
        span { display: block; font-size: 10px; margin-top: 4px; }
      }
    }

    .message-box {
      padding: 10px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      font-size: 13px;
      &.success { background: rgba(76, 175, 80, 0.2); border: 1px solid #4caf50; }
      &.error { background: rgba(244, 67, 54, 0.2); border: 1px solid #f44336; }
      &.info { background: rgba(33, 150, 243, 0.2); border: 1px solid #2196f3; }
      &.calibration { background: rgba(255, 193, 7, 0.2); border: 1px solid #ffc107; }
    }

    .action-buttons .q-btn { height: 44px; font-size: 14px; }
  }
}
</style>
