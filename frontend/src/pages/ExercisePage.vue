<template>
  <q-page class="exercise-page">
    <!-- Заголовок -->
    <div class="header">
      <div class="container">
        <div class="row items-center q-pa-md">
          <q-btn flat round dense icon="arrow_back" @click="confirmExit" class="q-mr-sm text-white" />
          <div class="text-h5 text-white">{{ exerciseName }}</div>
          <q-space />
          <q-chip v-if="connectionStatus === 'connected'" color="positive" text-color="white" icon="wifi">
            Подключено
          </q-chip>
          <q-chip v-else-if="connectionStatus === 'connecting'" color="warning" text-color="white" icon="wifi">
            Подключение...
          </q-chip>
          <q-chip v-else color="negative" text-color="white" icon="wifi_off">
            Отключено
          </q-chip>
        </div>
      </div>
    </div>

    <!-- Основной контент -->
    <div class="content">
      <div class="container">
        <div class="row">
          <!-- Левая колонка - видео -->
          <div class="col-12 col-md-8">
            <div class="video-container">
              <!-- Индикатор загрузки камеры -->
              <div v-if="!cameraReady" class="camera-loading">
                <q-spinner size="50px" color="white" />
                <p class="text-white q-mt-md">Запуск камеры...</p>
              </div>

              <!-- Видео с сервера (обработанное) -->
              <img
                v-if="processedFrame"
                :src="processedFrame"
                class="processed-video"
                alt="Processed video"
              />

              <!-- Локальное видео (сырое) -->
              <video
                ref="videoRef"
                autoplay
                playsinline
                muted
                class="local-video"
                :class="{ 'hidden': !showLocalVideo }"
              ></video>

              <!-- Детекция руки -->
              <div class="hand-detection" v-if="handDetected !== null">
                <q-badge :color="handDetected ? 'positive' : 'negative'" class="hand-badge">
                  <q-icon :name="handDetected ? 'back_hand' : 'pan_tool'" size="20px" />
                  {{ handDetected ? 'Рука в кадре' : 'Рука не обнаружена' }}
                </q-badge>
              </div>

              <!-- Информация о пальцах -->
              <div class="finger-info" v-if="fingerStates.length > 0">
                <div class="finger-row">
                  <div
                    v-for="(state, index) in fingerStates"
                    :key="index"
                    class="finger-indicator"
                    :class="{ active: state }"
                  >
                    <q-icon :name="getFingerIcon(index)" size="24px" />
                    <span>{{ getFingerName(index) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Правая колонка - статистика и управление -->
          <div class="col-12 col-md-4">
            <q-card flat class="stats-card">
              <q-card-section>
                <div class="text-h6">Прогресс упражнения</div>
                <q-separator class="q-my-md" />

                <!-- Для упражнения "Кулак-ладонь" (fist-palm) -->
                <template v-if="exerciseId === 'fist-palm'">
                  <div class="progress-section">
                    <div class="row items-center">
                      <q-icon name="repeat" size="24px" color="primary" />
                      <span class="q-ml-sm">Цикл {{ currentCycle }}/{{ totalCycles }}</span>
                    </div>

                    <q-linear-progress
                      :value="currentCycle / totalCycles"
                      color="primary"
                      class="q-mt-sm q-mb-md"
                      size="20px"
                    />

                    <div class="step-indicator">
                      <div
                        v-for="(step, index) in exerciseSteps"
                        :key="index"
                        class="step-item"
                        :class="{
                          completed: index < currentStepIndex,
                          active: index === currentStepIndex,
                          pending: index > currentStepIndex
                        }"
                      >
                        <div class="step-number">{{ index + 1 }}</div>
                        <div class="step-name">{{ step.name }}</div>
                        <div v-if="index === currentStepIndex && countdown" class="step-timer">
                          {{ countdown }}с
                        </div>
                      </div>
                    </div>
                  </div>
                </template>

                <!-- Для обычных упражнений -->
                <template v-else>
                  <div class="stats-grid">
                    <div class="stat-item">
                      <q-icon name="back_hand" size="32px" color="primary" />
                      <div class="stat-value">{{ raisedFingers }}/5</div>
                      <div class="stat-label">Пальцев поднято</div>
                    </div>
                  </div>

                  <div class="finger-grid">
                    <div
                      v-for="i in 5"
                      :key="i"
                      class="finger-stat"
                      :class="{ active: fingerStates[i-1] }"
                    >
                      <q-icon :name="getFingerIcon(i-1)" size="24px" />
                      <span>{{ getFingerName(i-1) }}</span>
                    </div>
                  </div>
                </template>

                <!-- Сообщение от сервера -->
                <div v-if="serverMessage" class="message-box q-mt-md" :class="messageClass">
                  <q-icon :name="messageIcon" size="20px" class="q-mr-sm" />
                  {{ serverMessage }}
                </div>

                <!-- Кнопки управления -->
                <div class="action-buttons q-mt-lg">
                  <q-btn
                    v-if="!exerciseCompleted"
                    :loading="savingStats"
                    color="negative"
                    label="Завершить досрочно"
                    icon="stop"
                    class="full-width"
                    @click="endWorkoutEarly"
                  />
                  <q-btn
                    v-else
                    color="positive"
                    label="Упражнение выполнено!"
                    icon="check_circle"
                    class="full-width"
                    disabled
                  />
                </div>
              </q-card-section>
            </q-card>
          </div>
        </div>
      </div>
    </div>

    <!-- Диалог подтверждения выхода -->
    <q-dialog v-model="showExitDialog">
      <q-card>
        <q-card-section>
          <div class="text-h6">Завершить тренировку?</div>
        </q-card-section>
        <q-card-section>
          Прогресс будет сохранен. Вы уверены?
        </q-card-section>
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

// Параметры
const exerciseId = route.params.id
const exerciseName = ref('')

// Состояние WebSocket
const ws = ref(null)
const connectionStatus = ref('disconnected')
const lastMessageTime = ref(Date.now())
const connectionCheckInterval = ref(null)

// Состояние камеры
const videoRef = ref(null)
const cameraReady = ref(false)
const showLocalVideo = ref(true)
const processedFrame = ref(null)
const frameInterval = ref(null)

// Данные от сервера
const handDetected = ref(null)
const fingerStates = ref([])
const raisedFingers = ref(0)
const serverMessage = ref('')
const messageClass = ref('')

// Для fist-palm упражнения
const currentCycle = ref(0)
const totalCycles = ref(5)
const countdown = ref(null)
const currentStepIndex = ref(-1)
const exerciseCompleted = ref(false)

// Статистика тренировки
const sessionId = ref(null)
const savingStats = ref(false)
const setsCompleted = ref(0)
const statsSavedForCycle = ref(new Set())

// UI состояния
const showExitDialog = ref(false)

// Шаги для fist-palm
const exerciseSteps = [
  { name: 'Сожмите кулак', state: 'waiting_fist' },
  { name: 'Держите кулак', state: 'holding_fist' },
  { name: 'Раскройте ладонь', state: 'waiting_palm' },
  { name: 'Держите ладонь', state: 'holding_palm' }
]

// Вычисляемые свойства
const messageIcon = computed(() => {
  if (serverMessage.value.includes('✅') || serverMessage.value.includes('поздравляю')) {
    return 'check_circle'
  } else if (serverMessage.value.includes('❌') || serverMessage.value.includes('ошибка')) {
    return 'error'
  } else {
    return 'info'
  }
})

// Функции
const getFingerIcon = (index) => {
  const icons = ['thumb_up', 'index', 'middle', 'ring', 'mic']
  return icons[index] || 'fingers'
}

const getFingerName = (index) => {
  const names = ['Большой', 'Указательный', 'Средний', 'Безымянный', 'Мизинец']
  return names[index] || ''
}

// Загрузка информации об упражнении
const loadExerciseInfo = async () => {
  try {
    const response = await api.get('/api/get_exercise_list')
    let exercisesData = []
    if (response.data && response.data.items) {
      exercisesData = response.data.items
    } else if (Array.isArray(response.data)) {
      exercisesData = response.data
    }

    const exercise = exercisesData.find(ex => ex.exercise_id === exerciseId)
    if (exercise) {
      exerciseName.value = exercise.name
    } else {
      exerciseName.value = 'Упражнение'
    }
  } catch (error) {
    console.error('Error loading exercise info:', error)
  }
}

// Начать тренировку
const startWorkout = async () => {
  try {
    const response = await api.post('/api/workout/start')
    sessionId.value = response.data.id
    console.log('Тренировка начата:', sessionId.value)
  } catch (error) {
    console.error('Error starting workout:', error)
    $q.notify({
      type: 'negative',
      message: 'Ошибка начала тренировки'
    })
  }
}

// Завершить тренировку
const endWorkout = async () => {
  if (!sessionId.value) return

  try {
    await api.post('/api/workout/end', { session_id: sessionId.value })
    console.log('Тренировка завершена')
  } catch (error) {
    console.error('Error ending workout:', error)
  }
}

// Добавить выполненное упражнение
const addExerciseSet = async (repetitions, duration, accuracy) => {
  if (!sessionId.value) return false

  savingStats.value = true
  try {
    await api.post('/api/workout/exercise', {
      session_id: sessionId.value,
      exercise_id: exerciseId,
      actual_repetitions: repetitions,
      actual_duration: duration,
      accuracy_score: accuracy
    })
    console.log('Статистика сохранена')
    return true
  } catch (error) {
    console.error('Error saving exercise stats:', error)
    return false
  } finally {
    savingStats.value = false
  }
}

// Сбросить упражнение на сервере
const resetExercise = async () => {
  try {
    await api.post('/api/exercise/reset', { exercise_type: exerciseId })
    console.log('Упражнение сброшено')
  } catch (error) {
    console.error('Error resetting exercise:', error)
  }
}

// Инициализация камеры
const initCamera = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 640 },
        height: { ideal: 480 },
        facingMode: 'user'
      },
      audio: false
    })

    if (videoRef.value) {
      videoRef.value.srcObject = stream

      videoRef.value.onloadedmetadata = () => {
        videoRef.value.play()
        cameraReady.value = true
        console.log('✅ Камера инициализирована')
      }
    }
  } catch (error) {
    console.error('❌ Ошибка доступа к камере:', error)
    $q.notify({
      type: 'negative',
      message: 'Не удалось получить доступ к камере'
    })
  }
}

