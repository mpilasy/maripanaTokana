# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY web/package.json web/package-lock.json ./
RUN npm ci

# Copy web source code (symlinks copied as-is, resolved below)
COPY web/ .

# Replace i18n locales symlink with actual canonical files
RUN rm -f src/lib/i18n/locales
COPY shared/i18n/locales/ ./src/lib/i18n/locales/

# Build
RUN npm run build

# Stage 2: Serve
FROM caddy:alpine
COPY web/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/build /srv/svelte
EXPOSE 80
