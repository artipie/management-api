/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.misc;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ValueFromBody}.
 * @since 0.5
 */
final class ValueFromBodyTest {
    @ParameterizedTest
    @ValueSource(strings = {"first=ignore&key=target", "key=target&second=ignore"})
    void returnsTargetByName(final String payload) {
        MatcherAssert.assertThat(
            new ValueFromBody(payload).byName("key").get(),
            new IsEqual<>("target")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "absent=any", "first=val1&second=val2"})
    void returnsEmptyIfAbsent(final String payload) {
        MatcherAssert.assertThat(
            new ValueFromBody(payload).byName("some").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsValueWhichContainsAmpersand() throws URISyntaxException {
        final String key = "mykey";
        final String val = "myval&with&ampersand";
        final URIBuilder bldr = new URIBuilder();
        bldr.addParameter(key, val);
        bldr.addParameter("second", "ignore");
        MatcherAssert.assertThat(
            new ValueFromBody(
                bldr.build().getRawQuery(), StandardCharsets.US_ASCII
            ).byName(key).get(),
            new IsEqual<>(val)
        );
    }
}
