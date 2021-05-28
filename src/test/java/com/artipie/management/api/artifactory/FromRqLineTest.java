/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link FromRqLine}.
 * @since 0.1
 */
final class FromRqLineTest {
    @Test
    void shouldReturnEmptyForBadRqLineUser() {
        final FromRqLine user = new FromRqLine(
            "GET /bad/api/security/users HTTP/1.1", FromRqLine.RqPattern.USER
        );
        MatcherAssert.assertThat(
            user.get().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldReturnUsernameFromRqLine() {
        final String username = "john";
        final FromRqLine user = new FromRqLine(
            String.format("GET /api/security/users/%s HTTP/1.1", username),
            FromRqLine.RqPattern.USER
        );
        MatcherAssert.assertThat(
            user.get().get(),
            new IsEqual<>(username)
        );
    }

    @Test
    void shouldReturnEmptyForBadRqLineRepo() {
        final FromRqLine repo = new FromRqLine(
            "GET /bad/api/security/permissions HTTP/1.1", FromRqLine.RqPattern.REPO
        );
        MatcherAssert.assertThat(
            repo.get().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldReturnRepoFromRqLine() {
        final String docker = "docker";
        final FromRqLine repo = new FromRqLine(
            String.format("GET /api/security/permissions/%s HTTP/1.1", docker),
            FromRqLine.RqPattern.REPO
        );
        MatcherAssert.assertThat(
            repo.get().get(),
            new IsEqual<>(docker)
        );
    }
}
