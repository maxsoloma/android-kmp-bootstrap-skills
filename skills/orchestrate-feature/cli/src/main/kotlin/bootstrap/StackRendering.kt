package bootstrap

fun renderStack(o: Options): Rendered {
    if (o.hasData) return renderDataStack(o)
    val base = renderModern(o.copy(ui = defaultUi(o.kind), di = "manual", navigation = "none"))
    val files = base.files
    fun put(path: String, content: String) { files[path] = content.toByteArray() }
    fun template(name: String) = substitute(Resources.text("stacks/$name.template"), o.tokens)
    val native = o.ui == "swiftui"; val kmp = o.kind == "kmp"
    val logic = if (kmp) "shared" else "androidApp"
    val ui = if (kmp && !native) "shared" else "androidApp"
    val pkg = o.pkg.replace('.', '/')
    files.remove("$logic/src/$pkg/App.kt")
    files.remove("$logic/test/$pkg/GreetingTest.kt")
    val graph = if (o.di == "koin") """
internal class AppGraph {
    private val application = org.koin.dsl.koinApplication {
        modules(org.koin.dsl.module {
            single { CounterRepository() }
            factory { CounterService(get()) }
        })
    }
    val counter: CounterService = application.koin.get()
    fun close() = application.close()
}
""" else """
internal class AppGraph {
    private val repository = CounterRepository()
    val counter = CounterService(repository)
    fun close() {}
}
"""
    put("$logic/src/$pkg/CounterController.kt", template("CounterController") + graph.trimStart('\n'))
    put("$logic/test/$pkg/CounterTest.kt", template("CounterTest"))
    if (native) {
        files.keys.filter { it.startsWith("shared/src@ios/") }.toList().forEach(files::remove)
        put("iosApp/src/ContentView.swift", template(if (o.navigation == "platform-native") "SwiftNavigation" else "SwiftCounter").replace("import Shared", "import KotlinModules"))
    }
    if (!native || "android" in o.targets) {
        put("$ui/src/$pkg/App.kt", template(if (o.navigation == "none") "ComposeCounter" else "ComposeNavigation"))
        put("$ui/src/$pkg/CounterViewModel.kt", template("CounterViewModel"))
        if (ui == logic) put("$ui/test/$pkg/CounterViewModelTest.kt", template("CounterViewModelTest"))
    }
    if (o.navigation == "navigation3" || native && o.navigation == "platform-native" && "android" in o.targets) {
        put("$ui/src/$pkg/Routes.kt", template("Routes"))
        put("$ui/test/$pkg/RoutesTest.kt", template("RoutesTest"))
    }
    configureToolchain(o, files)
    val metadata = projectMetadata(o)
    put("bootstrap.json", encode(metadata))
    val viewModelNote = "Compose obtains CounterViewModel (androidx.lifecycle ${componentPins.text("lifecycle")}, not the JetBrains fork) through `viewModel(factory = counterViewModelFactory)`; clearing its ViewModelStore closes the controller. Navigation shares one ViewModel above the back stack. Add screen ViewModels the same way: a factory with `initializer` is required because non-JVM targets have no reflection; do not add `org.jetbrains.androidx.lifecycle` or `koin-compose-viewmodel` (it still depends on that fork).\n"
    val summary = "\n## Selected stack\n\nUI: ${o.ui}; DI: ${o.di}; navigation: ${o.navigation}.\n" +
        if (native) "Shared Kotlin logic has a JVM test target: `./kotlin test -m shared --platform jvm`. iOS owns SwiftUI state.\n" + (if ("android" in o.targets) viewModelNote else "") else viewModelNote
    for (path in listOf("README.md", "AGENTS.md")) put(path, files.getValue(path).toString(Charsets.UTF_8) + summary)
    configureKoinAgents(o, files)
    return Rendered(files, metadata)
}

const val xcodeAppCommand = "xcodebuild -project \"$(pwd -P)/iosApp/module.xcodeproj\" -scheme app -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' PROJECT_DIR=\"$(pwd -P)/iosApp\" CODE_SIGNING_ALLOWED=NO build"
