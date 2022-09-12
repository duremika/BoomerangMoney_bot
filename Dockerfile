FROM bellsoft/liberica-openjdk-alpine-musl:11.0.3
WORKDIR /usr/local/app
ARG JAR_FILE=/target/*.jar
COPY ${JAR_FILE} app.jar
CMD java -jar ./app.jar