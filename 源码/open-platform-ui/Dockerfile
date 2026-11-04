# open-platform-web (映射目录：open-platform-ui) 前端镜像构建
FROM node:20-alpine AS builder

WORKDIR /app/open-platform-ui
COPY open-platform-ui/package*.json ./
RUN npm ci
COPY open-platform-ui/ ./
COPY shared/ /app/shared/
RUN npm run build

FROM nginx:1.27-alpine

# 时区设置为北京时间
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

COPY open-platform-ui/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/open-platform-ui/dist /usr/share/nginx/html

EXPOSE 80

# 远程构建示例：
# Run from the repository root:
# docker -H tcp://127.0.0.1:2375 build -f open-platform-ui/Dockerfile -t docker-hosted.example.invalid/nstl/open-platform/web-frontend:1.0.0 .
# 远程推送示例：
# docker -H tcp://127.0.0.1:2375 push docker-hosted.example.invalid/nstl/open-platform/web-frontend:1.0.0
