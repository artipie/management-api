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
package com.artipie.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Fake {@link RepoPermissions} implementation.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class FakeRepoPerms implements RepoPermissions {

    /**
     * Permissions.
     */
    private final Map<String, Pair<Collection<PermissionItem>, Collection<PathPattern>>> repos;

    /**
     * Ctor.
     * @param repos Repositories list
     */
    public FakeRepoPerms(
        final Map<String, Pair<Collection<PermissionItem>, Collection<PathPattern>>> repos
    ) {
        this.repos = repos;
    }

    /**
     * Ctor.
     * @param name Repository name
     * @param perms Permissions
     * @param patterns Patterns
     */
    public FakeRepoPerms(
        final String name,
        final Collection<PermissionItem> perms,
        final Collection<PathPattern> patterns
    ) {
        this(
            new HashMap<>(
                new MapOf<String, Pair<Collection<PermissionItem>, Collection<PathPattern>>>(
                    new MapEntry<>(name, new ImmutablePair<>(perms, patterns))
                )
            )
        );
    }

    /**
     * Ctor.
     * @param names Repository names
     */
    public FakeRepoPerms(final String... names) {
        this(Stream.of(names).collect(
            Collectors.toMap(
                name -> name,
                ignored -> new ImmutablePair<>(Collections.emptyList(), Collections.emptyList())
            )
        ));
    }

    /**
     * Ctor.
     */
    public FakeRepoPerms() {
        this(Collections.emptyMap());
    }

    @Override
    public CompletionStage<List<String>> repositories() {
        return CompletableFuture.completedFuture(new ArrayList<>(this.repos.keySet()));
    }

    @Override
    public CompletionStage<Void> remove(final String repo) {
        this.repos.remove(repo);
        return CompletableFuture.allOf();
    }

    @Override
    public CompletionStage<Void> update(
        final String repo, final Collection<PermissionItem> permissions,
        final Collection<PathPattern> patterns
    ) {
        this.repos.put(repo, new ImmutablePair<>(permissions, patterns));
        return CompletableFuture.allOf();
    }

    @Override
    public CompletionStage<Collection<PermissionItem>> permissions(final String repo) {
        return CompletableFuture.completedFuture(this.repos.get(repo).getKey());
    }

    @Override
    public CompletionStage<Collection<PathPattern>> patterns(final String repo) {
        return CompletableFuture.completedFuture(this.repos.get(repo).getValue());
    }

    /**
     * Permissions by repo name and user name.
     * @param repo Repo name
     * @param user User name
     * @return Permissions list
     */
    public Iterable<String> permissionsFor(final String repo, final String user) {
        return this.repos.get(repo).getKey().stream().filter(perm -> perm.username().equals(user))
            .findFirst().map(PermissionItem::permissions).orElseThrow();
    }

}
