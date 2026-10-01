#!/usr/bin/env bash
# ==============================================================================
# Java Deobfuscator Launcher (Linux / macOS)
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/deobfuscator-1.0.0.jar"

# Verify Java is available
if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] 'java' command not found. Please install Java 8 or newer (JDK 17 or 21 recommended)."
    echo "  Ubuntu/Debian: sudo apt update && sudo apt install -y openjdk-17-jdk"
  echo "  Fedora/RHEL:   sudo dnf install -y java-17-openjdk-devel"
  echo "  macOS (Homebrew): brew install openjdk@17"
    exit 1
fi

# Build jar if it doesn't exist
if [ ! -f "$JAR_FILE" ]; then
    echo "[INFO] Shaded JAR not found. Building project with Maven..."
    cd "$SCRIPT_DIR"
    mvn clean package -DskipTests
fi

# Recommended JVM memory arguments:
# -Xss128m: Crucial to avoid StackOverflowError during heavy AST/CFG traversal
# -Xmx2G: Sufficient heap for large bytecode analysis
JVM_OPTS="-Xss128m -Xmx2G"

echo "[INFO] Starting Java Deobfuscator..."
exec java $JVM_OPTS -jar "$JAR_FILE" "$@"
