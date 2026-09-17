import { defineConfig } from 'orval'

export default defineConfig({
  ticketwallet: {
    input: '../contracts/openapi.json',
    output: {
      target: './src/api/generated/client.ts',
      schemas: './src/api/generated/model',
      client: 'react-query',
      mode: 'tags-split',
      override: {
        mutator: { path: './src/api/client.ts', name: 'customClient' },
        query: { useQuery: true, useMutation: true },
      },
    },
  },
})
