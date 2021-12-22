/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.management.ConfigFiles;
import com.artipie.management.misc.ValueFromBody;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import org.reactivestreams.Publisher;

/**
 * Repo {@code DELETE} API.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ApiRepoDeleteSlice implements Slice {
    /**
     * Config file to support `.yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     * @param configfile Config file to support `.yaml` and `.yml` extensions
     */
    ApiRepoDeleteSlice(final Storage storage, final ConfigFiles configfile) {
        this.storage = storage;
        this.configfile = configfile;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Matcher matcher = ApiRepoPostRtSlice.PTN.matcher(
            new RequestLineFrom(line).uri().getPath()
        );
        if (!matcher.matches()) {
            throw new IllegalStateException(
                String.format(
                    "Uri '%s' does not match to the pattern '%s'",
                    line,
                    ApiRepoPostRtSlice.PTN
                )
            );
        }
        final String user = matcher.group("user");
        return new AsyncResponse(
            new PublisherAs(body).asciiString()
                .thenCompose(
                    form -> {
                        final ValueFromBody vals = new ValueFromBody(form);
                        final String name = vals.byNameOrThrow("repo");
                        final Key repo = new Key.From(user, String.format("%s.yaml", name));
                        return this.configfile.exists(repo)
                            .thenCompose(
                                exists -> {
                                    final CompletionStage<Response> res;
                                    if (exists) {
                                        res = this.deleteConfigAndItems(
                                            repo, new Key.From(user, name)
                                        ).thenApply(
                                            noth -> new RsWithHeaders(
                                                new RsWithStatus(RsStatus.OK),
                                                new Headers.From(
                                                    "Location",
                                                    String.format("/dashboard/%s", user)
                                                )
                                            )
                                        );
                                    } else {
                                        res = CompletableFuture.completedFuture(
                                            new RsWithBody(
                                                new RsWithStatus(RsStatus.BAD_REQUEST),
                                                String.format("Failed to delete repo '%s'", repo),
                                                StandardCharsets.UTF_8
                                            )
                                        );
                                    }
                                    return res;
                                }
                            );
                    }
                )
        );
    }

    /**
     * Removes configuration file and items from the storage.
     * @param repo Key to the repo configuration
     * @param prefix Key to the repo with items
     * @return Result of completion
     */
    private CompletionStage<Void> deleteConfigAndItems(final Key repo, final Key prefix) {
        return this.configfile.delete(repo)
            .thenCompose(noth -> this.storage.deleteAll(prefix));
    }
}