// Отправка кадров на сервер
const startFrameCapture = () => {
  if (!videoRef.value || !ws.value) return

  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 480
  const ctx = canvas.getContext('2d')

  frameInterval.value = setInterval(() => {
    if (!cameraReady.value || !ws.value || ws.value.readyState !== WebSocket.OPEN) return

    try {
      ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)

      canvas.toBlob((blob) => {
        const reader = new FileReader()
        reader.onloadend = () => {
          const base64data = reader.result.split(',')[1]

          ws.value.send(JSON.stringify({
            frame: base64data,
            exercise_type: exerciseId
          }))
        }
        reader.readAsDataURL(blob)
      }, 'image/jpeg', 0.5)

    } catch (error) {
      console.error('Ошибка отправки кадра:', error)
    }
  }, 200)
}

// Проверка соединения
const startConnectionCheck = () => {
  connectionCheckInterval.value = setInterval(() => {
    const timeSinceLastMessage = Date.now() - lastMessageTime.value

    if (timeSinceLastMessage > 10000 && connectionStatus.value === 'connected') {
      console.log('No messages for 10 seconds, reconnecting...')
      reconnect()
    }
  }, 5000)
}

// Переподключение
const reconnect = () => {
  if (ws.value) {
    ws.value.close()
  }

  if (frameInterval.value) {
    clearInterval(frameInterval.value)
  }

  setTimeout(() => {
    connectWebSocket()
  }, 1000)
}

