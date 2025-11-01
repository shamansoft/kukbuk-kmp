## US-15: Robust Persistent Login & Token Refresh
As a user, I want the app to securely and silently maintain my logged-in state across app restarts so that I do not have to re-authorize my Google account every time I open the app.

Acceptance Criteria:

[ ] After successful initial login, the app does not present the login screen on subsequent app launches.

[ ] On app launch, the authentication state is checked, and a stored valid token is used immediately.

[ ] If the stored access token is expired, the app silently attempts to refresh the token using the stored refresh token.

[ ] If the token refresh is successful, the user is logged in automatically.

[ ] If the token refresh fails (e.g., refresh token is revoked or expired), the app gracefully redirects the user to the "Sign in with Google" screen.

[ ] The secure storage mechanism (Keystore/Keychain) is verified to be working correctly for long-term storage of refresh tokens.

[ ] (Regression Test) Google Drive API calls (US-3) are successful immediately after an automatic login or silent refresh.

Technical Tasks:

[ ] Audit and Refactor token storage and retrieval logic in common code to ensure correct access and refresh token saving.

[ ] Implement robust Token Expiry Check logic on app startup.

[ ] Implement the Silent Token Refresh Mechanism using the Google OAuth client libraries on both Android and iOS.

[ ] Update the Authentication State Management to handle the Refreshing state and transition to Authenticated or Unauthenticated appropriately.

[ ] Verify platform-specific secure storage (Android Keystore / iOS Keychain) is correctly initialized and persisting data across device reboots/app reinstalls (where applicable for testing).

Definition of Done:

User can close and reopen the app (including a full process kill) and remain logged in without any visible authentication prompt.

Token refresh works silently in the background when needed.

The authentication persistence issue is confirmed as resolved on both iOS and Android.

## US-16: Manual Logout Flow
As a user, I want to be able to manually log out of the application so that I can secure my recipes or switch accounts.

Acceptance Criteria:

[ ] A "Logout" option is available in the app's settings screen (or a temporary easily accessible UI).

[ ] Tapping "Logout" revokes the Google session and clears all local authentication tokens (access and refresh).

[ ] After successful logout, the user is immediately redirected to the initial "Sign in with Google" screen.

[ ] All locally cached data (recipes from US-7, if implemented) are cleared upon logout (future proofing).

[ ] Logout works correctly even when the device is offline (clears local state, defers online revocation).

[ ] Clear feedback (e.g., a momentary loading indicator) is provided during the logout process.

Technical Tasks:

[ ] Design and implement the Logout Button UI in the SettingsScreen (or equivalent).

[ ] Implement the platform-specific logic to revoke the Google OAuth token (recommended practice) for both Android and iOS.

[ ] Implement logic to clear all credentials from the secure token storage (Keystore/Keychain).

[ ] Update the Authentication State Management to transition the state to Unauthenticated and trigger the navigation back to the login screen.

[ ] (Future-Proofing) Create a helper function to clear the local database/cache.

Definition of Done:

User can successfully log out on both platforms.

Logout completely clears the user's secure credentials.

After logout, the user is presented with the correct initial login screen.