package bootstrap

import kotlinx.serialization.json.*

fun configureFirebase(o: Options, files: MutableMap<String, ByteArray>) {
    val f = o.firebase ?: return
    fun put(path: String, content: String) { files[path] = content.toByteArray() }
    fun edit(path: String, transform: (String) -> String) { put(path, transform(files.getValue(path).toString(Charsets.UTF_8))) }
    if ("android" in o.targets) {
        val root = "androidApp/src/${o.pkg.replace('.', '/')}"
        put("$root/FirebaseServices.kt", androidFirebase(o))
        if ("messaging" in f.services) put("$root/AppMessagingService.kt", substitute(Resources.text("firebase/AppMessagingService.template"), o.tokens))
        f.android?.let { config ->
            val info = config.getValue("project_info").jsonObject
            val client = config.getValue("client").jsonArray.map { it.jsonObject }.single {
                it.getValue("client_info").jsonObject.getValue("android_client_info").jsonObject.text("package_name") == o.appId
            }
            val strings = linkedMapOf("google_app_id" to client.getValue("client_info").jsonObject.text("mobilesdk_app_id"),
                "gcm_defaultSenderId" to info.text("project_number"), "project_id" to info.text("project_id"),
                "google_api_key" to client.getValue("api_key").jsonArray.first { it.jsonObject["current_key"]?.jsonPrimitive?.content?.isNotBlank() == true }.jsonObject.text("current_key"))
            info["storage_bucket"]?.jsonPrimitive?.content?.let { strings["google_storage_bucket"] = it }
            put("androidApp/res/values/firebase.xml", "<resources>\n" + strings.entries.joinToString("\n") { (key, v) -> "    <string name=\"$key\" translatable=\"false\">${xml(v)}</string>" } + "\n</resources>\n")
        }
        edit("androidApp/src/AndroidManifest.xml") { manifest ->
            var result = manifest
            if ("android.permission.INTERNET" !in result) result = result.replace("    <application", "    <uses-permission android:name=\"android.permission.INTERNET\" />\n    <application")
            if ("messaging" in f.services) {
                result = result.replace("    <application", "    <uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />\n    <application")
                    .replace("    </application>", """
        <service android:name=".AppMessagingService" android:exported="false">
            <intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT" /></intent-filter>
        </service>
        <meta-data android:name="firebase_messaging_installation_id_enabled" android:value="true" />
        <meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" android:value="messages" />
    </application>""")
            }
            result
        }
        edit("$root/MainActivity.kt") { activity ->
            var result = activity.replace("        enableEdgeToEdge()", "        FirebaseServices.initialize(applicationContext)\n        enableEdgeToEdge()")
            if ("messaging" in f.services) result = result.replace("        setContent { App() }", "        setContent { FirebaseNotificationHost { App() } }")
            result
        }
        if ("messaging" in f.services) put("$root/FirebaseNotificationHost.kt", substitute(Resources.text("firebase/FirebaseNotificationHost.template"), o.tokens))
    }
    if ("ios" in o.targets) {
        put("iosApp/src/FirebaseServices.swift", iosFirebase(o))
        f.ios?.let { config ->
            put("iosApp/src/GoogleService-Info.plist", """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
""".trimStart() + config.entries.joinToString("\n") { (key, v) ->
                "<key>${xml(key)}</key>" + if ((v as JsonPrimitive).isString) "<string>${xml(v.content)}</string>" else "<${v.boolean}/>"
            } + "\n</dict></plist>\n")
        }
        edit("iosApp/src/iOSApp.swift") { app ->
            var result = app.replace("struct iOSApp: App {", "struct iOSApp: App {\n    @UIApplicationDelegateAdaptor(FirebaseAppDelegate.self) private var appDelegate")
            if ("messaging" in f.services) result = result.replace("            ContentView()", """
            VStack {
                Button("Enable notifications") { FirebaseServices.enableNotifications() }
                    .disabled(!FirebaseServices.configured)
                if !FirebaseServices.configured { Text("Firebase configuration missing") }
                ContentView()
            }""")
            result
        }
        edit("iosApp/module.xcodeproj/project.pbxproj") { firebaseXcode(it, f) }
        edit("iosApp/src/Info.plist") { it.replace("</dict>", "<key>FirebaseAppDelegateProxyEnabled</key><false/>\n</dict>") }
        if ("messaging" in f.services) {
            edit("iosApp/src/Info.plist") { it.replace("</dict>", "<key>FirebaseMessagingInstallationIdEnabled</key><true/>\n</dict>") }
            put("iosApp/PushNotifications.entitlements", """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict><key>aps-environment</key><string>${'$'}(APS_ENVIRONMENT)</string></dict></plist>
""".trimStart())
        }
    }
    put("FIREBASE.md", """
# Firebase

Selected services: ${f.services.joinToString()}. Project: ${f.projectId ?: "not selected"}.
Configuration: ${o.targets.filter { it in listOf("android", "ios") }.joinToString { "$it=${if (if (it == "android") f.android != null else f.ios != null) "CONFIGURED" else "CONFIG_MISSING"}" }}.
Live verification: NOT_RUN. SDK configuration does not verify console setup or delivery.

Android uses Firebase native SDKs in androidApp and generated XML resources read by FirebaseInitProvider.
iOS uses native Swift APIs in iosApp and exact Firebase ${componentPins.text("firebase_apple")} Swift Package Manager products.
Firebase APIs stay out of shared code and desktop/Wasm hosts. Android dependencies use explicit versions aligned to BoM ${componentPins.text("firebase_bom_reference")}.

Use FirebaseServices in each mobile host. Analytics exposes logEvent; Remote Config exposes fetchAndActivate with an error callback.
Messaging includes an Enable notifications action, runtime permission, Android foreground notifications and FID registration callbacks.
Connect onRegistrationChanged (and latestInstallationId for cold-start/recreation) to your authenticated backend; installation IDs are never logged by this sample.
iOS includes UIApplicationDelegateAdaptor, disabled Firebase delegate swizzling, manual notification Analytics forwarding, APNs token mapping and foreground presentation. Enable Push Notifications for your Apple team and upload an APNs key in Firebase Console. Signed Debug uses development APNs; Release uses production.
Background notification messages are handled by the SDK/OS. Data-only delivery, background modes and notification-to-route mapping need application-specific handling.

When CONFIG_MISSING, SDK calls are guarded and notification controls are disabled. Before creating a connected app, fetch configs using the skill's Firebase workflow and generate into a fresh destination with android_config/ios_config.
Do not copy service-account keys, APNs private keys or tokens into this project or agent prompts. Public SDK identifiers are recorded in bootstrap.spec.json for offline replay.

Verify Analytics with DebugView after logEvent; Messaging with a test notification on a real device and its current installation ID; Remote Config with a published parameter and fetch callback. Verify both foreground/background and permission denial. Configure Analytics consent before collection when required by your app.
Crashlytics is not implemented: it requires build-ID/mapping and dSYM upload tooling, beyond SDK dependencies. Auth, Firestore, Storage and server notification sending are also outside this recipe.
""".trimStart())
    edit("README.md") { it + "\nFirebase: see FIREBASE.md for configuration status, APIs and device/console checks.\n" }
    edit("AGENTS.md") { it + "\nFirebase: read FIREBASE.md before changing mobile SDK setup. Preserve project/app identity; keep private credentials outside the tree. CONFIGURED and live-verified are separate states.\n" }
}

