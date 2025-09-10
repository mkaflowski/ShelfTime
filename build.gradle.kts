buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.12.1")
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.12.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
}
