import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  build: {
    // MUI DataGrid is route-split into its own feature chunk.
    chunkSizeWarningLimit: 600,
  },
  plugins: [react()],
})
