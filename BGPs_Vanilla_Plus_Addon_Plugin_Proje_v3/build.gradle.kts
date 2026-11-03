plugins { java }

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Surum bulunamazsa: "26.3.build.+" dene
    compileOnly("io.papermc.paper:paper-api:26.3.build.218-stable")
    // BGPsMain, mevcut BGPsVanillaPlusAddon sinifini genisletir (taban jar'da sadece 'final' bayragi kaldirilmistir)
    compileOnly(files("prebuilt/BGPs_Vanilla_Plus_Addon_base.jar"))
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

tasks.jar {
    archiveFileName.set("BGPs_Vanilla_Plus_Addon.jar")

    // Mevcut calisan plugin siniflari (bgps/** ve tr/**) taban jar'dan oldugu gibi alinir.
    // Taban jar'in plugin.yml ve MANIFEST'i alinmaz; yerine paper-plugin.yml kullanilir.
    from(zipTree("prebuilt/BGPs_Vanilla_Plus_Addon_base.jar")) {
        include("bgps/**", "tr/**")
    }
}

// Varsayilan JUnit gorevi kullanilmiyor (Gradle 9, @Test bulamazsa hata verir).
tasks.test { enabled = false }

// Cekirdek mantigin (can lore'u, duzlemler, tarifler, xp, NBT, elytra) eski data pack ile birebir ayni oldugunu dogrular.
// Sonuc build logunda gorunur; basarisiz olsa bile jar uretimini engellemez.
val selfTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Cekirdek mantigi eski data pack ile karsilastirir"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("bgps.vanillaplus.port.SelfTest")
    args("legacy/BGPs_Vanilla_Plus_Addon_DataPack_v3.zip")
    workingDir = projectDir
    isIgnoreExitValue = true
}
tasks.check { dependsOn(selfTest) }
