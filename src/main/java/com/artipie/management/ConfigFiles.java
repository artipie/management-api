/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Supporting several config files extensions (e.g. `.yaml` and `.yml`).
 * Files with two different extensions are interpreted in the same way.
 * For example, if `name.yaml` is searched in the storage then
 * files `name.yaml` and `name.yml` are searched.
 * @since 0.4
 */
public interface ConfigFiles {

    /**
     * Does specified config file exist in the storage?
     * @param filename Filename
     * @return True if a file with either of the two extensions exists, false otherwise.
     */
    CompletionStage<Boolean> exists(Key filename);

    /**
     * Obtains contents of the config file from the storage.
     * @param filename Filename
     * @return Content of the config file.
     */
    CompletionStage<Content> value(Key filename);

    /**
     * Removes value from storage. Fails if value does not exist.
     * @param filename Filename
     * @return Result of completion.
     */
    CompletionStage<Void> delete(Key filename);

    /**
     * Saves the bytes to the specified key.
     *
     * @param key The key
     * @param content Bytes to save
     * @return Completion or error signal.
     */
    CompletableFuture<Void> save(Key key, Content content);

    /**
     * Filename.
     * @param filename Filename
     * @return Filename without extension.
     */
    String name(Key filename);

    /**
     * Extension.
     * @param filename Filename
     * @return Extension if present, empty otherwise.
     */
    Optional<String> extension(Key filename);

    /**
     * Is `yaml` or `yml` file?
     * @param filename Filename
     * @return True if is the file with `yaml` or `yml` extension, false otherwise.
     */
    boolean isYamlOrYml(Key filename);

}
