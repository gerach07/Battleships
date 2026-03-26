# Use Node.js LTS
FROM node:18-alpine AS production

# Set working directory
WORKDIR /app

# Set production environment
ENV NODE_ENV=production

# Copy package files first for better Docker layer caching
COPY package.json ./

# Install production dependencies only (package-lock.json optional)
RUN if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --omit=dev; fi && npm cache clean --force

# Copy server source
COPY server.js ./
COPY src/ ./src/

# Create non-root user for security
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodeuser -u 1001

# Fix file ownership for non-root user
RUN chown -R nodeuser:nodejs /app
USER nodeuser

# Expose port (Render sets PORT env var, default 10000)
EXPOSE 10000

# Start server
CMD ["node", "server.js"]