// Подключение WebSocket
const connectWebSocket = () => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return
  }

  connectionStatus.value = 'connecting'

  const wsUrl = process.env.NODE_ENV === 'production'
    ? `wss://${window.location.host}/ws/exercise/${exerciseId}`
    : `ws://localhost:9000/ws/exercise/${exerciseId}`

  console.log('Connecting to:', wsUrl)

  ws.value = new WebSocket(`${wsUrl}?token=${token}`)

  ws.value.onopen = () => {
    console.log('✅ WebSocket connected')
    connectionStatus.value = 'connected'
    lastMessageTime.value = Date.now()
    startConnectionCheck()
    startWorkout()
    startFrameCapture()
  }

  ws.value.onmessage = (event) => {
    lastMessageTime.value = Date.now()

    try {
      const data = JSON.parse(event.data)

      if (data.processed_frame) {
        processedFrame.value = `data:image/jpeg;base64,${data.processed_frame}`
      }

      handDetected.value = data.hand_detected
      fingerStates.value = data.finger_states || []
      raisedFingers.value = data.raised_fingers || 0
      serverMessage.value = data.message || ''

      if (serverMessage.value.includes('✅')) {
        messageClass.value = 'success'
      } else if (serverMessage.value.includes('❌')) {
        messageClass.value = 'error'
      } else {
        messageClass.value = 'info'
      }

      if (exerciseId === 'fist-palm' && data.structured) {
        const prevCycle = currentCycle.value
        currentCycle.value = data.structured.current_cycle || 0
        totalCycles.value = data.structured.total_cycles || 5
        countdown.value = data.structured.countdown

        const state = data.structured.state
        const stepIndex = exerciseSteps.findIndex(s => s.state === state)
        currentStepIndex.value = stepIndex

        // Отслеживаем завершение цикла
        if (currentCycle.value > prevCycle && prevCycle > 0) {
          setsCompleted.value = currentCycle.value
          $q.notify({
            type: 'positive',
            message: `✅ Цикл ${currentCycle.value}/${totalCycles.value} завершен!`,
            position: 'top',
            timeout: 1000
          })
        }

        // Проверяем завершение упражнения
        if (data.structured.completed && !exerciseCompleted.value) {
          handleExerciseComplete()
        }
      }
    } catch (error) {
      console.error('Error parsing message:', error)
    }
  }

  ws.value.onerror = (error) => {
    console.error('❌ WebSocket error:', error)
    connectionStatus.value = 'disconnected'
    $q.notify({
      type: 'negative',
      message: 'Ошибка WebSocket соединения'
    })
  }

  ws.value.onclose = () => {
    console.log('WebSocket closed')
    connectionStatus.value = 'disconnected'

    if (connectionCheckInterval.value) {
      clearInterval(connectionCheckInterval.value)
    }
  }
}

