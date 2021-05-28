/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeConfigFile;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ApiRepoGetSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ApiRepoGetSliceTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml"})
    void getRepoConfiguration(final String extension) {
        final String repo = "my-repo";
        this.storage.save(
            new Key.From(String.format("bob/%s%s", repo, extension)),
            new Content.From(yaml())
        ).join();
        MatcherAssert.assertThat(
            new ApiRepoGetSlice(new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasBody(yaml())
                    )
                ),
                new RequestLine(
                    RqMethod.GET, String.format("/api/repos/bob/%s", repo)
                )
            )
        );
    }

    @Test
    void returnsNotFoundWhenConfigIsAbsent() {
        MatcherAssert.assertThat(
            new ApiRepoGetSlice(new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/api/repos/bob/absent-repo")
            )
        );
    }

    private static byte[] yaml() {
        return Yaml.createYamlMappingBuilder().add(
            "repo", Yaml.createYamlMappingBuilder()
                .add("type", "docker")
                .add("storage", "path")
                .add(
                    "permissions", Yaml.createYamlMappingBuilder().add(
                        "john", Yaml.createYamlSequenceBuilder()
                            .add("read")
                            .add("write")
                            .build()
                    ).build()
                ).build()
        ).build().toString().getBytes();
    }

}
