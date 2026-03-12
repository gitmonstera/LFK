export default function (ctx) {
  return {
    boot: [],
    css: ['app.scss'],
    extras: [
      'roboto-font',
      'material-icons',
      'fontawesome-v6',
    ],
    build: {
      target: {
        browser: ['es2019', 'edge88', 'firefox78', 'chrome87', 'safari13.1'],
        node: 'node20'
      },
      vueRouterMode: 'history',
      publicPath: '/',
      distDir: 'dist',
      env: {
        API_URL: ctx.dev ? 'http://localhost:9000' : '/api'  // Go на 9000
      }
    },
    devServer: {
      open: true,
      port: 8080,  // Quasar на 8080
      proxy: {
        '/api': {
          target: 'http://localhost:9000',  // Прокси на Go (9000)
          changeOrigin: true,
        }
      }
    },
    framework: {
      config: {
        notify: {
          position: 'top',
          timeout: 2500
        },
        loading: {
          delay: 400
        }
      },
      plugins: [
        'Notify',
        'Loading',
        'Dialog'
      ],
    },
    animations: 'all',
  }
}
