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

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.SliceSimple;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.reactivestreams.Publisher;

/**
 * API authentication slice.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class ApiAuthSlice implements Slice {

    /**
     * Authentication.
     */
    private final Authentication auth;

    /**
     * Permissions.
     */
    private final Permissions perms;

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Authentication scheme.
     */
    private final AuthScheme scheme;

    /**
     * Ctor.
     * @param auth Authentication
     * @param perms Permissions
     * @param origin Origin slice
     * @param scheme Cookies
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public ApiAuthSlice(
        final Authentication auth,
        final Permissions perms,
        final Slice origin,
        final AuthScheme scheme
    ) {
        this.auth = auth;
        this.perms = perms;
        this.origin = origin;
        this.scheme = scheme;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Permission permission = new Permission.All(
            new ApiPermission(line),
            new Permission.ByName(this.perms, () -> new ListOf<>("api"))
        );
        return new AsyncSlice(
            this.scheme.authenticate(headers).thenApply(
                res -> {
                    final Slice slice;
                    final Optional<Authentication.User> user = res.user();
                    if (user.isPresent() && permission.allowed(user.get())) {
                        slice = this.origin;
                    } else if (user.isEmpty()) {
                        slice = new BasicAuthSlice(this.origin, this.auth, permission);
                    } else {
                        slice = new SliceSimple(new RsWithStatus(RsStatus.FORBIDDEN));
                    }
                    return slice;
                }
            )
        ).response(line, headers, body);
    }
}
