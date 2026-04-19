<template>
  <q-page class="profile-page">
    <div class="container q-pa-md">
      <!-- Индикатор загрузки -->
      <q-inner-loading :showing="loading">
        <q-spinner-gears size="50px" color="primary" />
      </q-inner-loading>

      <!-- Заголовок с приветствием -->
      <div class="row items-center q-mb-lg">
        <q-avatar size="64px" class="q-mr-md">
          <img :src="userAvatar">
        </q-avatar>
        <div>
          <div class="text-h5">Привет, {{ user?.username }}!</div>
          <div class="text-grey-7">{{ user?.email }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="logout" color="negative" @click="logout" />
      </div>

      <!-- Статистика пользователя -->
      <div class="row q-col-gutter-md q-mb-lg">
        <div class="col-6 col-md-3">
          <q-card flat class="stats-card" @click="showDetailedStats">
            <q-card-section class="text-center">
              <q-icon name="fitness_center" size="32px" color="primary" />
              <div class="text-h5">{{ stats?.total_sessions || 0 }}</div>
              <div class="text-caption">Тренировок</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="stats-card" @click="showDetailedStats">
            <q-card-section class="text-center">
              <q-icon name="repeat" size="32px" color="positive" />
              <div class="text-h5">{{ stats?.total_exercises || 0 }}</div>
              <div class="text-caption">Упражнений</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="stats-card" @click="showDetailedStats">
            <q-card-section class="text-center">
              <q-icon name="repeat" size="32px" color="orange" />
              <div class="text-h5">{{ stats?.total_repetitions || 0 }}</div>
              <div class="text-caption">Повторений</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="stats-card" @click="showDetailedStats">
            <q-card-section class="text-center">
              <q-icon name="emoji_events" size="32px" color="warning" />
              <div class="text-h5">{{ stats?.current_streak || 0 }}</div>
              <div class="text-caption">Дней подряд</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Дополнительная статистика -->
      <div class="row q-col-gutter-md q-mb-lg">
        <div class="col-6 col-md-4">
          <q-card flat class="stats-card-small">
            <q-card-section class="text-center">
              <q-icon name="schedule" size="24px" color="primary" />
              <div class="text-subtitle1">{{ formatDuration(stats?.total_duration || 0) }}</div>
              <div class="text-caption">Общее время</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-4">
          <q-card flat class="stats-card-small">
            <q-card-section class="text-center">
              <q-icon name="stars" size="24px" color="positive" />
              <div class="text-subtitle1">{{ stats?.unique_exercises || 0 }}</div>
              <div class="text-caption">Уникальных</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-4">
          <q-card flat class="stats-card-small">
            <q-card-section class="text-center">
              <q-icon name="whatshot" size="24px" color="orange" />
              <div class="text-subtitle1">{{ stats?.longest_streak || 0 }}</div>
              <div class="text-caption">Макс. серия</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Последняя тренировка -->
      <q-card flat class="q-mb-lg" v-if="stats?.last_workout_at">
        <q-card-section>
          <div class="row items-center">
            <q-icon name="history" size="32px" color="primary" class="q-mr-md" />
            <div>
              <div class="text-subtitle1">Последняя тренировка</div>
              <div class="text-grey-7">{{ formatLastWorkout(stats.last_workout_at) }}</div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Навигация по разделам -->
      <q-tabs
        v-model="tab"
        dense
        class="text-grey"
        active-color="primary"
        indicator-color="primary"
        align="justify"
        narrow-indicator
      >
        <q-tab name="profile" label="Профиль" icon="person" />
        <q-tab name="exercises" label="Упражнения" icon="fitness_center" />
        <q-tab name="stats" label="Статистика" icon="insights" />
      </q-tabs>

      <q-separator class="q-my-md" />

      <q-tab-panels v-model="tab" animated>
        <!-- Вкладка Профиль -->
        <q-tab-panel name="profile">
          <q-card flat class="profile-card">
            <q-card-section>
              <div class="text-h6 q-mb-md">Информация профиля</div>

              <q-list>
                <q-item>
                  <q-item-section avatar>
                    <q-icon name="person" color="primary" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>Имя пользователя</q-item-label>
                    <q-item-label caption>{{ user?.username }}</q-item-label>
                  </q-item-section>
                </q-item>

                <q-item>
                  <q-item-section avatar>
                    <q-icon name="email" color="primary" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>Email</q-item-label>
                    <q-item-label caption>{{ user?.email }}</q-item-label>
                  </q-item-section>
                </q-item>

                <q-item>
                  <q-item-section avatar>
                    <q-icon name="calendar_today" color="primary" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>Дата регистрации</q-item-label>
                    <q-item-label caption>{{ formatDate(user?.createdAt) }}</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-card-section>

            <q-card-actions align="right">
              <q-btn flat color="primary" label="Редактировать" icon="edit" />
            </q-card-actions>
          </q-card>
        </q-tab-panel>

        <!-- Вкладка Упражнения -->
        <q-tab-panel name="exercises">
          <div class="row items-center justify-between q-mb-md">
            <div class="text-h6">Доступные упражнения</div>
            <q-btn
              color="primary"
              label="Все упражнения"
              icon="arrow_forward"
              to="/profile/exercises"
              outline
            />
          </div>

          <div v-if="previewExercises.length > 0" class="row q-col-gutter-md">
            <div
              v-for="exercise in previewExercises"
              :key="exercise.exercise_id"
              class="col-12 col-md-4"
            >
              <q-card class="exercise-preview cursor-pointer" @click="router.push('/profile/exercises')">
                <q-card-section class="text-center">
                  <q-icon :name="getExerciseIcon(exercise.exercise_id)" size="32px" color="primary" />
                  <div class="text-subtitle1 q-mt-sm">{{ exercise.name }}</div>
                  <div class="text-caption text-grey-7">{{ exercise.category }}</div>
                </q-card-section>
              </q-card>
            </div>
          </div>

          <div v-else class="text-center text-grey-7 q-pa-md">
            <q-spinner v-if="loadingPreview" color="primary" size="2em" />
            <p v-else>Нет доступных упражнений</p>
          </div>
        </q-tab-panel>

        <!-- Вкладка Статистика -->
        <q-tab-panel name="stats">
          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-6">
              <q-card flat class="stats-detail-card">
                <q-card-section>
                  <div class="text-h6">Общая статистика</div>
                  <q-separator class="q-my-md" />
                  <q-list dense>
                    <q-item>
                      <q-item-section>Всего тренировок</q-item-section>
                      <q-item-section side>{{ stats?.total_sessions || 0 }}</q-item-section>
                    </q-item>
                    <q-item>
                      <q-item-section>Всего упражнений</q-item-section>
                      <q-item-section side>{{ stats?.total_exercises || 0 }}</q-item-section>
                    </q-item>
                    <q-item>
                      <q-item-section>Всего повторений</q-item-section>
                      <q-item-section side>{{ stats?.total_repetitions || 0 }}</q-item-section>
                    </q-item>
                    <q-item>
                      <q-item-section>Общее время</q-item-section>
                      <q-item-section side>{{ formatDuration(stats?.total_duration || 0) }}</q-item-section>
                    </q-item>
                  </q-list>
                </q-card-section>
              </q-card>
            </div>

            <div class="col-12 col-md-6">
              <q-card flat class="stats-detail-card">
                <q-card-section>
                  <div class="text-h6">Достижения</div>
                  <q-separator class="q-my-md" />
                  <q-list dense>
                    <q-item>
                      <q-item-section>Уникальных упражнений</q-item-section>
                      <q-item-section side>{{ stats?.unique_exercises || 0 }}</q-item-section>
                    </q-item>
                    <q-item>
                      <q-item-section>Текущая серия</q-item-section>
                      <q-item-section side>{{ stats?.current_streak || 0 }} дней</q-item-section>
                    </q-item>
                    <q-item>
                      <q-item-section>Максимальная серия</q-item-section>
                      <q-item-section side>{{ stats?.longest_streak || 0 }} дней</q-item-section>
                    </q-item>
                  </q-list>
                </q-card-section>
              </q-card>
            </div>
          </div>
        </q-tab-panel>
      </q-tab-panels>
    </div>

    <!-- Диалог детальной статистики -->
    <q-dialog v-model="statsDialog.show">
      <q-card style="min-width: 350px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Детальная статистика</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section>
          <q-list>
            <q-item>
              <q-item-section avatar><q-icon name="fitness_center" color="primary" /></q-item-section>
              <q-item-section>Тренировок</q-item-section>
              <q-item-section side>{{ stats?.total_sessions || 0 }}</q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="repeat" color="positive" /></q-item-section>
              <q-item-section>Упражнений</q-item-section>
              <q-item-section side>{{ stats?.total_exercises || 0 }}</q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="repeat" color="orange" /></q-item-section>
              <q-item-section>Повторений</q-item-section>
              <q-item-section side>{{ stats?.total_repetitions || 0 }}</q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="schedule" color="primary" /></q-item-section>
              <q-item-section>Время</q-item-section>
              <q-item-section side>{{ formatDuration(stats?.total_duration || 0) }}</q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="stars" color="positive" /></q-item-section>
              <q-item-section>Уникальных</q-item-section>
              <q-item-section side>{{ stats?.unique_exercises || 0 }}</q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="whatshot" color="orange" /></q-item-section>
              <q-item-section>Текущая серия</q-item-section>
              <q-item-section side>{{ stats?.current_streak || 0 }} дней</q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="emoji_events" color="warning" /></q-item-section>
              <q-item-section>Макс. серия</q-item-section>
              <q-item-section side>{{ stats?.longest_streak || 0 }} дней</q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from 'src/boot/axios'

const $q = useQuasar()
const router = useRouter()
const tab = ref('profile')
const loading = ref(false)
const loadingPreview = ref(false)

// Данные
const user = ref(null)
const stats = ref({})
const previewExercises = ref([])

// Диалоги
const statsDialog = ref({
  show: false
})

// Вычисляемые свойства
const userAvatar = computed(() => {
  return user.value?.avatar || `https://ui-avatars.com/api/?name=${user.value?.username || 'User'}&background=667eea&color=fff&size=128`
})

// Форматирование
const formatDate = (date) => {
  if (!date) return 'Неизвестно'
  return new Date(date).toLocaleDateString('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
}

const formatDuration = (seconds) => {
  if (!seconds) return '0 мин'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)

  if (hours > 0) {
    return `${hours} ч ${minutes} мин`
  }
  return `${minutes} мин`
}

const formatLastWorkout = (lastWorkout) => {
  if (!lastWorkout) return 'Нет данных'

  let dateStr = lastWorkout
  if (typeof lastWorkout === 'object' && lastWorkout.Time) {
    dateStr = lastWorkout.Time
  }

  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return String(dateStr).slice(0, 10)
  }
}

