package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.*
import kotlin.test.*

class FirebaseTest {
    private val temp = Files.createTempDirectory("bootstrap-firebase-")
    private val output = temp.resolve("app")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private val android = value(mapOf(
        "project_info" to mapOf("project_id" to "aurora-demo", "project_number" to "123456789", "storage_bucket" to "aurora-demo.firebasestorage.app"),
        "client" to listOf(mapOf("client_info" to mapOf("mobilesdk_app_id" to "1:123456789:android:abcdef", "android_client_info" to mapOf("package_name" to "org.example.app")), "api_key" to listOf(mapOf("current_key" to "public-config-key"))))
    )).jsonObject
    private val ios = value(mapOf("PROJECT_ID" to "aurora-demo", "BUNDLE_ID" to "org.example.app", "GOOGLE_APP_ID" to "1:123456789:ios:abcdef", "GCM_SENDER_ID" to "123456789", "API_KEY" to "public-config-key", "IS_ANALYTICS_ENABLED" to true)).jsonObject
    private fun spec(firebase: Any?, vararg fields: Pair<String, Any?>) = value(mapOf("name" to "Aurora", "package" to "com.example.app", "app_id" to "org.example.app", "navigation" to "navigation3", "firebase" to firebase) + fields).jsonObject
    private fun config(vararg fields: Pair<String, Any?>) = mapOf("services" to firebaseServices, "project_id" to "aurora-demo") + fields
    private fun GenerationPlan.file(path: String) = rendered.files.getValue(path).toString(Charsets.UTF_8)
    private fun plan(spec: JsonObject): GenerationPlan = planProject(spec, output).also { assertTrue(it.valid, it.issues.toString()) }.plan!!
    @Test fun nativeSdksStayInAppHostsForBothUiRecipesAndDataCombinations() {
        for (ui in listOf("shared-compose", "swiftui")) for (data in listOf(false, true)) {
            val p = plan(spec(config("android" to android, "ios" to ios), "ui" to ui,
                "navigation" to if (ui == "swiftui") "platform-native" else "navigation3", "ios_deployment_target" to "16.0",
                "network" to if (data) "ktor" else "none", "storage" to if (data) "settings" else "none"))
            assertFalse("com.google.firebase" in p.file("shared/module.yaml"))
            assertTrue("firebase-messaging:26.0.1" in p.file("androidApp/module.yaml"))
            assertFalse("firebase-messaging-ktx" in p.file("androidApp/module.yaml"))
            assertTrue("FirebaseServices.initialize" in p.file("androidApp/src/com/example/app/MainActivity.kt"))
            assertTrue("FirebaseNotificationHost { App() }" in p.file("androidApp/src/com/example/app/MainActivity.kt"))
            assertTrue("org.example.app" !in p.file("androidApp/res/values/firebase.xml")) // Identity is matched using app_id, sources use package.
            assertTrue("1:123456789:android:abcdef" in p.file("androidApp/res/values/firebase.xml"))
            assertTrue("UIApplicationDelegateAdaptor" in p.file("iosApp/src/iOSApp.swift"))
            assertTrue("FirebaseAppDelegateProxyEnabled</key><false/>" in p.file("iosApp/src/Info.plist"))
            assertTrue("FirebaseMessagingInstallationIdEnabled</key><true/>" in p.file("iosApp/src/Info.plist"))
            assertTrue("appDidReceiveMessage" in p.file("iosApp/src/FirebaseServices.swift"))
            assertTrue("didReceiveRegistration installationId" in p.file("iosApp/src/FirebaseServices.swift"))
            assertFalse("didReceiveRegistrationToken" in p.file("iosApp/src/FirebaseServices.swift"))
            assertTrue("firebase_messaging_installation_id_enabled" in p.file("androidApp/src/AndroidManifest.xml"))
            assertTrue("onRegistered(installationId" in p.file("androidApp/src/com/example/app/AppMessagingService.kt"))
            assertFalse(".token.addOnCompleteListener" in p.file("androidApp/src/com/example/app/FirebaseServices.kt"))
            assertTrue("FirebaseMessaging" in p.file("iosApp/module.xcodeproj/project.pbxproj"))
            assertTrue("exactVersion" in p.file("iosApp/module.xcodeproj/project.pbxproj"))
            assertEquals(1, Regex("APS_ENVIRONMENT = production;").findAll(p.file("iosApp/module.xcodeproj/project.pbxproj")).count())
            assertEquals(1, Regex("APS_ENVIRONMENT = development;").findAll(p.file("iosApp/module.xcodeproj/project.pbxproj")).count())
            assertTrue("GOOGLE_APP_ID" in p.file("iosApp/src/GoogleService-Info.plist"))
            val replay = planProject(p.spec, temp.resolve("copy"), p.lock)
            assertTrue(replay.valid, replay.issues.toString())
            assertEquals(p.rendered.files.mapValues { sha256(it.value) }, replay.plan!!.rendered.files.mapValues { sha256(it.value) })
        }
    }
    @Test fun missingConfigIsExplicitAndDoesNotCreateInventedIds() {
        val p = plan(spec(mapOf("services" to listOf("messaging")), "targets" to platforms))
        assertTrue("CONFIG_MISSING" in p.document().getValue("stack").toString())
        assertFalse("androidApp/res/values/firebase.xml" in p.rendered.files)
        assertFalse("iosApp/src/GoogleService-Info.plist" in p.rendered.files)
        assertTrue("const val configured = false" in p.file("androidApp/src/com/example/app/FirebaseServices.kt"))
        assertFalse("firebase-analytics:" in p.file("androidApp/module.yaml"))
        assertFalse("Firebase" in p.file("desktopApp/module.yaml"))
        assertFalse(output.exists())
    }
    @Test fun eachServiceCanBeSelectedAloneAndNonMobileTargetsAreNotGivenFakeSdks() {
        for (service in firebaseServices) {
            val p = plan(spec(config("services" to listOf(service)), "targets" to listOf("android"), "kind" to "android"))
            val artifact = mapOf("analytics" to "firebase-analytics", "messaging" to "firebase-messaging", "remote-config" to "firebase-config").getValue(service)
            assertTrue("com.google.firebase:$artifact:" in p.file("androidApp/module.yaml"))
            assertEquals(service == "messaging", p.rendered.files.keys.any { it.endsWith("AppMessagingService.kt") })
            assertEquals(service == "messaging", "POST_NOTIFICATIONS" in p.file("androidApp/src/AndroidManifest.xml"))
            val apple = plan(spec(config("services" to listOf(service)), "targets" to listOf("ios")))
            assertTrue("FirebaseAppDelegateProxyEnabled</key><false/>" in apple.file("iosApp/src/Info.plist"))
            assertEquals(service == "messaging", "FirebaseMessagingInstallationIdEnabled" in apple.file("iosApp/src/Info.plist"))
        }
        val invalid = planProject(spec(config(), "targets" to listOf("desktop", "web")), output)
        assertFalse(invalid.valid); assertTrue(invalid.issues.any { it.field == "firebase.services" })
        val mobile = plan(spec(config(), "targets" to listOf("ios", "desktop", "web")))
        assertFalse("com.google.firebase" in mobile.file("shared/module.yaml"))
    }
    @Test fun malformedUnknownCredentialsAndIdentityMismatchesFailBeforeWrites() {
        val wrongAndroid = JsonObject(android + ("project_info" to value(mapOf("project_id" to "different-project", "project_number" to "123"))))
        val wrongIos = JsonObject(ios + ("BUNDLE_ID" to JsonPrimitive("org.wrong.app")))
        val cases = listOf(null, "firebase", emptyMap<String, String>(), config("services" to emptyList<String>()), config("services" to listOf("crashlytics")),
            config("private_key" to "never-a-client-config"), config("android" to wrongAndroid), config("ios" to wrongIos), config("project_id" to "invalid!"),
            config("android" to android, "android_config" to "/missing"), config("android" to mapOf("type" to "service_account", "private_key" to "x")))
        cases.forEach { choice ->
            val result = planProject(spec(choice), output)
            assertFalse(result.valid, choice.toString()); assertFalse(output.exists())
        }
        assertFalse(planProject(spec(config("android" to android), "targets" to listOf("ios")), output).valid)
        assertFalse(planProject(spec(config("android" to android), "app_id" to "org.other.app"), output).valid)
        val wrongSender = JsonObject(ios + ("GCM_SENDER_ID" to value("999999999")))
        assertFalse(planProject(spec(config("ios" to wrongSender)), output).valid)
        val wrongNumber = JsonObject(android + ("project_info" to value(mapOf("project_id" to "aurora-demo", "project_number" to "999999999"))))
        assertFalse(planProject(spec(config("android" to wrongNumber)), output).valid)
        val otherProject = JsonObject(ios + mapOf("GCM_SENDER_ID" to value("999999999"), "GOOGLE_APP_ID" to value("1:999999999:ios:abcdef")))
        assertFalse(planProject(spec(config("android" to android, "ios" to otherProject)), output).valid)
    }
    @Test fun configPathsAreRelativeToSpecAndNormalizeToPortableContents() {
        temp.resolve("google-services.json").writeText(encode(android))
        temp.resolve("GoogleService-Info.plist").writeText("""
            <?xml version="1.0"?><!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0"><dict>${ios.entries.joinToString("") { (k, v) -> "<key>$k</key>" + if ((v as JsonPrimitive).isString) "<string>${v.content}</string>" else "<true/>" }}</dict></plist>
        """.trimIndent())
        val path = temp.resolve("config.json")
        path.writeText(encode(spec(config("android_config" to "google-services.json", "ios_config" to "GoogleService-Info.plist"))))
        val first = plan(readSpec(path))
        assertFalse(temp.toString() in encode(first.spec))
        temp.resolve("google-services.json").deleteExisting(); temp.resolve("GoogleService-Info.plist").deleteExisting()
        val copy = planProject(first.spec, temp.resolve("copy"), first.lock)
        assertTrue(copy.valid)
        val changed = JsonObject(first.spec + ("firebase" to value(config("android" to android, "ios" to JsonObject(ios + ("API_KEY" to value("rotated")))))))
        assertEquals("lock_mismatch", planProject(changed, output, first.lock).issues.single().code)
    }
}
