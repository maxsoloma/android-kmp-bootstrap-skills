# iosApp templating

`iosApp` is an Xcode project, NOT a Gradle module. It ships as opaque template
files; ONLY these bytes are substituted (plain string.replace):

| File | Substituted tokens |
|------|--------------------|
| `iosApp/iosApp.xcodeproj/project.pbxproj.template` | `{{PROJECT_NAME}}` (product/scheme name), `{{APP_ID}}` (PRODUCT_BUNDLE_IDENTIFIER), `{{IOS_DEPLOYMENT_TARGET}}` (IPHONEOS_DEPLOYMENT_TARGET) |
| `iosApp/iosApp/Info.plist.template` | No substitution needed — uses Xcode build setting variables (`$(PRODUCT_BUNDLE_IDENTIFIER)`, `$(PRODUCT_NAME)`) |
| `iosApp/iosApp/iOSApp.swift.template` | No content substitution — imports `Shared` framework verbatim |
| `iosApp/iosApp/ContentView.swift.template` | No content substitution — imports `Shared` framework verbatim |

## Provenance
The `project.pbxproj` was generated once via the KMP IDE wizard against the
golden project, then captured verbatim and frozen as a template. It is NOT
hand-transcribed. The only edits at generation time are the token
substitutions above. The Run Script build phase calls:
`cd "$SRCROOT/.." && ./gradlew :feature:{{FEATURE_NAME}}:embedAndSignAppleFrameworkForXcode`
and FRAMEWORK_SEARCH_PATHS includes
`$(SRCROOT)/../feature/{{FEATURE_NAME}}/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
The shared framework baseName is `Shared`; Swift imports it as `import Shared`.

## Non-negotiable plist key
`Info.plist.template` MUST contain:
`<key>CADisableMinimumFrameDurationOnPhone</key><true/>`
Omitting it can crash Compose-in-SwiftUI on device.

## Swift-name coupling
- `{{FEATURE_NAME}}ViewController.kt` (in feature:{{FEATURE_NAME}} iosMain) → `{{FEATURE_NAME}}ViewControllerKt.{{FEATURE_NAME}}ViewController()`
  When `{{FEATURE_NAME}}` is substituted, file becomes `<FeatureName>ViewController.kt` and
  Swift calls `<FeatureName>ViewControllerKt.<FeatureName>ViewController()`.
- `KoinIos.kt` holding `fun initKoinIos()` (in feature:{{FEATURE_NAME}} iosMain) → `KoinIosKt.doInitKoinIos()`

## Signing
Development team left EMPTY (out of scope). User fills it in Xcode.