// Обработка завершения упражнения
const handleExerciseComplete = () => {
  exerciseCompleted.value = true

  $q.notify({
    type: 'positive',
    message: '🎉 Упражнение выполнено!',
    position: 'top',
    timeout: 2000
  })

  endWorkout()

  $q.dialog({
    title: '🎉 Поздравляем!',
    message: 'Упражнение успешно выполнено. Хотите повторить или вернуться к списку?',
    cancel: {
      label: 'К списку',
      color: 'primary',
      flat: true
    },
    ok: {
      label: 'Повторить',
      color: 'positive',
      flat: true
    },
    persistent: true
  }).onOk(() => {
    resetAndRestart()
  }).onCancel(() => {
    exitExercise()
  })
}

// Сброс и повторное выполнение
const resetAndRestart = async () => {
  exerciseCompleted.value = false
  currentCycle.value = 0
  currentStepIndex.value = -1
  countdown.value = null
  statsSavedForCycle.value.clear()
  setsCompleted.value = 0

  await resetExercise()

  if (ws.value) {
    ws.value.close()
  }

  if (frameInterval.value) {
    clearInterval(frameInterval.value)
  }

  connectWebSocket()
}

// Досрочное завершение
const endWorkoutEarly = () => {
  showExitDialog.value = true
}

// Выход из упражнения
const exitExercise = async () => {
  $q.loading.show({
    message: 'Завершение тренировки...'
  })

  if (sessionId.value) {
    await endWorkout()
  }

  if (ws.value) {
    ws.value.close()
  }

  if (frameInterval.value) {
    clearInterval(frameInterval.value)
  }

  if (videoRef.value?.srcObject) {
    videoRef.value.srcObject.getTracks().forEach(track => track.stop())
  }

  if (connectionCheckInterval.value) {
    clearInterval(connectionCheckInterval.value)
  }

  $q.loading.hide()
  router.push('/profile/exercises')
}

// Подтверждение выхода
const confirmExit = () => {
  if (exerciseCompleted.value) {
    exitExercise()
  } else {
    showExitDialog.value = true
  }
}

// Обработка закрытия страницы
const handleBeforeUnload = (event) => {
  if (!exerciseCompleted.value && sessionId.value) {
    event.preventDefault()
    event.returnValue = 'Тренировка еще не завершена. Вы уверены?'
  }
}

