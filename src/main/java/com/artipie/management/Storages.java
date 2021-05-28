/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management;

import com.artipie.asto.Storage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Storages.
 * @since 0.3
 */
public interface Storages {

    /**
     * Get storage by repo name.
     *
     * @param name Repo name.
     * @return Repo storage.
     */
    CompletionStage<Storage> repoStorage(String name);

    /**
     * Fake {@link Storages} implementation.
     * @since 0.3
     */
    final class Fake implements Storages {

        /**
         * Repo storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         * @param storage Storage
         */
        public Fake(final Storage storage) {
            this.storage = storage;
        }

        @Override
        public CompletionStage<Storage> repoStorage(final String name) {
            return CompletableFuture.completedFuture(this.storage);
        }
    }
}
