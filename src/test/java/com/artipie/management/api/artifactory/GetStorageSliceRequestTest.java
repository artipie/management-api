/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link GetStorageSlice.Request}.
 *
 * @since 0.3
 */
class GetStorageSliceRequestTest {

    @ParameterizedTest
    @CsvSource({
        "flat,/api/storage/my-lib/foo/bar,my-lib",
        "org,/api/storage/my-company/my-lib/foo/bar,my-company/my-lib"
    })
    void shouldParseRepo(
        final String layout,
        final String path,
        final String repo
    ) {
        final GetStorageSlice.Request request = new GetStorageSlice.Request(
            GetStorageSliceTest.PATTERNS.get(layout),
            new RequestLine(RqMethod.GET, path).toString()
        );
        MatcherAssert.assertThat(
            request.repo(),
            new IsEqual<>(repo)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "flat,/api/storage/my-lib/foo/bar,foo/bar",
        "org,/api/storage/my-company/my-lib/foo/bar,foo/bar"
    })
    void shouldParseRoot(
        final String layout,
        final String path,
        final String root
    ) {
        final GetStorageSlice.Request request = new GetStorageSlice.Request(
            GetStorageSliceTest.PATTERNS.get(layout),
            new RequestLine(RqMethod.GET, path).toString()
        );
        MatcherAssert.assertThat(
            request.root().string(),
            new IsEqual<>(root)
        );
    }
}
