#!/usr/bin/env sh
cd "$(dirname "$0")/aqua-client" && chmod +x gradlew && ./gradlew build --no-daemon
