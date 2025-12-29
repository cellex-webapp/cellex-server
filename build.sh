#!/usr/bin/env bash
# Build script for Render deployment

echo "🚀 Starting build process for Cellex Backend..."

# Exit on error
set -o errexit

# Check Java version
echo "☕ Checking Java version..."
java -version

# Check Maven version
echo "📦 Checking Maven version..."
mvn -version

# Clean and build the project
echo "🔨 Building the project with Maven..."
mvn clean package -DskipTests

# Verify the build
if [ -f target/*.jar ]; then
    echo "✅ Build successful! JAR file created."
    ls -lh target/*.jar
else
    echo "❌ Build failed! JAR file not found."
    exit 1
fi

echo "🎉 Build process completed successfully!"
