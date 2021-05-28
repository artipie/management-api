/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.asto.Key;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.management.ConfigFiles;
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
     * Repository permissions.
     */
    private final RepoPermissions permissions;

    /**
     * Config file to support `yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * Ctor.
     * @param permissions Artipie repository permissions
     * @param configfile Config file to support `yaml` and `.yml` extensions
     */
    public DeletePermissionSlice(final RepoPermissions permissions, final ConfigFiles configfile) {
        this.permissions = permissions;
        this.configfile = configfile;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> opt = new FromRqLine(line, FromRqLine.RqPattern.REPO).get();
        return opt.<Response>map(
            repo -> new AsyncResponse(
                this.configfile.exists(new Key.From(repo))
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