private fun androidFirebase(o: Options): String {
    val f = o.firebase!!
    return buildString {
        append("package ${o.pkg}\n\nimport android.content.Context\nimport com.google.firebase.FirebaseApp\n\nobject FirebaseServices {\n    const val configured = ${f.android != null}\n")
        if ("analytics" in f.services) append("    private lateinit var analytics: com.google.firebase.analytics.FirebaseAnalytics\n")
        append("    fun initialize(context: Context): Boolean {\n        if (!configured) return false\n        check(FirebaseApp.initializeApp(context) != null) { \"Firebase resources are missing\" }\n")
        if ("analytics" in f.services) append("        analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)\n")
        if ("messaging" in f.services) append("""
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(
                android.app.NotificationChannel("messages", "Messages", android.app.NotificationManager.IMPORTANCE_DEFAULT))
        }
        com.google.firebase.messaging.FirebaseMessaging.getInstance().register().addOnCompleteListener { task ->
            if (!task.isSuccessful) onError?.invoke(task.exception ?: IllegalStateException("FCM registration unavailable"))
        }
""")
        append("        return true\n    }\n")
        if ("analytics" in f.services) append("    fun logEvent(name: String, parameters: android.os.Bundle? = null) {\n        check(configured && ::analytics.isInitialized) { \"Initialize configured Firebase first\" }\n        analytics.logEvent(name, parameters)\n    }\n")
        if ("messaging" in f.services) append("""
    @Volatile var latestInstallationId: String? = null
        private set
    @Volatile var onRegistrationChanged: ((String) -> Unit)? = null
    @Volatile var onError: ((Exception) -> Unit)? = null
    internal fun acceptRegistration(installationId: String) { latestInstallationId = installationId; onRegistrationChanged?.invoke(installationId) }
""")
        if ("remote-config" in f.services) append("""
    fun fetchAndActivate(completion: (Boolean, Exception?) -> Unit) {
        check(configured) { "Firebase configuration missing" }
        com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener { task ->
            completion(task.isSuccessful && task.result == true, task.exception)
        }
    }
    fun string(key: String): String {
        check(configured) { "Firebase configuration missing" }
        return com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getString(key)
    }
""")
        append("}\n")
    }
}