const getExerciseIcon = (type) => {
  const icons = {
    'fist': 'fist',
    'fist-index': 'pinch',
    'fist-palm': 'tap',
    'finger-touching': 'pinch',
    'default': 'sports_gymnastics'
  }
  return icons[type] || icons.default
}

// Загрузка данных
const loadOverallStats = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/stats/overall')
    stats.value = response.data
    console.log('Stats loaded:', stats.value)
  } catch (error) {
    console.error('Error loading stats:', error)
    $q.notify({
      type: 'negative',
      message: 'Ошибка загрузки статистики'
    })
  } finally {
    loading.value = false
  }
}

const loadPreviewExercises = async () => {
  loadingPreview.value = true
  try {
    const response = await api.get('/api/get_exercise_list')
    console.log('Preview exercises raw response:', response.data)

    // Проверяем структуру ответа
    let exercisesData = []
    if (response.data && response.data.items) {
      exercisesData = response.data.items
    } else if (Array.isArray(response.data)) {
      exercisesData = response.data
    } else {
      console.error('Unexpected response format:', response.data)
      exercisesData = []
    }

    previewExercises.value = exercisesData.slice(0, 3)
    console.log('Preview exercises loaded:', previewExercises.value)
  } catch (error) {
    console.error('Error loading preview exercises:', error)
  } finally {
    loadingPreview.value = false
  }
}

