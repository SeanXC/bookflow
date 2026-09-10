import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  build: {
    // MUI DataGrid is route-split into its own feature chunk.
    chunkSizeWarningLimit: 600,
  },
  plugins: [react()],
  test: {
    clearMocks: true,
    coverage: {
      exclude: [
        'src/test/**',
        'src/main.jsx',
        'src/app/lazyPages.jsx',
      ],
      include: ['src/**/*.{js,jsx}'],
      provider: 'v8',
      reporter: ['text', 'lcov'],
      thresholds: {
        lines: 50,
        statements: 50,
      },
    },
    environment: 'jsdom',
    pool: 'vmThreads',
    restoreMocks: true,
    setupFiles: './src/test/setupTests.js',
    testTimeout: 15_000,
  },
})
