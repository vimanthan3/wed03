// tag::all[]
// tag::no-description[]
tasks.register<Copy>("copy") {
    group = "Help"
// end::no-description[]
    description = "Copies the resource directory to the target directory."
// tag::no-description[]
    from("resources")
    into("target")
    include("**/*.txt", "**/*.xml", "**/*.properties")
}
// end::no-description[]
// end::all[]
