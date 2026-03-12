import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'

export default function (/* { store, ssrContext } */) {
  const Router = createRouter({
    scrollBehavior: (to, from, savedPosition) => {
      if (savedPosition) {
        return savedPosition
      } else {
        return { left: 0, top: 0 }
      }
    },
    routes,
    history: createWebHistory()
  })

  return Router
}
