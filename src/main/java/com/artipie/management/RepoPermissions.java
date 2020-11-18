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

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.management.api.ContentAsYaml;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.cactoos.set.SetOf;

/**
 * Repository permissions settings.
 * @since 0.1
 */
public interface RepoPermissions {

    /**
     * Artipie repositories list.
     * @return Repository names list
     */
    CompletionStage<List<String>> repositories();

    /**
     * Deletes all permissions for repository.
     * @param repo Repository name
     * @return Completion remove action
     */
    CompletionStage<Void> remove(String repo);

    /**
     * Adds or updates repository permissions.
     * @param repo Repository name
     * @param permissions Permissions list
     * @param patterns Included path patterns
     * @return Completion action
     */
    CompletionStage<Void> update(
        String repo,
        Collection<PermissionItem> permissions,
        Collection<PathPattern> patterns
    );

    /**
     * Get repository permissions settings, returns users permissions list.
     * @param repo Repository name
     * @return Completion action with map with users and permissions
     */
    CompletionStage<Collection<PermissionItem>> permissions(String repo);

    /**
     * Read included path patterns.
     *
     * @param repo Repository name
     * @return Collection of included path patterns
     */
    CompletionStage<Collection<PathPattern>> patterns(String repo);

    /**
     * {@link RepoPermissions} from Artipie settings.
     * @since 0.1
     * @todo #2:30min Get rid of this implementation: this implementation should stay in artipie,
     *  now it is used in several slices and tests, refactor slices to accept interface as a ctor
     *  parameter and create fake implementation for test purposes.
     */
    final class FromSettings implements RepoPermissions {

        /**
         * Permissions section name.
         */
        private static final String PERMISSIONS = "permissions";

        /**
         * Permissions include patterns section in YAML settings.
         */
        private static final String INCLUDE_PATTERNS = "permissions_include_patterns";

        /**
         * Repo section in yaml settings.
         */
        private static final String REPO = "repo";

        /**
         * Artipie settings storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         * @param storage Artipie settings storage
         */
        public FromSettings(final Storage storage) {
            this.storage = storage;
        }

        @Override
        public CompletionStage<List<String>> repositories() {
            return this.storage.list(Key.ROOT)
                .thenApply(
                    list -> list.stream()
                        .map(Key::string)
                        .filter(key -> key.contains("yaml"))
                        .map(key -> key.replace(".yaml", ""))
                        .collect(Collectors.toList())
            );
        }

        @Override
        public CompletionStage<Void> remove(final String repo) {
            final Key key = FromSettings.repoSettingsKey(repo);
            return this.repo(key).thenApply(
                mapping -> Yaml.createYamlMappingBuilder()
                    .add(FromSettings.REPO, FromSettings.copyRepoSection(mapping).build()).build()
            ).thenCompose(
                result -> this.saveSettings(key, result)
            );
        }

        @Override
        public CompletionStage<Void> update(
            final String repo,
            final Collection<PermissionItem> permissions,
            final Collection<PathPattern> patterns) {
            final Key key = FromSettings.repoSettingsKey(repo);
            return this.repo(key).thenApply(
                mapping -> {
                    YamlMappingBuilder res = FromSettings.copyRepoSection(mapping);
                    YamlMappingBuilder perms = Yaml.createYamlMappingBuilder();
                    if (!permissions.isEmpty()) {
                        for (final PermissionItem item : permissions) {
                            perms = perms.add(item.name, item.yaml().build());
                        }
                        res = res.add(FromSettings.PERMISSIONS, perms.build());
                    }
                    if (!patterns.isEmpty()) {
                        YamlSequenceBuilder builder = Yaml.createYamlSequenceBuilder();
                        for (final PathPattern pattern : patterns) {
                            builder = builder.add(pattern.string());
                        }
                        res = res.add(
                            FromSettings.INCLUDE_PATTERNS, builder.build()
                        );
                    }
                    return Yaml.createYamlMappingBuilder()
                        .add(FromSettings.REPO, res.build()).build();
                }
            ).thenCompose(
                result -> this.saveSettings(key, result)
            );
        }

        @Override
        public CompletionStage<Collection<PermissionItem>> permissions(final String repo) {
            return this.repo(FromSettings.repoSettingsKey(repo)).thenApply(
                yaml -> Optional.ofNullable(yaml.yamlMapping(FromSettings.PERMISSIONS))
            ).thenApply(
                yaml -> yaml.map(
                    perms -> perms.keys().stream().map(
                        node -> new PermissionItem(
                            node.asScalar().value(),
                            perms.yamlSequence(node.asScalar().value()).values().stream()
                            .map(item -> item.asScalar().value())
                                .collect(Collectors.toList())
                        )
                    ).collect(Collectors.toList())
                ).orElse(Collections.emptyList())
            );
        }

