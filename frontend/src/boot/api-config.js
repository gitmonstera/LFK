// Конфигурация API для разных окружений

// Функция для получения базового URL API
export const getApiBaseUrl = () => {
  if (process.env.NODE_ENV === 'production') {
    // В production используем IP и порт 8080
    return 'http://80.93.63.206:8080'
  } else {
    // В разработке используем localhost:9000
    return 'http://localhost:9000'
  }
}

// Функция для получения полного URL API (с путем)
export const getApiUrl = (path) => {
  const baseUrl = getApiBaseUrl()
  // Добавляем /api если его нет
  const apiPath = path.startsWith('/api') ? path : `/api${path}`
  return `${baseUrl}${apiPath}`
}

// Функция для получения WebSocket URL
export const getWsUrl = (exerciseId) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')

  if (process.env.NODE_ENV === 'production') {
    // В production используем IP и порт 8080 для WebSocket
    return `ws://80.93.63.206:8080/ws/exercise/${exerciseId}?token=${token}`
  } else {
    // В разработке используем localhost:9000
    return `ws://localhost:9000/ws/exercise/${exerciseId}?token=${token}`
  }
}

// Функция для проверки окружения
export const isProduction = () => {
  return process.env.NODE_ENV === 'production'
}

// Функция для получения текущего IP/домена
export const getServerAddress = () => {
  return isProduction() ? '80.93.63.206:8080' : 'localhost:9000'
}

// Функция для получения заголовков с токеном
export const getAuthHeaders = () => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
}

// Функция для проверки соединения с сервером
export const checkServerConnection = async () => {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/health`, {
      method: 'GET',
      headers: getAuthHeaders()
    })
    return response.ok
  } catch (error) {
    console.error('Server connection check failed:', error)
    return false
  }
}
