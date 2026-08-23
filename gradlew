#!/bin/sh
# Simplified gradlew bootstrap - full script in CI will use system gradle if needed
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || {
  echo "Wrapper jar missing - using system gradle if available"
  gradle "$@"
}
