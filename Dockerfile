# Stage 1: Compile the Java files
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .

# Finds and compiles all .java files, including those in subdirectories
RUN find . -name "*.java" > sources.txt && javac @sources.txt

# Stage 2: Run the compiled code
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the compiled .class files from the builder stage
COPY --from=builder /app .

EXPOSE 8080

# Replace 'Main' with the exact name of your main class
ENTRYPOINT ["java", "Main"]
