FROM public.ecr.aws/docker/library/node:22-slim AS runtime

COPY --from=public.ecr.aws/awsguru/aws-lambda-adapter:0.9.1 /lambda-adapter /opt/extensions/lambda-adapter

WORKDIR /var/task

ENV PORT=3000
ENV AWS_LWA_PORT=3000

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates openssl \
  && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
COPY prisma ./prisma
RUN npm ci --include=dev

COPY . .
RUN npm run build
RUN npm prune --omit=dev

ENV NODE_ENV=production

CMD ["node", "dist/main.js"]
