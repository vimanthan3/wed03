// tag::http-build-cache[]
buildCache {
    remote<HttpBuildCache> {
        setUrl("https://example.com:8123/cache/")
    }
}
// end::http-build-cache[]

// tag::allow-untrusted-server[]
buildCache {
    remote<HttpBuildCache> {
        setUrl("https://example.com:8123/cache/")
        isAllowUntrustedServer = true
    }
}
// end::allow-untrusted-server[]
