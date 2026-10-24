
FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update \
    && apt-get install -y g++ \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY . .

RUN g++ backend/main.cpp -o backend/expense \
    && javac server/ExpenseServer.java

EXPOSE 10000

CMD ["java", "-cp", "server", "ExpenseServer"]

