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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.management.RepoPermissions;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Artifactory `DELETE /api/security/permissions/{target}` endpoint, deletes all permissions from
 * repository.
 * @since 0.1
 */
public final class DeletePermissionSlice implements Slice {

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Repository permissions.
     */
    private final RepoPermissions permissions;

    /**
     * Ctor.
     * @param storage Artipie settings storage
     * @param permissions Artipie repository permissions
     */
    public DeletePermissionSlice(final Storage storage, final RepoPermissions permissions) {
        this.storage = storage;
        this.permissions = permissions;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> opt = new FromRqLine(line, FromRqLine.RqPattern.REPO).get();
        return opt.<Response>map(
            repo -> new AsyncResponse(
                this.storage
                    .exists(new Key.From(String.format("%s.yaml", repo)))
                    .thenCompose(
                        exists -> {
                            final CompletionStage<Response> res;
                            if (exists) {
                                res = this.permissions.remove(repo).thenApply(
                                    ignored -> new RsWithBody(
                                        // @checkstyle LineLengthCheck (2 lines)
                                        String.format("Permission Target '%s' has been removed successfully.", repo),
                                        StandardCharsets.UTF_8
                                    )
                                );
                            } else {
                                res = CompletableFuture.completedStage(StandardRs.NOT_FOUND);
                            }
                            return res;
                        }
                    )
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }
}
