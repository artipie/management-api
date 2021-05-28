/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.http.auth.Authentication;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link ApiPermission}.
 *
 * @since 0.1
 */
class ApiPermissionTest {

    @ParameterizedTest
    @CsvSource({
        "/,false",
        "/dashboard/alice,true",
        "/api/lalala/alice,true",
        "/dashboard/bob,false",
        "/api/lalala/bob,false"
    })
    void allowed(final String path, final boolean result) {
        MatcherAssert.assertThat(
            new ApiPermission(new RequestLine(RqMethod.GET, path).toString()).allowed(
                new Authentication.User("alice")
            ),
            new IsEqual<>(result)
        );
    }
}
