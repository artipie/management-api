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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ApiRepoGetSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ApiRepoGetSliceTest {

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml"})
    void getRepoConfiguration(final String extension) {
        final Storage storage = new InMemoryStorage();
        final String repo = "my-repo";
        storage.save(
            new Key.From(String.format("bob/%s%s", repo, extension)),
            new Content.From(yaml().getBytes())
        ).join();
        MatcherAssert.assertThat(
            new ApiRepoGetSlice(new FakeConfigFile(storage)),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasBody(yaml().getBytes())
                    )
                ),
                new RequestLine(
                    RqMethod.GET, String.format("/api/repos/bob/%s", repo)
                )
            )
        );
    }

    private static String yaml() {
        return Yaml.createYamlMappingBuilder().add(
            "repo", Yaml.createYamlMappingBuilder()
                .add("type", "docke r")
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
