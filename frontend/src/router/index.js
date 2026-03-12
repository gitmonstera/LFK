import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'

const router = createRouter({
  scrollBehavior: () => ({ left: 0, top: 0 }),
  routes,
  history: createWebHistory()
})

// Защита маршрутов
router.beforeEach((to, from, next) => {
  const isAuthenticated = !!(localStorage.getItem('token') || sessionStorage.getItem('token'))

  console.log('Navigating to:', to.path, 'Auth:', isAuthenticated)

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && isAuthenticated) {
    next('/profile')
  } else {
    next()
  }
})

export default router
