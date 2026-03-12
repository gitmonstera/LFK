<template>
  <q-layout view="lHh Lpr lFf" class="login-layout">
    <q-page-container>
      <q-page class="row items-center justify-center">
        <div class="login-container">
          <q-card class="login-card q-pa-lg">
            <q-card-section class="text-center">
              <q-avatar size="80px" class="q-mb-md">
                <img src="/logo/logo.svg" alt="LFK">
              </q-avatar>
              <h2 class="text-h4 gradient-text">Добро пожаловать!</h2>
              <p class="text-grey-7">Войдите или зарегистрируйтесь</p>
            </q-card-section>

            <q-card-section>
              <q-tabs
                v-model="tab"
                dense
                class="text-grey"
                active-color="primary"
                indicator-color="primary"
                align="justify"
                narrow-indicator
              >
                <q-tab name="login" label="Вход" />
                <q-tab name="register" label="Регистрация" />
              </q-tabs>

              <q-separator class="q-mb-md" />

              <q-tab-panels v-model="tab" animated>
                <!-- Панель входа -->
                <q-tab-panel name="login">
                  <q-form @submit="onLogin" class="q-gutter-md">
                    <q-input
                      v-model="loginForm.email"
                      type="email"
                      label="Email"
                      placeholder="your@email.com"
                      :rules="[val => !!val || 'Обязательное поле']"
                      outlined
                      dense
                    >
                      <template v-slot:prepend>
                        <q-icon name="email" />
                      </template>
                    </q-input>

                    <q-input
                      v-model="loginForm.password"
                      type="password"
                      label="Пароль"
                      :rules="[val => !!val || 'Обязательное поле']"
                      outlined
                      dense
                    >
                      <template v-slot:prepend>
                        <q-icon name="lock" />
                      </template>
                    </q-input>

                    <q-btn
                      type="submit"
                      label="Войти"
                      color="primary"
                      size="lg"
                      class="full-width"
                      :loading="loading"
                      unelevated
                    />
                  </q-form>
                </q-tab-panel>

                <!-- Панель регистрации -->
                <q-tab-panel name="register">
                  <q-form @submit="onRegister" class="q-gutter-md">
                    <q-input
                      v-model="registerForm.username"
                      label="Имя пользователя"
                      :rules="[val => !!val || 'Обязательное поле']"
                      outlined
                      dense
                    >
                      <template v-slot:prepend>
                        <q-icon name="person" />
                      </template>
                    </q-input>

                    <q-input
                      v-model="registerForm.email"
                      type="email"
                      label="Email"
                      :rules="[
                        val => !!val || 'Обязательное поле',
                        val => /.+@.+\..+/.test(val) || 'Введите корректный email'
                      ]"
                      outlined
                      dense
                    >
                      <template v-slot:prepend>
                        <q-icon name="email" />
                      </template>
                    </q-input>

                    <q-input
                      v-model="registerForm.password"
                      type="password"
                      label="Пароль"
                      :rules="[
                        val => !!val || 'Обязательное поле',
                        val => val.length >= 6 || 'Минимум 6 символов'
                      ]"
                      outlined
                      dense
                    >
                      <template v-slot:prepend>
                        <q-icon name="lock" />
                      </template>
                    </q-input>

                    <q-btn
                      type="submit"
                      label="Зарегистрироваться"
                      color="primary"
                      size="lg"
                      class="full-width"
                      :loading="loading"
                      unelevated
                    />
                  </q-form>
                </q-tab-panel>
              </q-tab-panels>
            </q-card-section>
          </q-card>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter } from 'vue-router'
import { api } from 'src/boot/axios'

const $q = useQuasar()
const router = useRouter()

const tab = ref('login')
const loading = ref(false)

const loginForm = ref({
  email: '',
  password: ''
})

const registerForm = ref({
  username: '',
  email: '',
  password: ''
})

const onLogin = async () => {
  loading.value = true
  try {
    const response = await api.post('/api/login', {  // Убираем лишний /api
      email: loginForm.value.email,
      password: loginForm.value.password
    })

    console.log('Login response:', response.data)

    const token = response.data.token
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(response.data.user))

    $q.notify({
      type: 'positive',
      message: 'Успешный вход!',
      position: 'top'
    })

    router.push('/profile')
  } catch (error) {
    console.error('Login error:', error)
    $q.notify({
      type: 'negative',
      message: error.response?.data?.error || 'Ошибка входа',
      position: 'top'
    })
  } finally {
    loading.value = false
  }
}

const onRegister = async () => {
  loading.value = true
  try {
    const response = await api.post('/api/register', {  // Убираем лишний /api
      username: registerForm.value.username,
      email: registerForm.value.email,
      password: registerForm.value.password
    })

    console.log('Register response:', response.data)

    $q.notify({
      type: 'positive',
      message: 'Регистрация успешна! Теперь войдите',
      position: 'top'
    })

    tab.value = 'login'
    registerForm.value = {
      username: '',
      email: '',
      password: ''
    }
  } catch (error) {
    console.error('Register error:', error)
    $q.notify({
      type: 'negative',
      message: error.response?.data?.error || 'Ошибка регистрации',
      position: 'top'
    })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-layout {
  background: linear-gradient(135deg, #667eea, #764ba2);
  min-height: 100vh;
}

.login-container {
  width: 100%;
  max-width: 450px;
  margin: 20px;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.gradient-text {
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
</style>