// Очистка ресурсов
const cleanup = () => {
  if (frameInterval.value) {
    clearInterval(frameInterval.value)
  }

  if (connectionCheckInterval.value) {
    clearInterval(connectionCheckInterval.value)
  }

  if (ws.value) {
    ws.value.close()
  }

  if (videoRef.value?.srcObject) {
    videoRef.value.srcObject.getTracks().forEach(track => track.stop())
  }
}

// Монтирование компонента
onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  await loadExerciseInfo()
  await initCamera()
  connectWebSocket()
})

// Демонтирование
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  cleanup()
})
</script>

<style lang="scss" scoped>
.exercise-page {
  min-height: 100vh;
  background: #0a0a1f;
  color: white;

  .header {
    background: linear-gradient(135deg, #667eea, #764ba2);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
  }

  .container {
    max-width: 1400px;
    margin: 0 auto;
  }

  .content {
    margin-top: 80px;
    padding: 20px;
  }

  .video-container {
    position: relative;
    background: #000;
    border-radius: 16px;
    overflow: hidden;
    aspect-ratio: 4/3;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);

    .local-video,
    .processed-video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .local-video {
      position: absolute;
      top: 0;
      left: 0;

      &.hidden {
        opacity: 0;
      }
    }

    .camera-loading {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.7);
    }

    .hand-detection {
      position: absolute;
      top: 20px;
      left: 20px;

      .hand-badge {
        padding: 8px 16px;
        font-size: 16px;
        border-radius: 20px;
        backdrop-filter: blur(5px);
      }
    }

    .finger-info {
      position: absolute;
      bottom: 20px;
      left: 20px;
      right: 20px;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(5px);
      border-radius: 12px;
      padding: 12px;

      .finger-row {
        display: flex;
        justify-content: space-around;
      }

      .finger-indicator {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        padding: 8px;
        border-radius: 8px;
        transition: all 0.3s;
        opacity: 0.5;

        &.active {
          opacity: 1;
          background: rgba(102, 126, 234, 0.3);
          transform: scale(1.1);
        }

        span {
          font-size: 12px;
        }
      }
    }
  }

  .stats-card {
    background: rgba(255, 255, 255, 0.1) !important;
    backdrop-filter: blur(10px);
    color: white;
    border-radius: 16px;

    .q-separator {
      background: rgba(255, 255, 255, 0.2);
    }

    .stats-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 20px;
      margin: 20px 0;

      .stat-item {
        text-align: center;

        .stat-value {
          font-size: 48px;
          font-weight: bold;
          color: #667eea;
        }

        .stat-label {
          color: rgba(255, 255, 255, 0.7);
        }
      }
    }

    .finger-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 8px;
      margin: 20px 0;

      .finger-stat {
        text-align: center;
        padding: 8px;
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.1);
        opacity: 0.5;
        transition: all 0.3s;

        &.active {
          opacity: 1;
          background: rgba(102, 126, 234, 0.3);
          transform: scale(1.05);
        }

        span {
          display: block;
          font-size: 12px;
          margin-top: 4px;
        }
      }
    }

    .progress-section {
      .step-indicator {
        margin: 20px 0;

        .step-item {
          display: flex;
          align-items: center;
          padding: 12px;
          margin: 4px 0;
          border-radius: 8px;
          background: rgba(255, 255, 255, 0.1);
          transition: all 0.3s;

          &.completed {
            opacity: 0.7;

            .step-number {
              background: #4caf50;
            }
          }

          &.active {
            background: rgba(102, 126, 234, 0.3);
            transform: translateX(5px);

            .step-number {
              background: #667eea;
              transform: scale(1.1);
            }
          }

          .step-number {
            width: 30px;
            height: 30px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.2);
            margin-right: 12px;
            font-weight: bold;
            transition: all 0.3s;
          }

          .step-name {
            flex: 1;
          }

          .step-timer {
            color: #ffd700;
            font-weight: bold;
          }
        }
      }
    }

    .message-box {
      padding: 12px;
      border-radius: 8px;
      display: flex;
      align-items: center;

      &.success {
        background: rgba(76, 175, 80, 0.2);
        border: 1px solid #4caf50;
      }

      &.error {
        background: rgba(244, 67, 54, 0.2);
        border: 1px solid #f44336;
      }

      &.info {
        background: rgba(33, 150, 243, 0.2);
        border: 1px solid #2196f3;
      }
    }

    .action-buttons {
      .q-btn {
        height: 56px;
        font-size: 16px;
      }
    }
  }
}
</style>