        @Override
        public CompletionStage<Collection<PathPattern>> patterns(final String repo) {
            return this.repo(FromSettings.repoSettingsKey(repo)).thenApply(
                yaml -> Optional.ofNullable(yaml.yamlSequence(FromSettings.INCLUDE_PATTERNS))
            ).thenApply(
                yaml -> yaml.map(
                    seq -> seq.values().stream()
                        .map(value -> value.asScalar().value())
                        .map(PathPattern::new)
                        .collect(Collectors.toList())
                ).orElse(Collections.emptyList())
            );
        }

        /**
         * Repo sections from settings.
         * @param key Repo settings key
         * @return Completion action with yaml repo section
         */
        private CompletionStage<YamlMapping> repo(final Key key) {
            return new RxStorageWrapper(this.storage)
                .value(key)
                .to(new ContentAsYaml())
                .map(yaml -> yaml.yamlMapping(FromSettings.REPO))
                .to(SingleInterop.get());
        }

        /**
         * Saves changed settings to storage.
         * @param key Key to save storage item by
         * @param result Yaml result
         * @return Completable operation
         */
        private CompletableFuture<Void> saveSettings(final Key key, final YamlMapping result) {
            return this.storage.save(
                key, new Content.From(result.toString().getBytes(StandardCharsets.UTF_8))
            );
        }

        /**
         * Repo settings key.
         * @param repo Repo name
         * @return Settings key
         */
        private static Key repoSettingsKey(final String repo) {
            return new Key.From(String.format("%s.yaml", repo));
        }

        /**
         * Copy `repo` section without permissions from existing yaml setting.
         * @param mapping Repo section mapping
         * @return Setting without permissions
         */
        private static YamlMappingBuilder copyRepoSection(final YamlMapping mapping) {
            YamlMappingBuilder res = Yaml.createYamlMappingBuilder();
            final List<YamlNode> keep = mapping.keys().stream()
                .filter(
                    node -> !new SetOf<>(
                        FromSettings.PERMISSIONS, FromSettings.INCLUDE_PATTERNS
                    ).contains(node.asScalar().value())
                )
                .collect(Collectors.toList());
            for (final YamlNode node : keep) {
                res = res.add(node, mapping.value(node));
            }
            return res;
        }
    }

    /**
     * User permission item.
     * @since 0.1
     */
    final class PermissionItem {

        /**
         * Username.
         */
        private final String name;

        /**
         * Permissions list.
         */
        private final List<String> perms;

        /**
         * Ctor.
         * @param name Username
         * @param permissions Permissions
         */
        public PermissionItem(final String name, final List<String> permissions) {
            this.name = name;
            this.perms = permissions;
        }

        /**
         * Ctor.
         * @param name Username
         * @param permission Permission
         */
        public PermissionItem(final String name, final String permission) {
            this(name, Collections.singletonList(permission));
        }

        /**
         * Get username.
         * @return String username
         */
        public String username() {
            return this.name;
        }

        /**
         * Get permissions list.
         * @return List of permissions
         */
        public List<String> permissions() {
            return this.perms;
        }

        @Override
        public boolean equals(final Object other) {
            final boolean res;
            if (this == other) {
                res = true;
            } else if (other == null || getClass() != other.getClass()) {
                res = false;
            } else {
                final PermissionItem that = (PermissionItem) other;
                res = Objects.equals(this.name, that.name)
                    && Objects.equals(this.perms, that.perms);
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.perms);
        }

        /**
         * Permissions yaml sequence.
         * @return Yaml permissions sequence builder
         */
        public YamlSequenceBuilder yaml() {
            YamlSequenceBuilder res = Yaml.createYamlSequenceBuilder();
            for (final String item : this.perms) {
                res = res.add(item);
            }
            return res;
        }
    }

    /**
     * Represents path pattern used to check permissions inside repository.
     * Specified by expression in Ant-like syntax, example: "/path/**&#47;*.txt"
     *
     * @since 0.1
     */
    final class PathPattern {

        /**
         * Pattern expression.
         */
        private final String expr;

        /**
         * Ctor.
         *
         * @param expr Pattern expression.
         */
        public PathPattern(final String expr) {
            this.expr = expr;
        }

        /**
         * Get pattern expression.
         *
         * @return Pattern expression string.
         */
        public String string() {
            return this.expr;
        }

        /**
         * Check that pattern is valid.
         *
         * @param repo Repository name.
         * @return True if valid, false - otherwise
         */
        public boolean valid(final String repo) {
            return this.expr.matches(String.format("(%s/)?(\\*\\*)*(/\\*)?", repo));
        }
    }
}
