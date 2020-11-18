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
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.management.Users;
import com.artipie.management.api.ContentAsJson;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.json.JsonString;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cactoos.list.ListOf;
import org.reactivestreams.Publisher;

/**
 * Artifactory `PUSH/PUT /api/security/users/{userName}` endpoint,
 * updates/adds user record in credentials.
 *
 * @since 0.1
 */
public final class AddUpdateUserSlice implements Slice {

    /**
     * Artipie users.
     */
    private final Users users;

    /**
     * Ctor.
     *
     * @param users Artipie users
     */
    public AddUpdateUserSlice(final Users users) {
        this.users = users;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> user = new FromRqLine(line, FromRqLine.RqPattern.USER).get();
        return user.<Response>map(
            username -> new AsyncResponse(
                AddUpdateUserSlice.info(body, username).thenCompose(
                    json -> json.map(
                        info -> this.users.add(
                            info.getKey(),
                            DigestUtils.sha256Hex(info.getValue()),
                            Users.PasswordFormat.SHA256
                        ).thenApply(ok -> new RsWithStatus(RsStatus.OK))
                    ).orElse(
                        CompletableFuture.completedFuture(new RsWithStatus(RsStatus.BAD_REQUEST))
                    )
                )
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }

    /**
     * Extracts password and email from the request body.
     *
     * @param body Request body
     * @param name Username
     * @return Password and email as completion.
     */
    private static CompletionStage<Optional<Pair<Users.User, String>>> info(
        final Publisher<ByteBuffer> body, final String name
    ) {
        final String email = "email";
        final String pswd = "password";
        final String groups = "groups";
        return Single.just(body).to(new ContentAsJson()).map(
            json -> {
                final Optional<Pair<Users.User, String>> res;
                if (json.containsKey(pswd) && json.containsKey(email)) {
                    Set<String> set = new HashSet<>(1);
                    if (json.containsKey(groups)) {
                        set = json.getJsonArray(groups).getValuesAs(JsonString.class)
                            .stream().map(JsonString::getString).collect(Collectors.toSet());
                    }
                    set.add("readers");
                    res = Optional.of(
                        new ImmutablePair<>(
                            new Users.User(
                                name, Optional.of(json.getString(email)), new ListOf<>(set)
                            ),
                            json.getString(pswd)
                        )
                    );
                } else {
                    res = Optional.empty();
                }
                return res;
            }
        ).to(SingleInterop.get());
    }
}
