FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/mcp-*.jar app.jar
EXPOSE 8282
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -qO- http://localhost:8282/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]