module.exports = function (ctx) {
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
        node: 'node18'
      },
      vueRouterMode: 'history',
      publicPath: '/',
      distDir: 'dist',
      env: {
        API_URL: ctx.dev ? 'http://localhost:9000' : 'http://80.93.63.206',
        WS_URL: ctx.dev ? 'ws://localhost:9000' : 'ws://80.93.63.206',
        NODE_ENV: ctx.dev ? 'development' : 'production'
      }
    },
    devServer: {
      open: true,
      port: 8080,
      proxy: {
        '/api': {
          target: 'http://localhost:9000',
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
