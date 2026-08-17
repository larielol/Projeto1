import { configDefaults, defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    exclude: [...configDefaults.exclude, 'e2e/**'],
    css: false,
    coverage: {
      provider: 'v8',
      all: true,
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/test/**', 'src/types/**', 'src/main.tsx', 'src/**/*.d.ts'],
      reporter: ['text', 'html'],
      thresholds: {
        lines: 95,
        statements: 95,
        functions: 85,
        branches: 80,
      },
    },
  },
})