// Действия
const logout = () => {
  $q.dialog({
    title: 'Выход',
    message: 'Вы уверены, что хотите выйти?',
    cancel: true,
    persistent: true
  }).onOk(() => {
    localStorage.removeItem('token')
    sessionStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/')
    $q.notify({
      type: 'positive',
      message: 'Вы вышли из системы'
    })
  })
}

const showDetailedStats = () => {
  statsDialog.value.show = true
}

const loadUser = () => {
  const userData = localStorage.getItem('user')
  if (userData) {
    user.value = JSON.parse(userData)
  }
}

// Загрузка при монтировании
onMounted(() => {
  loadUser()
  loadOverallStats()
  loadPreviewExercises()
})
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f5f5;

  .container {
    max-width: 1200px;
    margin: 0 auto;
  }

  .stats-card {
    background: white;
    border-radius: 16px;
    transition: all 0.3s;
    cursor: pointer;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
    }
  }

  .stats-card-small {
    background: white;
    border-radius: 12px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
    }
  }

  .profile-card {
    background: white;
    border-radius: 16px;
  }

  .exercise-preview {
    transition: all 0.3s;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 5px 20px rgba(102, 126, 234, 0.2);
    }
  }

  .stats-detail-card {
    background: white;
    border-radius: 12px;
    height: 100%;
  }
}
</style>
