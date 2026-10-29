# Firebase mobile setup

Read when Firebase is selected. Native SDKs live in Android/iOS hosts; shared
logic, desktop and Wasm receive no Firebase dependencies. Implemented services:
`analytics`, `messaging`, `remote-config`, independently selectable. Crashlytics
needs build ID/mapping and dSYM upload integration and is rejected by this recipe.
Auth/Firestore/Storage/server sending are not implemented.

## Connect before generating

Prefer connected Firebase tools when available and sufficient. Otherwise use the
official Firebase CLI; it is optional for connection, not for installing/running
the bootstrap skill. Its standalone binary avoids an npm requirement. Check the
installed CLI's `--help` for exact options before use; current reviewed CLI is
15.34.1. Discover capabilities on the current agent, not another worker's tools.

1. Reuse a supplied project ID or list accessible projects and let the user select
   Existing / Create new / Prepare SDKs only. Project creation/Analytics linking
   require that explicit choice and the intended IDs; do not guess an account,
   billing plan or organization. Selecting services alone does not authorize
   creating cloud projects. Once the user has chosen, complete that scope without
   asking repeatedly. Use the current authenticated session; never request or
   echo service-account JSON, OAuth tokens or APNs private keys in chat.
2. List registered apps in that exact project. Reuse an Android app with matching
   package/application ID, and an iOS app with matching bundle ID. Create only
   missing apps. On an uncertain write result, list/read back before retrying to
   avoid duplicates. Do not delete/recreate projects/apps or change billing/IAM.
3. Fetch Android `google-services.json` and Apple `GoogleService-Info.plist` to
   temporary files outside the output. Config examples, after verifying CLI help:

   ```sh
   firebase projects:list --json
   firebase apps:list --project PROJECT_ID --json
   # Only after choosing Create new and confirming the intended ID:
   firebase projects:create PROJECT_ID --display-name DISPLAY_NAME --json
   firebase apps:create ANDROID DISPLAY_NAME --package-name APP_ID --project PROJECT_ID --json
   firebase apps:create IOS DISPLAY_NAME --bundle-id APP_ID --project PROJECT_ID --json
   firebase apps:sdkconfig ANDROID FIREBASE_APP_ID --project PROJECT_ID --out /absolute/tmp/google-services.json
   firebase apps:sdkconfig IOS FIREBASE_APP_ID --project PROJECT_ID --out /absolute/tmp/GoogleService-Info.plist
   ```

   These are argument examples, not strings to interpolate into shell commands.
   Use structured tools or correctly quoted argument arrays. Project IDs/app IDs
   are different identifiers; read back the SDK app ID from app registration.
4. Add the selected services, actual `project_id`, `android_config` and/or
   `ios_config` paths to the bootstrap JSON config. Paths resolve relative to the
   config file; use absolute paths when calling plan programmatically. Run plan.
   The generator checks project/application identity, embeds public SDK config
   in the normalized spec, generates Android XML and packages the Apple plist.
   Review that normalized plan, then generate. Replay no longer needs the inputs.
5. Analytics requires Google Analytics enabled/linked for the Firebase project.
   Complete linking only with the chosen Analytics account and its authorization;
   if unavailable, report that console step pending. Messaging on iOS requires
   Apple team signing/Push Notifications and an APNs key uploaded in Firebase
   Console; use secure platform UI for the private key. The recipe emits
   development Debug / production Release entitlements. No APNs secret belongs
   in generated files. A generic SDK config receipt does not finish these steps.

If authentication or configuration is unavailable, offer Prepare SDKs only or
keep the connection pending, following the user's choice. Omit unknown config
fields; never fabricate IDs/keys or silently switch the selected project.

## Config

```json
{
  "name": "Aurora",
  "package": "com.example.aurora",
  "navigation": "navigation3",
  "firebase": {
    "services": ["analytics", "messaging", "remote-config"],
    "project_id": "actual-project-id",
    "android_config": "/absolute/path/google-services.json",
    "ios_config": "/absolute/path/GoogleService-Info.plist"
  }
}
```

Omit the Firebase object for None. For preparation, provide only `services` and
an optional project ID; plan reports CONFIG_MISSING per mobile platform. Embedded
`android` JSON and `ios` dictionary are the normalized replay form. Do not provide
both a platform's path and embedded data. These are public SDK identifiers, not
admin credentials; keep environment-specific configs under appropriate access.

## Verify in layers

- Run generated platform build/tests, including Xcode host compilation. Android
  uses pinned native main artifacts (no retired `-ktx` modules), aligned to BoM
  35.1.0. It generates official resource names instead of a Gradle Google Services
  plugin. iOS links exact Firebase 13.1.1 SPM products; Xcode 26.2+ is required.
- Inspect `FIREBASE.md`, FirebaseServices, Android messaging service, permission
  UI, Apple delegate/APNs mapping and entitlements. Connect FID registration
  callbacks plus latestInstallationId to the app's authenticated backend. The
  starter never logs installation identifiers. Android enables FID registration
  and uses onRegistered/register; Apple enables FirebaseMessagingInstallationIdEnabled
  and uses didReceiveRegistration. These replace the deprecated registration-token APIs.
- Verify permission grant/denial and real-device foreground/background notification
  delivery. Test Analytics logEvent in DebugView and a published Remote Config
  parameter through fetchAndActivate; record errors as well as values.

Report separately: project/app registration, SDK configuration, compilation,
Analytics linking, APNs setup and live receipts. Plan's CONFIGURED means identity
validation only. `live_verification` remains NOT_RUN until those checks happen.

Sources reviewed 2026-10-28: [Android setup](https://firebase.google.com/docs/android/setup),
[resource conversion](https://firebase.google.com/docs/projects/multiprojects),
[Android release notes](https://firebase.google.com/support/release-notes/android),
[Apple release notes](https://firebase.google.com/support/release-notes/ios),
[Android FCM](https://firebase.google.com/docs/cloud-messaging/android/get-started),
[Apple FCM](https://firebase.google.com/docs/cloud-messaging/ios/get-started),
[CLI](https://firebase.google.com/docs/cli).
