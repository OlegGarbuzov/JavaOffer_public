FROM openjdk:22
WORKDIR /app

# копируем jar на всякий случай
COPY target/JavaOffer-0.1.jar /app/javaoffer.jar

# Открываем порты
EXPOSE 8090
EXPOSE 5005

# Тут главное: указываем classpath вручную — классы + зависимости
ENTRYPOINT ["java", "-cp", "/app/target/classes/:/app/target/dependency/*", "com.example.javaoffer.JavaOfferApplication"]