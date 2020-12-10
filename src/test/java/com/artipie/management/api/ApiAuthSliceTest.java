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
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ApiAuthSlice}.
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ApiAuthSliceTest {

    @Test
    void usesCookiesWhenPresent() {
        final String user = "aladdin";
        final String pswd = "open";
        MatcherAssert.assertThat(
            new ApiAuthSlice(
                new Authentication.Single(user, pswd),
                new Permissions.Single(user, "api"),
                new SliceSimple(StandardRs.OK),
                new AuthScheme.Fake(user)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, String.format("/%s", user)),
                new Headers.From(new Authorization.Basic(user, pswd)),
                new Content.Empty()
            )
        );
    }

    @Test
    void usesCookiesAndRequiresPermissions() {
        final String user = "aladdin";
        final String pswd = "open";
        MatcherAssert.assertThat(
            new ApiAuthSlice(
                new Authentication.Single(user, pswd),
                new Permissions.Single("someone", "anything"),
                new SliceSimple(StandardRs.OK),
                new AuthScheme.Fake(user)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FORBIDDEN),
                new RequestLine(RqMethod.GET, String.format("/%s", user)),
                new Headers.From(new Authorization.Basic(user, pswd)),
                new Content.Empty()
            )
        );
    }

    @Test
    void usesBasicAndUsernameInPathWhenCookiesAreAbsent() {
        final String user = "mark";
        final String pswd = "abc";
        MatcherAssert.assertThat(
            new ApiAuthSlice(
                new Authentication.Single(user, pswd),
                new Permissions.Single(user, "api"),
                new SliceSimple(StandardRs.OK),
                new AuthScheme.Fake()
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, String.format("/%s", user)),
                new Headers.From(new Authorization.Basic(user, pswd)),
                new Content.Empty()
            )
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/api/security/users/any", "/api/security/permissions/my_repo",
            "/api/security/permissions", "/api/repositories/abc/123"
        }
    )
    void passesByRqPatterns(final String rqline) {
        final String user = "jane";
        final String pswd = "000";
        MatcherAssert.assertThat(
            new ApiAuthSlice(
                new Authentication.Single(user, pswd),
                new Permissions.Single(user, "api"),
                new SliceSimple(StandardRs.OK),
                new AuthScheme.Fake()
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, rqline),
                new Headers.From(new Authorization.Basic(user, pswd)),
                new Content.Empty()
            )
        );
    }

    @Test
    void doesNotAuthorizeByBasicWhenNoPermissionsSet() {
        final String user = "mark";
        final String pswd = "abc";
        MatcherAssert.assertThat(
            new ApiAuthSlice(
                new Authentication.Single(user, pswd),
                new Permissions.Single("someone", "anything"),
                new SliceSimple(StandardRs.OK),
                new AuthScheme.Fake()
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FORBIDDEN),
                new RequestLine(RqMethod.GET, "/some"),
                new Headers.From(new Authorization.Basic(user, pswd)),
                new Content.Empty()
            )
        );
    }

}
