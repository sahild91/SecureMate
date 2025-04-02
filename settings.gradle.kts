pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SecureMate"
include(":app")
include(":sms_scanner")
include(":wifi_scanner")
include(":intruder_selfie")
include(":permission_monitor")
include(":developer_options_toggle")
include(":link_interceptor")
include(":file_scanner")
