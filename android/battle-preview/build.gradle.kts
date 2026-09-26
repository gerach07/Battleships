plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val generatedPreviewResDir = layout.buildDirectory.dir("generated/res/battlePreview")

val copyBattlePreviewResources by tasks.registering(Copy::class) {
    from("../app/src/main/res/layout") {
        include("activity_battle_screen.xml")
        include("preview_iphone_battle_screen.xml")
        include("preview_battle_header.xml")
        include("preview_battle_enemy_grid.xml")
        include("preview_battle_fleet_grid.xml")
        include("preview_battle_row_*.xml")
        into("layout")
    }
    from("../app/src/main/res/drawable") {
        include("preview_battle_*.xml")
        into("drawable")
    }
    from("../app/src/main/res/values") {
        include("preview_strings.xml")
        into("values")
    }
    into(generatedPreviewResDir)
}

android {
    namespace = "com.anasio.battleships.preview"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anasio.battleships.preview"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets.getByName("main").res.srcDir(generatedPreviewResDir)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}

tasks.named("preBuild").configure {
    dependsOn(copyBattlePreviewResources)
}