// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    kotlin("android") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    alias(libs.plugins.android.library) apply false
}
