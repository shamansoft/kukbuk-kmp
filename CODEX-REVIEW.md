## Findings
- **Major** (`AGENTS.md:1`): The document header says `# CLAUDE.md`, which mislabels the file and can confuse future readers or automation looking for agent-specific guidance.
- **Major** (`AGENTS.md:37`): Lists `./gradlew composeApp:allTests`, but no such Gradle task exists in the project—`rg` finds no definition and the Android Gradle plugin does not create it by default. Following this command will fail.
- **Major** (`AGENTS.md:41`): Mentions `./gradlew composeApp:lintFix`; this task is not defined anywhere in the build and the Android plugin does not register it, so the command will fail.
- **Minor** (`AGENTS.md:64`): Claims secure token storage uses “DataStore/Keychain”, but the iOS implementation currently uses `NSUserDefaults` (`composeApp/src/iosMain/kotlin/net/shamansoft/kukbuk/auth/IOSSecureStorage.kt`). The mismatch can mislead maintainers about current security guarantees.

