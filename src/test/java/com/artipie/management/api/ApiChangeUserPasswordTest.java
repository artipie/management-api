/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeUsers;
import com.artipie.management.Users;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ApiChangeUserPassword}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ApiChangeUserPasswordTest {

    @Test
    void returnsFoundIfUserWasAddedToCredentials() {
        final String username = "mike";
        final String pswd = "qwerty123";
        final FakeUsers users = new FakeUsers(username);
        MatcherAssert.assertThat(
            "ApiChangeUserPassword response should be FOUND",
            new ApiChangeUserPassword(users),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.FOUND),
                    new RsHasHeaders(
                        new Headers.From("Location", String.format("/dashboard/%s", username))
                    )
                ),
                new RequestLine(RqMethod.PUT, String.format("/api/users/%s/password", username)),
                Headers.EMPTY,
                this.body(pswd)
            )
        );
        MatcherAssert.assertThat(
            "User with correct password should be added",
            users.pswd(username),
            new IsEqual<>(
                new FakeUsers.Password(DigestUtils.sha256Hex(pswd), Users.PasswordFormat.SHA256)
            )
        );
    }

    @Test
    void returnsFoundIfPasswordWasUpdated() {
        final String username = "john";
        final String pswd = "0000";
        final FakeUsers users = new FakeUsers(username);
        MatcherAssert.assertThat(
            "ApiChangeUserPassword response should be FOUND",
            new ApiChangeUserPassword(users),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.FOUND),
                    new RsHasHeaders(
                        new Headers.From("Location", String.format("/dashboard/%s", username))
                    )
                ),
                new RequestLine(RqMethod.PUT, String.format("/api/users/%s/password", username)),
                Headers.EMPTY,
                this.body(pswd)
            )
        );
        MatcherAssert.assertThat(
            "User with correct password should be added",
            users.pswd(username),
            new IsEqual<>(
                new FakeUsers.Password(DigestUtils.sha256Hex(pswd), Users.PasswordFormat.SHA256)
            )
        );
    }

    private Content body(final String pswd) {
        return new Content.From(String.format("password=%s", pswd).getBytes());
    }

}
