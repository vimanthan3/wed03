plugins {
    id("java")
}
// tag::project-dependencies[]
dependencies {
    implementation(projects.utils("distribution"))
    implementation(projects.api)
}
// end::project-dependencies[]
