// tag::configure-build-src-build-cache[]
val isCiServer = System.getenv().containsKey("CI")

buildCache {
    local {
        isEnabled = !isCiServer
    }
    remote<HttpBuildCache> {
        setUrl("https://example.com:8123/cache/")
        isPush = isCiServer
    }
}
// end::configure-build-src-build-cache[]
