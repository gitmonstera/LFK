const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'download', component: () => import('pages/DownloadPage.vue') }
    ]
  },
  {
    path: '/login',
    component: () => import('pages/LoginPage.vue'),
    meta: { public: true }  // для публичного доступа
  },
  {
    path: '/profile',
    component: () => import('layouts/ProfileLayout.vue'),
    meta: { requiresAuth: true },  // требует авторизации
    children: [
      { path: '', component: () => import('pages/ProfilePage.vue') }
    ]
  }
]

export default routes
