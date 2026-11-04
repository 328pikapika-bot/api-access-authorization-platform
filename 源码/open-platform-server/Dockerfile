# open-platform-service (映射目录：open-platform-server) 后端镜像构建
FROM eclipse-temurin:11-jre

# 时区设置为北京时间
ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Shanghai"


WORKDIR /app
COPY target/*.jar /app/app.jar

# 运行环境变量：test / prod（默认 test）
ENV APP_ENV=test
ENV JAVA_OPTS=""

EXPOSE 17300
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$APP_ENV -jar /app/app.jar"]

# 远程构建示例：
# docker -H tcp://127.0.0.1:2375 build -t docker-hosted.example.invalid/nstl/open-platform/service-backend:1.0.0 .
# 远程推送示例：
# docker -H tcp://127.0.0.1:2375 push docker-hosted.example.invalid/nstl/open-platform/service-backend:1.0.0
