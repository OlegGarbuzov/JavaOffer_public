FROM openjdk:22

WORKDIR /app

COPY target/javaOffer.jar app.jar

EXPOSE 8090
EXPOSE 5005

ENTRYPOINT ["java", "-jar", "app.jar"]