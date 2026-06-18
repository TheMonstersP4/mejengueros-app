module.exports = {
  moduleFileExtensions: ['js', 'json', 'ts'],
  rootDir: '.',
  testMatch: [
    '<rootDir>/test/unit/**/*.spec.ts',
    '<rootDir>/test/integration/prisma-relational-schema.contract.spec.ts'
  ],
  transform: {
    '^.+\\.(t|j)s$': ['ts-jest', { tsconfig: 'tsconfig.spec.json' }]
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1'
  },
  setupFiles: ['<rootDir>/test/jest.setup.ts'],
  collectCoverageFrom: [
    'src/**/*.(t|j)s',
    '!src/**/index.ts',
    '!src/generated/**'
  ],
  coverageThreshold: {
    global: {
      statements: 90,
      branches: 85,
      functions: 90,
      lines: 90
    }
  },
  coverageDirectory: 'coverage',
  coveragePathIgnorePatterns: [
    '/node_modules/',
    '/dist/',
    '/src/generated/'
  ],
  coverageReporters: ['text', 'text-summary', 'lcov'],
  testEnvironment: 'node',
  verbose: true
};
