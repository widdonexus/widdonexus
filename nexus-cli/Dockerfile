FROM eclipse-temurin:17-jre

RUN addgroup --system nexus && adduser --system --group nexus

WORKDIR /app

COPY target/nexus-cli-*.jar /app/nexus-cli.jar
COPY bin/nexus-cli /usr/local/bin/nexus-cli

RUN chmod +x /usr/local/bin/nexus-cli && \
    chown nexus:nexus /app/nexus-cli.jar

USER nexus

ENTRYPOINT ["java", "-jar", "/app/nexus-cli.jar"]
CMD ["--help"]