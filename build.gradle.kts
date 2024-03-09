plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.3.3"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
}

group = "com.songoda"
version = "1.0.0"


repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven("https://repo.viaversion.com") // ViaVersion
    maven("https://repo.codemc.org/repository/maven-public/") // BentoBox
    maven("https://maven.playpro.com")
    maven("https://jitpack.io") // DecentHolograms | Vault ?
    maven("https://maven.enginehub.org/repo/")
    //maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    compileOnly("com.viaversion:viaversion-api:4.0.1")
    compileOnly("world.bentobox:bentobox:2.1.1-SNAPSHOT")
    compileOnly("net.coreprotect:coreprotect:22.1")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("org.bukkit")
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.4") {
        exclude("org.spigotmc")
    }
    //compileOnly("com.gmail.filoghost.holographicdisplays:holographicdisplays-api:2.4.9")
    compileOnly("com.github.Zrips:Jobs:v4.17.2") //repo is jitpack
    compileOnly(files("libs/Clearlag.jar"))
    compileOnly("io.lumine:Mythic-Dist:4.13.0")

    compileOnly("com.github.decentsoftware-eu:decentholograms:2.8.6")


    // paperweightDevBundle("com.example.paperfork", "1.18.1-R0.1-SNAPSHOT")

    // You will need to manually specify the full dependency if using the groovy gradle dsl
    // (paperDevBundle and paperweightDevBundle functions do not work in groovy)
    // paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.18.1-R0.1-SNAPSHOT")
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    /*
    reobfJar {
      // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
      // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
      outputJar.set(layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar"))
    }
     */
}
