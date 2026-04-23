# 使用 Amazon Corretto 25
FROM amazoncorretto:25-alpine AS builder

WORKDIR /app

# 安装 Maven
RUN apk add --no-cache maven

COPY ./m2-repo /root/.m2/repository
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -B

FROM amazoncorretto:25-alpine

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]