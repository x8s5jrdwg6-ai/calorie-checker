FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY .mvn .mvn
COPY pom.xml pom.xml

RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

RUN cp target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
