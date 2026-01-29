FROM eclipse-temurin:17-jdk

WORKDIR /app

# Maven Wrapper と pom を先にコピー（キャッシュが効く）
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY .mvn .mvn
COPY pom.xml pom.xml

# 依存関係を先に取得
RUN ./mvnw -B -q dependency:go-offline

# ソースをコピーしてビルド
COPY src src
RUN ./mvnw -B clean package -DskipTests

# jar を固定名にして起動
RUN cp target/*.jar app.jar

EXPOSE 8080
CMD ["java","-jar","app.jar"]
