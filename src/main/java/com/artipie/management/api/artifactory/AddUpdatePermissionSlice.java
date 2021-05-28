/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.management.RepoPermissions;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.reactivestreams.Publisher;

/**
 * Artifactory `PUT /api/security/permissions/{target}` endpoint, updates `permissions` section
 * in repository section.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AddUpdatePermissionSlice implements Slice {

    /**
     * Permissions mapping: translates artifactory permissions on Artipie language.
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private static final Map<String, String> MAPPING =
        new MapOf<>(
            new MapEntry<>("r", "read"),
            new MapEntry<>("read", "read"),
            new MapEntry<>("w", "write"),
            new MapEntry<>("write", "write"),
            new MapEntry<>("deploy", "write"),
            new MapEntry<>("m", "*"),
            new MapEntry<>("admin", "*"),
            new MapEntry<>("manage", "*"),
            new MapEntry<>("d", "delete"),
            new MapEntry<>("delete", "delete")
        );

    /**
     * Artipie settings storage.
     */
    private final RepoPermissions permissions;

    /**
     * Ctor.
     * @param permissions Artipie settings storage
     */
    public AddUpdatePermissionSlice(final RepoPermissions permissions) {
        this.permissions = permissions;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> opt = new FromRqLine(line, FromRqLine.RqPattern.REPO).get();
        return opt.<Response>map(
            repo -> new AsyncResponse(
                new PublisherAs(body).bytes().thenApply(
                    bytes -> Json.createReader(new ByteArrayInputStream(bytes)).readObject()
                ).thenCompose(
                    json -> this.update(json, repo).thenApply(
                        success -> {
                            final Response result;
                            if (success) {
                                result = StandardRs.EMPTY;
                            } else {
                                result = new RsWithStatus(RsStatus.BAD_REQUEST);
                            }
                            return result;
                        }
                    )
                )
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }

    /**
     * Update repo permissions.
     * @param json Json body
     * @param name Repository name
     * @return True - if JSON is valid and update performed,
     *  false - in case JSON is invalid and update was aborted
     */
    private CompletionStage<Boolean> update(final JsonObject json, final String name) {
        final JsonObject repo = json.getJsonObject("repo");
        final String actions = "actions";
        final List<RepoPermissions.PermissionItem> res =
            Stream.concat(
                Stream.of(new RepoPermissions.PermissionItem("/readers", new ListOf<>("read"))),
                Stream.concat(
                    AddUpdatePermissionSlice.permsFromJson(
                        Optional.ofNullable(repo.getJsonObject(actions).getJsonObject("users")),
                        ""
                    ),
                    AddUpdatePermissionSlice.permsFromJson(
                        Optional.ofNullable(repo.getJsonObject(actions).getJsonObject("groups")),
                        "/"
                    )
                )
            ).distinct().collect(Collectors.toList());
        final List<RepoPermissions.PathPattern> patterns = Optional.ofNullable(
            repo.getJsonArray("include-patterns")
        )
            .map(array -> array.getValuesAs(JsonString.class))
            .orElse(Collections.emptyList())
            .stream()
            .map(JsonString::getString)
            .map(RepoPermissions.PathPattern::new)
            .collect(Collectors.toList());
        final CompletionStage<Boolean> result;
        if (patterns.stream().allMatch(ptrn -> ptrn.valid(name))) {
            result = this.permissions
                .update(name, res, patterns)
                .thenApply(nothing -> true);
        } else {
            result = CompletableFuture.completedFuture(false);
        }
        return result;
    }

    /**
     * Permission items from json.
     * @param perms Json permissions
     * @param prefix Prefix for permission name
     * @return List of {@link RepoPermissions.PermissionItem}
     */
    private static Stream<RepoPermissions.PermissionItem> permsFromJson(
        final Optional<JsonObject> perms, final String prefix
    ) {
        return perms.map(items -> items.entrySet().stream()).map(
            items -> items.map(
                json -> new RepoPermissions.PermissionItem(
                    String.format("%s%s", prefix, json.getKey()), json.getValue()
                    .asJsonArray().stream().map(item -> item.toString().replace("\"", ""))
                    .map(
                        item -> Optional.ofNullable(
                            AddUpdatePermissionSlice.MAPPING.get(item)
                        ).orElseThrow(
                            () -> new IllegalArgumentException(
                                String.format("Unsupported permission '%s'!", item)
                            )
                        )
                    ).distinct().collect(Collectors.toList())
                )
            )
        ).orElse(Stream.empty());
    }
}
