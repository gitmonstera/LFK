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
    meta: { public: true }
  },
  {
    path: '/profile',
    component: () => import('layouts/ProfileLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', component: () => import('pages/ProfilePage.vue') },
      { path: 'exercises', component: () => import('pages/ExercisesPage.vue') },
      { path: 'exercise/:id', component: () => import('pages/ExercisePage.vue') },
    ]
  }
]

export default routes
