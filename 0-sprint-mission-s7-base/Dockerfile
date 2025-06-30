# 1. Amazon Corretto 17이미지를 베이스로 사용
FROM amazoncorretto:17

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 프로젝트 파일 복사
COPY . .

# 4. Gradle Wrapper를 사용하여 애플리케이션 빌드
RUN ./gradlew bootJar

# 5. 80 포트 노출
EXPOSE 80

# 6. 환경 변수 설정
ENV PROJECT_NAME=discodeit \
    PROJECT_VERSION=1.2-M8 \
    JVM_OPTS=""

# 7. 애플레케이션 실행 명령어 설정
ENTRYPOINT ["sh","-c","java $JVM_OPTS -jar build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar"]