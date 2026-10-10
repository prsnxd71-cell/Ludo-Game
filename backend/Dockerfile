# Use official Eclipse Temurin OpenJDK 17 LTS
FROM eclipse-temurin:17-jdk-jammy

# Set application working directory
WORKDIR /app

# Copy libraries, Java source files, and frontend web assets
COPY lib/ ./lib/
COPY src/ ./src/
COPY frontend/ ./frontend/

# Compile Java source code using plain javac and MySQL Connector/J
RUN mkdir -p bin && javac -cp "lib/*:src" -d bin src/*.java

# Render dynamically assigns the PORT environment variable at runtime
ENV PORT=8080
EXPOSE 8080

# Launch the Ludo GameServer
CMD ["java", "-cp", "bin:lib/*", "GameServer"]
