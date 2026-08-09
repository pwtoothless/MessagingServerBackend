# Stage 1: Compile the Java files
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .

# Compile using any .jar files found in a "lib" folder
RUN find . -name "*.java" > sources.txt && \
    javac -cp ".:lib/*" @sources.txt

# Stage 2: Run the compiled code
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the compiled files and the lib folder
COPY --from=builder /app .

EXPOSE 8081 8082

# Run the app, telling Java where to find the libraries
# NOTE: Replace 'Main' with your actual class name or package.Main
ENTRYPOINT ["java", "-cp", ".:src/:lib/*", "Main"]