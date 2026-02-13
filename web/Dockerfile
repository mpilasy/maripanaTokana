# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app

# Copy all package files
COPY web/svelte/package.json web/svelte/package-lock.json ./svelte/
COPY web/react/package.json web/react/package-lock.json ./react/
COPY web/angular/package.json web/angular/package-lock.json ./angular/

# Install Svelte dependencies
WORKDIR /app/svelte
RUN npm ci

# Install React dependencies
WORKDIR /app/react
RUN npm ci

# Install Angular dependencies
WORKDIR /app/angular
RUN npm ci

# Copy web source code (symlinks copied as-is, resolved below)
WORKDIR /app
COPY web/ .

# Replace i18n symlink with actual canonical files from top-level shared/
RUN rm -f shared/i18n
COPY shared/i18n/ ./shared/i18n/

# Make html2canvas resolvable from shared/ (outside any app's node_modules)
RUN mkdir -p node_modules && ln -s /app/svelte/node_modules/html2canvas node_modules/html2canvas

# Copy shared assets into each framework's static dir
RUN node scripts/copy-shared-assets.js

# Build all three apps
RUN cd svelte && npm run build
RUN cd react && npm run build
RUN cd angular && npm run build

# Stage 2: Serve
FROM caddy:alpine
COPY web/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/svelte/build /srv/svelte
COPY --from=build /app/react/dist /srv/react
COPY --from=build /app/angular/dist/browser /srv/ng
EXPOSE 80
