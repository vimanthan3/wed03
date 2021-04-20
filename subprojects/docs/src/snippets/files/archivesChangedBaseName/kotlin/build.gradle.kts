plugins {
    base
}

version = "1.0"

// tag::base-plugin-config[]
base {
    archivesBaseName = "gradle"
}
// end::base-plugin-config[]

val myZip by tasks.registering(Zip::class) {
    from("somedir")
}

val myOtherZip by tasks.registering(Zip::class) {
    archiveAppendix.set("wrapper")
    archiveClassifier.set("src")
    from("somedir")
}

tasks.register("echoNames") {
    doLast {
        println("Project name: ${project.name}")
        println(myZip.get().archiveFileName.get())
        println(myOtherZip.get().archiveFileName.get())
    }
}
