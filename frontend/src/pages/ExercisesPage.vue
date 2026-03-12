<template>
  <q-page class="exercises-page">
    <div class="container q-pa-md">
      <!-- Заголовок -->
      <div class="row items-center q-mb-lg">
        <q-btn flat round dense icon="arrow_back" to="/profile" class="q-mr-sm" />
        <div class="text-h4">Упражнения</div>
        <q-space />
        <q-input
          v-model="searchQuery"
          dense
          outlined
          placeholder="Поиск упражнений..."
          class="search-input"
          debounce="300"
        >
          <template v-slot:prepend>
            <q-icon name="search" />
          </template>
          <template v-slot:append>
            <q-icon v-if="searchQuery" name="close" @click="searchQuery = ''" class="cursor-pointer" />
          </template>
        </q-input>
      </div>

      <!-- Индикатор загрузки -->
      <div v-if="loading" class="text-center q-pa-xl">
        <q-spinner-gears size="50px" color="primary" />
        <p class="q-mt-md">Загрузка упражнений...</p>
      </div>

      <!-- Сетка упражнений -->
      <div v-else-if="filteredExercises.length > 0" class="row q-col-gutter-md">
        <div
          v-for="exercise in filteredExercises"
          :key="exercise.exercise_id"
          class="col-12 col-sm-6 col-md-4 col-lg-3"
        >
          <q-card
            class="exercise-card cursor-pointer"
            @click="goToExercise(exercise)"
          >
            <!-- В exercise-image блоке -->
            <div class="exercise-image" :style="{ backgroundColor: exercise.category_color || '#667eea' }">
              <template v-if="exercise.category_icon">
                <span class="exercise-emoji">{{ exercise.category_icon }}</span>
              </template>
              <q-icon
                v-else
                :name="getExerciseIcon(exercise.exercise_id, exercise.name, exercise.category)"
                size="48px"
                color="white"
              />
            </div>

            <!-- Контент -->
            <q-card-section>
              <div class="text-h6">{{ exercise.name }}</div>
              <div class="text-caption text-grey-7">{{ exercise.description || 'Нет описания' }}</div>
            </q-card-section>

            <!-- Теги -->
            <q-card-section class="q-pt-none">
              <q-chip
                size="sm"
                :color="getDifficultyColor(exercise.difficulty_level)"
                text-color="white"
              >
                Сложность: {{ exercise.difficulty_level || '?' }}
              </q-chip>
              <q-chip
                size="sm"
                :style="{ backgroundColor: exercise.category_color || '#667eea', color: 'white' }"
                :icon="exercise.category_icon"
              >
                {{ exercise.category }}
              </q-chip>
              <q-chip
                v-if="exercise.duration_seconds"
                size="sm"
                color="grey"
                text-color="white"
                icon="schedule"
              >
                {{ formatDuration(exercise.duration_seconds) }}
              </q-chip>
            </q-card-section>

            <!-- Целевые мышцы -->
            <q-card-section v-if="exercise.target_muscles" class="q-pt-none">
              <div class="text-caption text-grey-7">
                <q-icon name="fitness_center" size="xs" class="q-mr-xs" />
                {{ exercise.target_muscles.join(', ') }}
              </div>
            </q-card-section>

            <!-- Кнопка действия -->
            <q-card-actions align="right">
              <q-btn
                flat
                color="primary"
                label="Начать"
                icon="play_arrow"
                @click.stop="startExercise(exercise)"
              />
            </q-card-actions>
          </q-card>
        </div>
      </div>

      <!-- Пустое состояние -->
      <div v-else class="text-center q-pa-xl">
        <q-icon name="fitness_center" size="64px" color="grey-5" class="q-mb-md" />
        <h3 class="text-h5 text-grey-7">Упражнения не найдены</h3>
        <p class="text-grey-6" v-if="searchQuery">По запросу "{{ searchQuery }}" ничего не найдено</p>
        <p class="text-grey-6" v-else>Список упражнений пока пуст</p>
        <q-btn
          v-if="searchQuery"
          color="primary"
          label="Сбросить поиск"
          icon="clear"
          class="q-mt-md"
          @click="searchQuery = ''"
        />
      </div>
    </div>

    <!-- Диалог с деталями упражнения -->
    <q-dialog v-model="showExerciseDialog">
      <q-card style="min-width: 350px; max-width: 500px; width: 100%;">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">{{ selectedExercise?.name }}</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section>
          <div class="row items-center q-mb-md">
            <q-chip
              size="sm"
              :color="getDifficultyColor(selectedExercise?.difficulty_level)"
              text-color="white"
            >
              Сложность: {{ selectedExercise?.difficulty_level || '?' }}
            </q-chip>
            <q-chip
              size="sm"
              :style="{ backgroundColor: selectedExercise?.category_color || '#667eea', color: 'white' }"
              :icon="selectedExercise?.category_icon"
            >
              {{ selectedExercise?.category }}
            </q-chip>
          </div>

          <p class="text-grey-7">{{ selectedExercise?.description || 'Описание отсутствует' }}</p>

          <div class="q-mt-md" v-if="selectedExercise?.target_muscles">
            <div class="text-subtitle2">Целевые мышцы:</div>
            <div class="row q-col-gutter-sm q-mt-xs">
              <div v-for="muscle in selectedExercise.target_muscles" :key="muscle" class="col-12">
                <q-item dense>
                  <q-item-section avatar>
                    <q-icon name="fitness_center" size="xs" color="primary" />
                  </q-item-section>
                  <q-item-section>{{ muscle }}</q-item-section>
                </q-item>
              </div>
            </div>
          </div>

          <div class="q-mt-md" v-if="selectedExercise?.instructions">
            <div class="text-subtitle2">Инструкция:</div>
            <q-list dense>
              <q-item v-for="(instruction, index) in selectedExercise.instructions" :key="index">
                <q-item-section avatar>
                  <q-avatar size="24px" color="primary" text-color="white">
                    {{ index + 1 }}
                  </q-avatar>
                </q-item-section>
                <q-item-section>{{ instruction }}</q-item-section>
              </q-item>
            </q-list>
          </div>

          <div class="q-mt-md text-center" v-if="selectedExercise?.duration_seconds">
            <q-chip color="primary" text-color="white" icon="schedule">
              Длительность: {{ formatDuration(selectedExercise.duration_seconds) }}
            </q-chip>
          </div>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Закрыть" color="primary" v-close-popup />
          <q-btn flat label="Начать тренировку" color="primary" @click="startExercise(selectedExercise)" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from 'src/boot/axios'

