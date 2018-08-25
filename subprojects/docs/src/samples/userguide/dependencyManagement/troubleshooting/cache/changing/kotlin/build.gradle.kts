plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        setUrl("https://repo.spring.io/snapshot/")
    }
}

dependencies {
    implementation("org.springframework:spring-web:5.0.3.BUILD-SNAPSHOT")
}

// tag::changing-module-cache-control[]
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(4, "hours")
}
// end::changing-module-cache-control[]

tasks.create<Copy>("copyLibs") {
    from(configurations.compileClasspath)
    into("$buildDir/libs")
}
