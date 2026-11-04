package com.nstl.openplatform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SandboxServiceImplUrlTest {

    @Test
    void absoluteUrlWithQueryIsUsedWithoutRebuildingOrDuplicatingParams() {
        String originalUrl = "https://api.example.com/search?q=NSTL&rows=20";
        Map<String, Object> displayedParams = new LinkedHashMap<>();
        displayedParams.put("q", "NSTL");

        String targetUrl = SandboxServiceImpl.buildTargetUrl(
                "http://mockoon.invalid",
                originalUrl,
                displayedParams
        );

        assertEquals(originalUrl, targetUrl);
    }

    @Test
    void relativeUrlWithQueryAlsoKeepsItsOriginalQuery() {
        String targetUrl = SandboxServiceImpl.buildTargetUrl(
                "https://api.example.com/",
                "/search?q=NSTL&rows=20",
                Map.of("q", "changed")
        );

        assertEquals("https://api.example.com/search?q=NSTL&rows=20", targetUrl);
    }

    @Test
    void paramsAreAppendedWhenUrlHasNoQueryString() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("q", "NSTL");
        params.put("rows", 20);

        String targetUrl = SandboxServiceImpl.buildTargetUrl(
                "https://api.example.com/",
                "/search",
                params
        );

        assertEquals("https://api.example.com/search?q=NSTL&rows=20", targetUrl);
    }
}
