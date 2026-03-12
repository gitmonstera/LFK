<template>
  <q-page class="profile-page">
    <div class="container q-pa-md">
      <!-- Заголовок с приветствием -->
      <div class="row items-center q-mb-lg">
        <q-avatar size="64px" class="q-mr-md">
          <img :src="user?.avatar || 'https://ui-avatars.com/api/?name=' + user?.username + '&background=667eea&color=fff&size=128'">
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
          <q-card flat class="stats-card">
            <q-card-section class="text-center">
              <q-icon name="fitness_center" size="32px" color="primary" />
              <div class="text-h5">{{ userStats?.workouts || 0 }}</div>
              <div class="text-caption">Тренировок</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="stats-card">
            <q-card-section class="text-center">
              <q-icon name="schedule" size="32px" color="positive" />
              <div class="text-h5">{{ userStats?.minutes || 0 }}</div>
              <div class="text-caption">Минут</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="stats-card">
            <q-card-section class="text-center">
              <q-icon name="local_fire_department" size="32px" color="orange" />
              <div class="text-h5">{{ userStats?.calories || 0 }}</div>
              <div class="text-caption">Калорий</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="stats-card">
            <q-card-section class="text-center">
              <q-icon name="emoji_events" size="32px" color="warning" />
              <div class="text-h5">{{ userStats?.streak || 0 }}</div>
              <div class="text-caption">Дней подряд</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

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
          <div class="text-center q-pa-xl">
            <q-icon name="fitness_center" size="64px" color="primary" class="q-mb-md" />
            <h3 class="text-h5">Список упражнений</h3>
            <p class="text-grey-7">Здесь будет список доступных упражнений</p>
          </div>
        </q-tab-panel>

        <!-- Вкладка Статистика -->
        <q-tab-panel name="stats">
          <div class="text-center q-pa-xl">
            <q-icon name="insights" size="64px" color="primary" class="q-mb-md" />
            <h3 class="text-h5">Статистика</h3>
            <p class="text-grey-7">Здесь будет ваша статистика тренировок</p>
          </div>
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'

const $q = useQuasar()
const router = useRouter()
const tab = ref('profile')

const user = ref(null)
const userStats = ref({
  workouts: 12,
  minutes: 345,
  calories: 2340,
  streak: 5
})

const formatDate = (date) => {
  if (!date) return 'Неизвестно'
  return new Date(date).toLocaleDateString('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
}

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

const loadUser = () => {
  const userData = localStorage.getItem('user')
  if (userData) {
    user.value = JSON.parse(userData)
  }
}

onMounted(() => {
  loadUser()
})
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;

  .container {
    max-width: 1200px;
    margin: 0 auto;
  }

  .stats-card {
    background: white;
    border-radius: 16px;
    transition: transform 0.3s;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 30px rgba(0,0,0,0.1);
    }
  }

  .profile-card {
    background: white;
    border-radius: 16px;
  }
}
</style>
