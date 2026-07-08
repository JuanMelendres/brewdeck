import '@testing-library/jest-dom/vitest';

// recharts' ResponsiveContainer relies on ResizeObserver, which jsdom lacks.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

globalThis.ResizeObserver =
  globalThis.ResizeObserver ?? (ResizeObserverStub as unknown as typeof ResizeObserver);

// Node.js 22+ ships an experimental `localStorage` that is unavailable without
// --localstorage-file, which leaves `window.localStorage` as `undefined` in the
// jsdom test environment. Provide a reliable in-memory implementation so token
// store tests (and any future storage tests) work regardless of the Node version.
if (typeof window !== 'undefined' && !window.localStorage) {
  const store = new Map<string, string>();
  const localStorageMock: Storage = {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, value);
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
    clear: () => {
      store.clear();
    },
    key: (index: number) => [...store.keys()][index] ?? null,
    get length() {
      return store.size;
    },
  };
  Object.defineProperty(window, 'localStorage', {
    value: localStorageMock,
    writable: false,
    configurable: true,
  });
}