private fun iosFirebase(o: Options): String {
    val f = o.firebase!!; val messaging = "messaging" in f.services
    return buildString {
        append("import UIKit\nimport FirebaseCore\n")
        if (messaging) append("import FirebaseMessaging\nimport UserNotifications\n")
        if ("analytics" in f.services) append("import FirebaseAnalytics\n")
        if ("remote-config" in f.services) append("import FirebaseRemoteConfig\n")
        append("\nenum FirebaseServices {\n    static var configured: Bool { FirebaseApp.app() != nil }\n")
        if (messaging) append("""
    static private(set) var latestInstallationId: String?
    static var onRegistrationChanged: ((String) -> Void)?
    static var onError: ((Error) -> Void)?
    static func acceptRegistration(_ installationId: String) { latestInstallationId = installationId; onRegistrationChanged?(installationId) }
    static func enableNotifications() {
        guard configured else { return }
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            DispatchQueue.main.async {
                if let error { onError?(error) }
                if granted { UIApplication.shared.registerForRemoteNotifications() }
            }
        }
    }
""")
        if ("analytics" in f.services) append("    static func logEvent(_ name: String, parameters: [String: Any]? = nil) {\n        precondition(configured, \"Firebase configuration missing\")\n        Analytics.logEvent(name, parameters: parameters)\n    }\n")
        if ("remote-config" in f.services) append("""
    static func fetchAndActivate(completion: @escaping (RemoteConfigFetchAndActivateStatus, Error?) -> Void) {
        precondition(configured, "Firebase configuration missing")
        RemoteConfig.remoteConfig().fetchAndActivate(completionHandler: completion)
    }
    static func string(_ key: String) -> String {
        precondition(configured, "Firebase configuration missing")
        return RemoteConfig.remoteConfig().configValue(forKey: key).stringValue
    }
""")
        append("}\n\nclass FirebaseAppDelegate: NSObject, UIApplicationDelegate${if (messaging) ", MessagingDelegate, UNUserNotificationCenterDelegate" else ""} {\n")
        append("""
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return true }
        FirebaseApp.configure()
""")
        if (messaging) append("        Messaging.messaging().delegate = self\n        UNUserNotificationCenter.current().delegate = self\n")
        append("        return true\n    }\n")
        if (messaging) append("""
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        FirebaseServices.onError?(error)
    }
    func messaging(_ messaging: Messaging, didReceiveRegistration installationId: String?) {
        if let installationId { FirebaseServices.acceptRegistration(installationId) }
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        if FirebaseServices.configured { Messaging.messaging().appDidReceiveMessage(notification.request.content.userInfo) }
        completionHandler([.banner, .badge, .sound])
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        if FirebaseServices.configured { Messaging.messaging().appDidReceiveMessage(response.notification.request.content.userInfo) }
        completionHandler()
    }
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable: Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if FirebaseServices.configured { Messaging.messaging().appDidReceiveMessage(userInfo) }
        completionHandler(.noData)
    }
""")
        append("}\n")
    }
}

