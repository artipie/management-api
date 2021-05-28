/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.http.Headers;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CookiesAuthScheme}.
 *
 * @since 0.1
 * @todo #231:30min Add tests for Cookies class.
 *  `Cookies` class lacks test coverage for primary code flow branches.
 *  It is important that to check that proper user is extracted when headers contain session cookie.
 *  This might require `Cookie` class refactoring as the class depends on system environment.
 */
class CookiesTest {

    @Test
    void shouldNotFindUserInEmptyHeaders() {
        MatcherAssert.assertThat(
            new CookiesAuthScheme().authenticate(Headers.EMPTY).toCompletableFuture().join().user(),
            new IsEqual<>(Optional.empty())
        );
    }
}
