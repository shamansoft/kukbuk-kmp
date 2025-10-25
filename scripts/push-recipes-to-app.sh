#!/bin/bash

# Script to push recipe YAML files to the app's internal storage

RECIPES_DIR="/Users/alexey/dev/save-a-recipe/sar-kmp/no-git/kukbuk"
APP_PACKAGE="net.shamansoft.kukbuk"
TARGET_DIR="/data/data/$APP_PACKAGE/files/recipes"

echo "Creating recipes directory in app storage..."
adb shell "run-as $APP_PACKAGE mkdir -p $TARGET_DIR"

echo ""
echo "Pushing recipe files..."
count=0

cd "$RECIPES_DIR"
for file in *.yaml *.yml; do
    if [ -f "$file" ]; then
        echo "  - $file"
        # Push to temp location (world-accessible)
        adb push "$file" /data/local/tmp/ > /dev/null 2>&1
        # Copy to app's private storage
        adb shell "run-as $APP_PACKAGE cp /data/local/tmp/$file $TARGET_DIR/"
        # Clean up temp file
        adb shell "rm /data/local/tmp/$file"
        count=$((count + 1))
    fi
done

echo ""
echo "Done! Pushed $count recipe files."
echo ""
echo "Verifying..."
adb shell "run-as $APP_PACKAGE ls -l $TARGET_DIR" | head -10

echo ""
echo "Total files in app storage:"
adb shell "run-as $APP_PACKAGE ls $TARGET_DIR | wc -l"
