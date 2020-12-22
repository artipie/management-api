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
package com.artipie.management.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeConfigFile;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ApiRepoUpdateSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Disabled
final class ApiRepoUpdateSliceTest {

    /**
     * User.
     */
    private static final String USER = "bob";

    /**
     * Repo.
     */
    private static final String REPO = "my-repo";

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void createsRepoConfiguration() throws IOException {
        MatcherAssert.assertThat(
            "Returns FOUND",
            new ApiRepoUpdateSlice(new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FOUND),
                new RequestLine(
                    RqMethod.POST, String.format("/api/repos/%s", ApiRepoUpdateSliceTest.USER)
                ),
                new Headers.From(
                    "Location", String.format(
                        "/dashboard/%s", ApiRepoUpdateSliceTest.userRepo()
                    )
                ),
                new Content.From(body(ApiRepoUpdateSliceTest.REPO, "maven"))
            )
        );
        MatcherAssert.assertThat(
            "Config file exist",
            this.storage.exists(
                new Key.From(String.format("%s.yaml", ApiRepoUpdateSliceTest.userRepo()))
            ).join(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml"})
    void updatesRepoConfiguration(final String extension) throws IOException {
        final String oldtype = "pypi";
        final String type = "docker";
        this.storage.save(
            new Key.From(String.format("%s%s", ApiRepoUpdateSliceTest.userRepo(), extension)),
            new Content.From(yaml(oldtype).getBytes())
        ).join();
        MatcherAssert.assertThat(
            "Returns FOUND",
            new ApiRepoUpdateSlice(new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FOUND),
                new RequestLine(
                    RqMethod.POST,
                    String.format("/api/repos/%s", ApiRepoUpdateSliceTest.USER)
                ),
                new Headers.From(
                    "Location", String.format(
                        "/dashboard/%s", ApiRepoUpdateSliceTest.userRepo()
                )
                ),
                new Content.From(body(ApiRepoUpdateSliceTest.REPO, type))
            )
        );
        MatcherAssert.assertThat(
            "Config file is updated",
            Yaml.createYamlInput(
                new PublisherAs(
                    this.storage.value(
                        new Key.From(String.format("%s.yaml", ApiRepoUpdateSliceTest.userRepo()))
                    ).join()
                ).asciiString()
                .toCompletableFuture().join()
            ).readYamlMapping()
            .yamlMapping("repo")
            .string("type"),
            new IsEqual<>(type)
        );
    }

    private static byte[] body(final String reponame, final String type) {
        return String.format("repo=%s;config=%s", reponame, yaml(type)).getBytes();
    }

    private static String userRepo() {
        return String.format("%s/%s", ApiRepoUpdateSliceTest.USER, ApiRepoUpdateSliceTest.REPO);
    }

    private static String yaml(final String type) {
        return Yaml.createYamlMappingBuilder().add(
            "repo", Yaml.createYamlMappingBuilder()
                .add("type", type)
                .add("storage", "path")
                .add(
                    "permissions", Yaml.createYamlMappingBuilder().add(
                        "john", Yaml.createYamlSequenceBuilder()
                            .add("read")
                            .add("write")
                            .build()
                    ).build()
                ).build()
        ).build().toString();
    }

}
