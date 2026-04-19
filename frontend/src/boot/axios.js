import axios from 'axios'

// Создаем экземпляр axios с базовым URL
const api = axios.create({
  baseURL: '',  // Убираем /api отсюда
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Перехватчик для добавления токена
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    console.log('API Request:', config.method.toUpperCase(), config.url)
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Перехватчик для обработки ошибок
api.interceptors.response.use(
  response => {
    console.log('API Response:', response.status, response.config.url)
    return response
  },
  error => {
    console.error('API Error:', error.response?.status, error.config?.url, error.message)

    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      sessionStorage.removeItem('token')
      localStorage.removeItem('user')

      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export { api }
