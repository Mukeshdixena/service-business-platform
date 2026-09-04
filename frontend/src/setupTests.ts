import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// @testing-library/react's automatic afterEach(cleanup) only registers itself
// when it detects vitest's *global* test hooks. This project keeps `globals:
// false` in vite.config.ts (test files import describe/it/expect explicitly),
// so cleanup is wired up here instead — without it, each test's rendered DOM
// piles up in document.body across the whole file.
afterEach(() => {
  cleanup()
})
