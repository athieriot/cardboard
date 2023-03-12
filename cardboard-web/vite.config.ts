import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: 'localhost',
    proxy: {
      '/command': 'http://0.0.0.0:8080',
      '/connect': {
        target: 'ws://0.0.0.0:8080',
        ws: true,
      }
    }
  }
})
