plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
architectury {
    platformSetupLoomIde()
    neoForge()
}

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

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation(project(":common")) // 添加对 common 模块的依赖
    compileOnly(files("libs/paper-1.21.jar"))
    compileOnly("me.clip:placeholderapi:2.11.6")
}


tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("") // 移除默认的 `-all` 后缀
    mergeServiceFiles() // 合并服务文件（如果需要）

    // 只包含 common 模块的编译输出
    from(project(":common").sourceSets.main.get().output) {
        into("/") // 将 common 的代码放入 JAR 的根目录
    }

    // 排除 common 模块的依赖项
    dependencies {
        exclude(dependency(".*:.*"))
        // 排除 mappings.tiny 文件
        exclude("mappings.tiny") // 排除根目录下的 mappings.tiny 文件
        exclude("**/mappings.tiny") // 排除所有目录下的 mappings.tiny 文件
    }

}


tasks.build {
    dependsOn("shadowJar")// 将 shadowJar 任务绑定到 build 任务
}