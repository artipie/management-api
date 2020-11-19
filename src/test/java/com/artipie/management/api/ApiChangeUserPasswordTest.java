/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
