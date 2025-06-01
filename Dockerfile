# Use OpenJDK base image with Java 21
FROM openjdk:21-slim

# Set environment variables
ENV ANDROID_SDK_ROOT=/usr/local/android-sdk
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"
ENV GRADLE_USER_HOME=/root/.gradle

# Install required packages
RUN set -eux && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    wget \
    unzip \
    curl \
    git \
    dos2unix \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android SDK
RUN set -eux && \
    mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm cmdline-tools.zip

# Accept licenses and install required Android SDK packages
RUN set -eux && \
    yes | sdkmanager --licenses > /dev/null && \
    sdkmanager --verbose \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "extras;android;m2repository" \
    "extras;google;m2repository"

# Set working directory
WORKDIR /app

# Create a script to fix line endings
RUN echo '#!/bin/bash\ndos2unix "$1" && chmod +x "$1"' > /usr/local/bin/fix-script && \
    chmod +x /usr/local/bin/fix-script

# Copy gradle wrapper files first
COPY gradlew ./
COPY gradle gradle/
RUN /usr/local/bin/fix-script gradlew

# Copy build files
COPY build.gradle.kts settings.gradle.kts ./
COPY app/build.gradle.kts app/

# Copy the rest of the project
COPY . .

# Fix all script files
RUN find . -type f -name "*.sh" -exec /usr/local/bin/fix-script {} \; && \
    find . -type f -name "gradlew*" -exec /usr/local/bin/fix-script {} \;

# Test gradle wrapper
RUN ./gradlew --version

ENTRYPOINT ["./gradlew"]
CMD ["tasks"] 