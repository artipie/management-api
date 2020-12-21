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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Supporting several config files extensions (e.g. `.yaml` and `.yml`).
 * Files with two different extensions are interpreted in the same way.
 * For example, if `name.yaml` is searched in the storage then
 * files `name.yaml` and `name.yml` are searched.
 * @since 0.4
 */
    public interface ConfigFile {

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
