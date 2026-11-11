# Stage 1: Build
FROM node:24-alpine AS build
WORKDIR /app

COPY web/package.json web/package-lock.json ./
RUN npm ci

# The i18n locales symlink is copied as-is and replaced below
COPY web/ .

# Replace the i18n locales symlink with the canonical files
RUN rm -f src/lib/i18n/locales
COPY shared/i18n/locales/ ./src/lib/i18n/locales/

RUN npm run build

# Stage 2: Run
FROM node:24-alpine
WORKDIR /app
COPY --from=build /app/build ./build

ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0
# adapter-node compresses responses with brotli/gzip by default
ENV COMPRESS=true

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://127.0.0.1:3000/ >/dev/null || exit 1

USER node

CMD ["node", "build/index.js"]
