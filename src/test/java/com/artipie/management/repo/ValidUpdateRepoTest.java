/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.ArtipieException;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link UpdateRepo.Valid}.
 * @since 0.6
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UnusedPrivateMethod"})
final class ValidUpdateRepoTest {
    @ParameterizedTest
    @MethodSource("badConfigs")
    void throwsWhenRequiredFieldAreAbsent(final YamlMapping raw) {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new UpdateRepo.Valid(raw).repo()
        );
    }

    @ParameterizedTest
    @MethodSource("validConfigs")
    void worksForValidConfigs(final YamlMapping valid) {
        final YamlMapping res = new UpdateRepo.Valid(valid).repo();
        for (final YamlNode node : valid.keys()) {
            MatcherAssert.assertThat(
                valid.value(node).equals(res.value(node)),
                new IsEqual<>(true)
            );
        }
    }

    private static Stream<Arguments> badConfigs() {
        return Stream.of(
            Arguments.of(
                Yaml.createYamlMappingBuilder().add("type", "bin").build()
            ),
            Arguments.of(
                Yaml.createYamlMappingBuilder().add("storage", "default").build()
            )
        );
    }

    private static Stream<Arguments> validConfigs() {
        return Stream.of(
            Arguments.of(
                Yaml.createYamlMappingBuilder().add("type", "bin")
                    .add("storage", "default").build()
            ),
            Arguments.of(
                Yaml.createYamlMappingBuilder().add("type", "maven")
                    .add("storage", "default")
                    .add("path", "some/path")
                    .build()
            ),
            Arguments.of(
                Yaml.createYamlMappingBuilder().add("type", "maven")
                    .add(
                        "storage", Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", "/var/path/data").build()
                    ).add(
                        "permissions", Yaml.createYamlMappingBuilder()
                            .add(
                                "john", Yaml.createYamlSequenceBuilder()
                                    .add("deploy")
                                    .add("delete").build()
                            ).build()
                    ).build()
            )
        );
    }
}
