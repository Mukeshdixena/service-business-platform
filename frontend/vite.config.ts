import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    css: true,
    // The default `forks` pool intermittently hangs on worker spawn in this
    // dev environment (unrelated to any test code); `threads` is the
    // documented fallback and equally well-supported for jsdom + RTL.
    pool: 'threads',
  },
})
