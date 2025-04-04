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
include(":universal_link_guard")
include(":file_scanner")
include(":flagged_links_logger")
include(":threat_model")