private fun firebaseXcode(project: String, f: FirebaseOptions): String {
    val products = listOf("FirebaseCore") + f.services.map { when (it) { "analytics" -> "FirebaseAnalytics"; "messaging" -> "FirebaseMessaging"; else -> "FirebaseRemoteConfig" } }
    fun id(name: String) = sha256(name.toByteArray()).take(24).uppercase()
    val packageId = id("firebase-package")
    val sections = buildString {
        append("/* Begin XCRemoteSwiftPackageReference section */\n$packageId = {isa = XCRemoteSwiftPackageReference; repositoryURL = \"https://github.com/firebase/firebase-ios-sdk.git\"; requirement = {kind = exactVersion; version = ${componentPins.text("firebase_apple")}; }; };\n/* End XCRemoteSwiftPackageReference section */\n")
        append("/* Begin XCSwiftPackageProductDependency section */\n")
        products.forEach { append("${id(it)} = {isa = XCSwiftPackageProductDependency; package = $packageId; productName = $it; };\n") }
        append("/* End XCSwiftPackageProductDependency section */\n/* Begin PBXBuildFile section */\n")
        products.forEach { append("${id("build-$it")} = {isa = PBXBuildFile; productRef = ${id(it)}; };\n") }
        append("/* End PBXBuildFile section */\n")
    }
    var result = project.replace("/* Begin PBXFileReference section */", sections + "\n/* Begin PBXFileReference section */")
        .replace("isa = PBXFrameworksBuildPhase;", "isa = PBXFrameworksBuildPhase;\n            files = (${products.joinToString { id("build-$it") }},);")
        .replace("productType = \"com.apple.product-type.application\";", "productType = \"com.apple.product-type.application\";\n            packageProductDependencies = (${products.joinToString { id(it) }},);")
        .replace("preferredProjectObjectVersion = 77;", "preferredProjectObjectVersion = 77;\n            packageReferences = ($packageId,);")
        .replace("PRODUCT_MODULE_NAME = app;", "PRODUCT_MODULE_NAME = app;\n                OTHER_LDFLAGS = \"$(inherited) -ObjC\";")
    if ("messaging" in f.services) result = result.replace("CODE_SIGN_STYLE = Automatic;", "CODE_SIGN_STYLE = Automatic;\n                CODE_SIGN_ENTITLEMENTS = PushNotifications.entitlements;")
        .replace("CURRENT_PROJECT_VERSION = 1;", "CURRENT_PROJECT_VERSION = 1;\n                APS_ENVIRONMENT = development;")
        .let { p ->
            val release = p.indexOf("32E3F5C4A9879D9D3A791DD8 /* Release */ = {")
            val end = p.indexOf("name = Release;", release)
            p.replaceRange(release, end, p.substring(release, end).replace("APS_ENVIRONMENT = development;", "APS_ENVIRONMENT = production;"))
        }
    return result
}
