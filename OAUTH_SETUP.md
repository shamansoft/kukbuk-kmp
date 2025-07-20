# Google OAuth Configuration Setup

This document provides step-by-step instructions for configuring Google OAuth for the Kukbuk app.

## Prerequisites

- Google Cloud Console account
- Firebase project (optional but recommended)

## 1. Google Cloud Console Setup

### Create a Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Note your project ID

### Enable APIs
1. Go to "APIs & Services" > "Library"
2. Search for and enable:
   - Google Sign-In API
   - Google Drive API

### Create OAuth 2.0 Credentials

#### Web Application (Required for server-side verification)
1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Select "Web application"
4. Add authorized origins:
   - `http://localhost` (for development)
   - Your production domain
5. Save the **Client ID** and **Client Secret**

#### Android Application
1. Create another OAuth client ID
2. Select "Android"
3. Package name: `net.shamansoft.kukbuk`
4. SHA-1 certificate fingerprint:
   - For debug: Get from Android Studio or run `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
   - For release: Use your production keystore
5. Save the **Client ID**

#### iOS Application
1. Create another OAuth client ID
2. Select "iOS"
3. Bundle ID: `net.shamansoft.kukbuk`
4. Save the **Client ID**

## 2. Android Configuration

### Update strings.xml
Replace `YOUR_GOOGLE_WEB_CLIENT_ID` in `composeApp/src/androidMain/res/values/strings.xml`:

```xml
<string name="google_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
```

### Add Google Services
1. Download `google-services.json` from Firebase Console
2. Place it in `composeApp/` directory
3. Add to `composeApp/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins
    id("com.google.gms.google-services") version "4.4.0"
}
```

## 3. iOS Configuration

### Add Dependencies
Add to `iosApp/Podfile`:

```ruby
target 'iosApp' do
  use_frameworks!
  platform :ios, '12.0'
  
  pod 'GoogleSignIn'
end
```

### Configure URL Schemes
Add to `iosApp/iosApp/Info.plist`:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string></string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>YOUR_IOS_CLIENT_ID_REVERSED</string>
        </array>
    </dict>
</array>
```

### Add GoogleService-Info.plist
1. Download from Firebase Console
2. Add to iOS project in Xcode

## 4. Google Drive Permissions

The app requests these Google Drive scopes:
- `https://www.googleapis.com/auth/drive.file` - Access to files created by the app
- `https://www.googleapis.com/auth/drive.appdata` - Access to app-specific data folder

These scopes allow the app to:
- Store recipe data in Google Drive
- Sync recipes across devices
- Access only files created by the app (not all user files)

## 5. Security Considerations

### Token Storage
- **Android**: Uses DataStore with EncryptedSharedPreferences for secure storage
- **iOS**: Uses Keychain for secure token storage

### Network Security
- All API calls use HTTPS
- Tokens are validated server-side
- Refresh tokens are stored securely

## 6. Testing

### Debug Mode
1. Use debug SHA-1 fingerprint for Android testing
2. Ensure localhost is in authorized origins for web client

### Production
1. Update with production SHA-1 fingerprint
2. Add production domain to authorized origins
3. Use production client IDs

## 7. Troubleshooting

### Common Issues
- **Sign-in fails**: Check SHA-1 fingerprint matches
- **Invalid client**: Verify client ID configuration
- **Permission denied**: Ensure proper scopes are requested
- **Token expired**: Implement proper refresh token handling

### Debug Steps
1. Check client ID configuration
2. Verify package/bundle ID matches
3. Confirm API is enabled in Google Cloud Console
4. Check device/emulator Google Play Services