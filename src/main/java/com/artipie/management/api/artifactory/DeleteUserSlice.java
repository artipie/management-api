/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.management.Users;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Artifactory `DELETE /api/security/users/{userName}` endpoint,
 * deletes user record from credentials.
 *
 * @since 0.1
 */
public final class DeleteUserSlice implements Slice {
    /**
     * Artipie users.
     */
    private final Users users;

    /**
     * Ctor.
     * @param users Users
     */
    public DeleteUserSlice(final Users users) {
        this.users = users;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> user = new FromRqLine(line, FromRqLine.RqPattern.USER).get();
        return user.<Response>map(
            username -> new AsyncResponse(
                this.users.list().thenApply(
                    items -> items.stream().anyMatch(item -> item.name().equals(username))
                ).thenCompose(
                    has -> {
                        final CompletionStage<Response> resp;
                        if (has) {
                            resp = this.users.remove(username)
                                .thenApply(
                                    ok -> new RsWithBody(
                                        new RsWithStatus(RsStatus.OK),
                                        String.format(
                                            "User '%s' has been removed successfully.",
                                            username
                                        ).getBytes(StandardCharsets.UTF_8)
                                    )
                                );
                        } else {
                            resp = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                        }
                        return resp;
                    }
                )
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }
}
