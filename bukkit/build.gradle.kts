plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// group/version inherited from rootProject's allprojects block

repositories {
    maven {
        name = "Spigot"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation(project(":common")) {
        exclude("dev.architectury", "architectury-transformer")
        exclude("software.bernie.geckolib")
        exclude("net.fabricmc.fabric-api")
        exclude("net.fabricmc", "fabric-loader")
    }
    implementation("com.github.CSneko:NekoAI:v0.1.3-alpha")
    compileOnly(files("libs/paper-1.21.jar"))
    compileOnly("me.clip:placeholderapi:2.11.6")
}


tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    mergeServiceFiles()

    // Only include common classes, exclude entity/rendering that depends on Minecraft/GeckoLib
    from(project(":common").sourceSets.main.get().output) {
        into("/")
        exclude("org/cneko/toneko/common/mod/entities/NekoEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/AdventurerNeko*.class")
        exclude("org/cneko/toneko/common/mod/entities/CrystalNekoEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/FightingNekoEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/GhostNekoEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/NoelleMaidNekoEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/RavennEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/FlySwordEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/AmmunitionEntity*.class")
        exclude("org/cneko/toneko/common/mod/entities/boss/")
        exclude("org/cneko/toneko/common/mod/entities/ai/")
        exclude("org/cneko/toneko/common/mod/client/")
        exclude("org/cneko/toneko/common/mod/mixin/")
        exclude("org/cneko/toneko/common/mod/blocks/")
        exclude("org/cneko/toneko/common/mod/items/")
        exclude("org/cneko/toneko/common/mod/genetics/")
        exclude("org/cneko/toneko/common/mod/packets/")
        exclude("org/cneko/toneko/common/mod/recipes/")
        exclude("org/cneko/toneko/common/mod/effects/")
        exclude("org/cneko/toneko/common/mod/events/")
        exclude("org/cneko/toneko/common/mod/ModBootstrap.class")
        exclude("org/cneko/toneko/common/mod/misc/ToNekoAttributes.class")
        exclude("org/cneko/toneko/common/mod/misc/ToNekoDamageTypes.class")
        exclude("org/cneko/toneko/common/mod/misc/ToNekoEnchantments.class")
        exclude("org/cneko/toneko/common/mod/misc/ToNekoSoundEvents.class")
        exclude("org/cneko/toneko/common/mod/misc/ToNekoSongs.class")
        exclude("org/cneko/toneko/common/mod/items/NekoArmor*.class")
        exclude("org/cneko/toneko/common/mod/entities/NekoBrain*.class")
        exclude("org/cneko/toneko/common/mod/entities/NekoLookController*.class")
        exclude("org/cneko/toneko/common/mod/entities/NekoInventory*.class")
        exclude("org/cneko/toneko/common/mod/api/")
        exclude("org/cneko/toneko/fabric/")
        exclude("org/cneko/toneko/neoforge/")
        exclude("org/cneko/gal/common/client/")
    }

    dependencies {
        exclude(dependency(".*:.*"))
        exclude("mappings.tiny")
        exclude("**/mappings.tiny")
    }

    from(project(":common").configurations.getByName("runtimeClasspath").filter {
        it.name.contains("NekoAI")
    }) {
        into("/")
    }

}


tasks.processResources {
    filesMatching("*.yml") {
        expand(project.properties)
    }
}

tasks.build {
    dependsOn("shadowJar")
}