FROM eclipse-temurin:25-jre-noble

RUN apt-get update && apt-get install -y \
    curl \
    gzip \
    libfontconfig1 \
    libicu-dev \
    libssl3 \
    ca-certificates \
    libgraphite2-3 \
    libharfbuzz0b \
    && rm -rf /var/lib/apt/lists/*

RUN curl -L --fail https://github.com/tectonic-typesetting/tectonic/releases/download/tectonic%400.15.0/tectonic-0.15.0-x86_64-unknown-linux-gnu.tar.gz \
    | tar -xz -C /usr/local/bin/

COPY target/rewrite-backend.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]