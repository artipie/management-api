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
package com.artipie.management.api.artifactory;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rs.common.RsJson;
import com.artipie.management.Users;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Artifactory `GET /api/security/users/{userName}` endpoint, returns user information.
 * @since 0.1
 */
public final class GetUserSlice implements Slice {

    /**
     * Artipie users.
     */
    private final Users users;

    /**
     * Ctor.
     * @param users Artipie users
     */
    public GetUserSlice(final Users users) {
        this.users = users;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> name = new FromRqLine(line, FromRqLine.RqPattern.USER).get();
        return name.<Response>map(
            username -> new AsyncResponse(
                this.users.list().thenApply(
                    items -> items.stream().filter(item -> item.name().equals(username)).findFirst()
                ).thenApply(
                    user -> {
                        final Response resp;
                        if (user.isPresent()) {
                            resp = new RsJson(
                                Json.createObjectBuilder()
                                    .add("name", user.get().name())
                                    .add(
                                        "email",
                                        user.get().email().orElse(
                                            String.format("%s@artipie.com", user.get().name())
                                        )
                                    )
                                    .add("lastLoggedIn", "2020-01-01T01:01:01.000+01:00")
                                    .add("realm", "Internal")
                                    .add(
                                        "groups",
                                        Json.createArrayBuilder(user.get().groups()).build()
                                    )::build,
                                StandardCharsets.UTF_8
                            );
                        } else {
                            resp = StandardRs.NOT_FOUND;
                        }
                        return resp;
                    }
                )
            )
        ).orElse(StandardRs.NOT_FOUND);
    }
}
