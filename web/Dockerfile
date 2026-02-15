# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app

# Copy package files
COPY web/svelte/package.json web/svelte/package-lock.json ./svelte/

# Install Svelte dependencies
WORKDIR /app/svelte
RUN npm ci

# Copy web source code (symlinks copied as-is, resolved below)
WORKDIR /app
COPY web/ .

# Replace i18n symlink with actual canonical files from top-level shared/
RUN rm -f shared/i18n
COPY shared/i18n/ ./shared/i18n/

# Make html2canvas resolvable from shared/ (outside any app's node_modules)
RUN mkdir -p node_modules && ln -s /app/svelte/node_modules/html2canvas node_modules/html2canvas

# Copy shared assets into framework's static dir
RUN node scripts/copy-shared-assets.js

# Build
RUN cd svelte && npm run build

# Stage 2: Serve
FROM caddy:alpine
COPY web/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/svelte/build /srv/svelte
EXPOSE 80
