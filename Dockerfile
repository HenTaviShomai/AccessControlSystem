# 更精简版本 - 无需 curl
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY ./acsystem-0.0.1.jar  app.jar

RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

USER appuser

EXPOSE 8080

# 使用 TCP 连接检查（不需要 curl）
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
    CMD nc -z localhost 8080 || exit 1

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseG1GC", "-jar", "app.jar"]