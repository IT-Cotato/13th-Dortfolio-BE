# 1단계: 빌드 (Gradle 캐시를 활용하려고 의존성 먼저 받아둔다)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# bootJar 결과물만 남긴다 (build 태스크가 함께 만드는 -plain.jar와 섞이지 않도록)
RUN find build/libs -name '*-plain.jar' -delete \
    && mv "$(find build/libs -name '*.jar' | head -1)" /workspace/app.jar

# 2단계: 실행 (JDK가 아닌 JRE만 담아 이미지 크기를 줄인다)
FROM eclipse-temurin:17-jre
WORKDIR /app

# 헬스체크에 필요한 curl은 베이스 이미지에 없을 수 있어 명시적으로 설치한다
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# root로 띄우지 않는다
RUN useradd -r -u 1001 dortfolio

COPY --from=builder --chown=dortfolio:dortfolio /workspace/app.jar app.jar

USER dortfolio

EXPOSE 8080

# 컨테이너 메모리 한도에 맞춰 힙이 잡히도록 한다
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