const $q = useQuasar()
const router = useRouter()

// Состояние
const loading = ref(false)
const exercises = ref([])
const searchQuery = ref('')
const showExerciseDialog = ref(false)
const selectedExercise = ref(null)

// Фильтрация упражнений
const filteredExercises = computed(() => {
  if (!searchQuery.value) return exercises.value

  const query = searchQuery.value.toLowerCase()
  return exercises.value.filter(ex =>
    ex.name.toLowerCase().includes(query) ||
    (ex.description && ex.description.toLowerCase().includes(query)) ||
    (ex.category && ex.category.toLowerCase().includes(query))
  )
})

// Форматирование длительности
const formatDuration = (seconds) => {
  if (!seconds) return '0 сек'
  if (seconds < 60) return `${seconds} сек`
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return secs > 0 ? `${minutes} мин ${secs} сек` : `${minutes} мин`
}

// Получение иконки для упражнения
const getExerciseIcon = (exerciseId, exerciseName) => {
  // Словарь соответствия правильным Material Icons
  const icons = {
    // По ID упражнения
    'fist': 'back_hand',  // вместо 'fist'
    'fist-index': 'pinch',
    'fist-palm': 'tap_and_play',
    'finger-touching': 'pinch',
    'finger_touching': 'pinch',

    // По ключевым словам в названии
    'кулак': 'back_hand',
    'кисть': 'back_hand',
    'палец': 'pinch',
    'считалочка': 'pinch',
    'пальцы': 'pinch',
    'рука': 'back_hand',
    'руки': 'back_hand',
    'моторика': 'pinch',
    'растяжка': 'accessibility_new',
    'сила': 'fitness_center',
    'баланс': 'balance',
    'кардио': 'directions_run',

    // Общие
    'default': 'sports_gymnastics'
  }

  // Сначала проверяем по ID
  if (icons[exerciseId]) {
    return icons[exerciseId]
  }

  // Если не нашли по ID, ищем по названию
  if (exerciseName) {
    const name = exerciseName.toLowerCase()
    for (const [key, value] of Object.entries(icons)) {
      if (name.includes(key)) {
        return value
      }
    }
  }

  // Если ничего не нашли, возвращаем иконку по умолчанию
  return icons.default
}
// Цвет для сложности
const getDifficultyColor = (difficulty) => {
  const colors = {
    1: 'positive',
    2: 'warning',
    3: 'negative'
  }
  return colors[difficulty] || 'grey'
}

// Загрузка упражнений
const loadExercises = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/get_exercise_list')
    console.log('Exercises raw response:', response.data)

    // Обрабатываем разные форматы ответа
    let exercisesData = []
    if (response.data && response.data.items) {
      exercisesData = response.data.items
    } else if (Array.isArray(response.data)) {
      exercisesData = response.data
    } else {
      console.error('Unexpected response format:', response.data)
      exercisesData = []
    }

    exercises.value = exercisesData
    console.log('Exercises loaded:', exercises.value)
  } catch (error) {
    console.error('Error loading exercises:', error)
    $q.notify({
      type: 'negative',
      message: 'Ошибка загрузки упражнений'
    })
  } finally {
    loading.value = false
  }
}

// Переход к упражнению
const goToExercise = (exercise) => {
  selectedExercise.value = exercise
  showExerciseDialog.value = true
}

// Начать упражнение
const startExercise = (exercise) => {
  $q.notify({
    type: 'info',
    message: `Запуск упражнения "${exercise.name}"`,
    position: 'top'
  })
  // Здесь будет логика запуска тренировки
  showExerciseDialog.value = false

  // В будущем здесь будет переход на страницу выполнения упражнения
  // router.push(`/exercise/${exercise.exercise_id}`)
}

// Загрузка при монтировании
onMounted(() => {
  loadExercises()
})
</script>

<style lang="scss" scoped>
.exercise-emoji {
  font-size: 48px;
  line-height: 1;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}

.exercises-page {
  min-height: 100vh;
  background: #f5f5f5;

  .container {
    max-width: 1400px;
    margin: 0 auto;
  }

  .search-input {
    width: 300px;

    @media (max-width: 768px) {
      width: 100%;
      margin-top: 10px;
    }
  }

  .exercise-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    transition: all 0.3s;
    border-radius: 16px;
    overflow: hidden;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 20px 40px rgba(102, 126, 234, 0.2);
    }

    .exercise-image {
      height: 120px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .q-card__section {
      padding: 16px;
    }
  }
}
</style>
