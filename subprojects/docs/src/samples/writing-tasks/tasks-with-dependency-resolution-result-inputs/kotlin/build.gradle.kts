plugins {
    id("application")
    id("com.example.dependency-reports")
}

dependencies {
    constraints {
        implementation("org.apache.commons:commons-text:1.9")
    }
    implementation("org.apache.commons:commons-text")
    implementation(project(":utilities"))
}
