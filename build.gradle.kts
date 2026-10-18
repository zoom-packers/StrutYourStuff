import java.util.zip.ZipEntry
import java.util.zip.ZipFile

// NeoForge 26.3 build for Zoompack 5, against ZP5's NeoForge port of Create Fly (see PORTING.md).
plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.148"
}

group = property("maven_group") as String
version = "${property("mod_version")}+neoforge-${property("minecraft_version")}-${property("port_build")}"

base {
    archivesName.set(property("archives_base_name") as String)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

val createJar = files(property("create_jar") as String)

sourceSets {
    main {
        // Fabric entrypoints and metadata, replaced by com/cake/struts/neoforge/** (see PORTING.md)
        java.exclude("com/cake/struts/fabric/**")
        resources.exclude("fabric.mod.json")
    }
}

neoForge {
    version = property("neo_version") as String

    // Create's access transformer and interface injections, so code that uses them compiles
    accessTransformers.from(file("libs/create-accesstransformer.cfg"))
    interfaceInjectionData.from(file("libs/create-interfaces.json"))

    mods {
        create("struts") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
            programArgument("--nogui")
        }
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", "struts")
        }
        configureEach {
            logLevel = org.slf4j.event.Level.INFO
        }
    }
}

dependencies {
    compileOnly(createJar)
    // Create is loaded as a mod in the dev runs
    runtimeOnly(createJar)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000", "-Xmaxwarns", "1000"))
}

tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Expands ${...} placeholders in src/main/templates (neoforge.mods.toml) from gradle.properties
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_version_range" to project.property("minecraft_version_range") as String,
        "neo_version_range" to project.property("neo_version_range") as String,
        "create_version_range" to project.property("create_version_range") as String,
    )
    inputs.properties(props)
    expand(props)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.jar {
    from("LICENSE")
    from("NOTICE")
}

tasks.named<Jar>("sourcesJar") {
    from("LICENSE")
    from("NOTICE")
}

val allowedJarPrefixes = listOf(
    "com/cake/struts/",
    "assets/struts/",
    "data/struts/",
    "META-INF/",
    "LICENSE",
    "NOTICE",
    "struts.mixins.json",
    "struts.client.mixins.json",
)

afterEvaluate {
    val checkForeignNamespaces = tasks.register("checkForeignNamespaces") {
        val jarNames = listOf("remapJar", "remapSourcesJar", "jar", "sourcesJar")
                .filter { tasks.names.contains(it) }
                .let { names -> if (names.contains("remapJar")) names.filter { it.startsWith("remap") } else names }
        require(jarNames.isNotEmpty()) { "checkForeignNamespaces found no jar task to inspect" }
        val jarTasks = jarNames.map { tasks.named<Jar>(it).get() }
        dependsOn(jarTasks)
        val archives: List<Provider<RegularFile>> = jarTasks.map { it.archiveFile }
        val prefixes = allowedJarPrefixes
        doLast {
            val bad = mutableListOf<String>()
            var checked = 0
            for (provider in archives) {
                val jar: File = provider.get().asFile
                if (!jar.exists()) continue
                checked++
                val zip = ZipFile(jar)
                try {
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry: ZipEntry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name: String = entry.name
                        if (prefixes.none { p -> name.startsWith(p) }) {
                            bad.add(jar.name + "!" + name)
                        }
                    }
                } finally {
                    zip.close()
                }
            }
            if (checked == 0) {
                throw GradleException("checkForeignNamespaces inspected no archives")
            }
            if (bad.isNotEmpty()) {
                throw GradleException("Foreign namespace entries in published artifacts:\n" + bad.joinToString("\n"))
            }
        }
    }
    tasks.named("check") { dependsOn(checkForeignNamespaces) }
}

tasks.register("printCompileClasspath") {
    val cp = sourceSets.main.get().compileClasspath
    val out = layout.projectDirectory.file(".classpath.txt")
    doLast {
        out.asFile.writeText(cp.files.joinToString("\n") { it.absolutePath } + "\n")
    }
}
