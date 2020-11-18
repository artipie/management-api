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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Fake {@link RepoPermissions} implementation.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class FakeRepoPerms implements RepoPermissions {

    /**
     * Repositories list.
     */
    private final List<String> repos;

    /**
     * Ctor.
     * @param repos Repositories list
     */
    public FakeRepoPerms(final List<String> repos) {
        this.repos = repos;
    }

    /**
     * Ctor.
     * @param name Repository name
     */
    public FakeRepoPerms(final String name) {
        this(Stream.of(name).collect(Collectors.toList()));
    }

    /**
     * Ctor.
     */
    public FakeRepoPerms() {
        this(Collections.emptyList());
    }

    @Override
    public CompletionStage<List<String>> repositories() {
        return CompletableFuture.completedFuture(this.repos);
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
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public CompletionStage<Collection<PermissionItem>> permissions(final String repo) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public CompletionStage<Collection<PathPattern>> patterns(final String repo) {
        throw new NotImplementedException("Not implemented yet");
    }
}
