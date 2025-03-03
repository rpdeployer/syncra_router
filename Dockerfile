FROM gradle:jdk17-corretto-al2023

COPY . /app

WORKDIR /app

RUN ./gradlew clean bootJar

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]