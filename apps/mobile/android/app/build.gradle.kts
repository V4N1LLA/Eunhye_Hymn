import java.util.Base64

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

fun Project.flutterDartDefine(name: String): String? {
    val encoded = findProperty("dart-defines") as String? ?: return null
    return encoded
        .split(",")
        .asSequence()
        .mapNotNull { raw ->
            runCatching { String(Base64.getDecoder().decode(raw)) }.getOrNull()
        }
        .mapNotNull { define ->
            val index = define.indexOf('=')
            if (index <= 0) {
                null
            } else {
                define.substring(0, index) to define.substring(index + 1)
            }
        }
        .firstOrNull { (key, _) -> key == name }
        ?.second
}

android {
    namespace = "com.example.eunhye_hymn_mobile"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.example.eunhye_hymn_mobile"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        val kakaoNativeAppKey = flutterDartDefine("KAKAO_NATIVE_APP_KEY")
            ?.takeIf { it.isNotBlank() }
        manifestPlaceholders["kakaoScheme"] =
            if (kakaoNativeAppKey == null) "kakao" else "kakao$kakaoNativeAppKey"
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

flutter {
    source = "../.."
}
