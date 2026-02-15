# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY web/package.json web/package-lock.json ./
RUN npm ci

# Copy web source code (symlinks copied as-is, resolved below)
COPY web/ .

# Replace i18n symlink with actual canonical files from top-level shared/
RUN rm -f shared/i18n
COPY shared/i18n/ ./shared/i18n/

# Copy shared assets into static dir
RUN node scripts/copy-shared-assets.js

# Build
RUN npm run build

# Stage 2: Serve
FROM caddy:alpine
COPY web/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/build /srv/svelte
EXPOSE 80
