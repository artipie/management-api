/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Fake {@link ConfigFiles} implementation.
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class FakeConfigFile implements ConfigFiles {

    /**
     * Pattern to divide `yaml` or `yml` filename into two groups: name and extension.
     */
    private static final Pattern PTN_YAML = Pattern.compile("(?<name>.*)(\\.yaml|\\.yml)$");

    /**
     * Pattern to divide all filenames into two groups: name and extension.
     */
    private static final Pattern PTN_ALL = Pattern.compile("(?<name>.+?)(?<extension>\\.[^.]*$|$)");

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public FakeConfigFile(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Boolean> exists(final Key filename) {
        final CompletionStage<Boolean> res;
        if (this.isYamlOrYml(filename) || this.extension(filename).isEmpty()) {
            final String name = this.name(filename);
            final Key yaml = new Key.From(String.format("%s.yaml", name));
            res = this.storage.exists(yaml)
                .thenCompose(
                    exist -> {
                        final CompletionStage<Boolean> result;
                        if (exist) {
                            result = CompletableFuture.completedFuture(true);
                        } else {
                            final Key yml = new Key.From(String.format("%s.yml", name));
                            result = this.storage.exists(yml);
                        }
                        return result;
                    }
                );
        } else {
            res = CompletableFuture.completedFuture(false);
        }
        return res;
    }

    @Override
    public CompletionStage<Content> value(final Key filename) {
        final String name = this.name(filename);
        final Key yaml = new Key.From(String.format("%s.yaml", name));
        return this.storage.exists(yaml)
            .thenCompose(
                exists -> {
                    final CompletionStage<Content> result;
                    if (exists) {
                        result = this.storage.value(yaml);
                    } else {
                        final Key yml = new Key.From(String.format("%s.yml", name));
                        result = this.storage.value(yml);
                    }
                    return result;
                }
            );
    }

    @Override
    public CompletionStage<Void> delete(final Key filename) {
        throw new NotImplementedException("not implemented yet");
    }

    @Override
    public CompletableFuture<Void> save(final Key filename, final Content content) {
        return this.storage.save(filename, content);
    }

    @Override
    public String name(final Key filename) {
        return FakeConfigFile.matcher("name", filename);
    }

    @Override
    public Optional<String> extension(final Key filename) {
        final Optional<String> extnsn;
        final String val = FakeConfigFile.matcher("extension", filename);
        if (val.isEmpty()) {
            extnsn = Optional.empty();
        } else {
            extnsn = Optional.of(val);
        }
        return extnsn;
    }

    @Override
    public boolean isYamlOrYml(final Key filename) {
        return PTN_YAML.matcher(filename.string()).matches();
    }

    /**
     * Matcher.
     * @param group Matcher group name
     * @param filename Filename
     * @return Value for specified group name.
     */
    private static String matcher(final String group, final Key filename) {
        final Matcher matcher = PTN_ALL.matcher(filename.string());
        if (!matcher.matches()) {
            throw new IllegalStateException(
                String.format("Failed to get name from string `%s`", filename.string())
            );
        }
        return matcher.group(group);
    }
}
