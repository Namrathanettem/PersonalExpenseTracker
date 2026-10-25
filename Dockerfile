WORKDIR /app
COPY . .
RUN g++ main.cpp -o expense && javac server/ExpenseServer.java
EXPOSE 10000
CMD ["java", "-cp", "server", "ExpenseServer"]
