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
package com.artipie.management.api.artifactory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeConfigFile;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link CreateRepoSlice}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CreateRepoSliceTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsOkAndSavesYamlIfJsonIsValidWithUser() {
        MatcherAssert.assertThat(
            "Returns 200 OK",
            new CreateRepoSlice(this.storage, new FakeConfigFile(this.storage)).response(
                new RequestLine("PUT", "/api/repositories/username/my_repo").toString(),
                Collections.emptyList(),
                this.jsonBody()
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "Saves yaml to storage",
            this.storage.exists(new Key.From("username/my_repo.yaml")).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsOkAndSavesYamlIfJsonIsValid() {
        MatcherAssert.assertThat(
            "Returns 200 OK",
            new CreateRepoSlice(this.storage, new FakeConfigFile(this.storage)).response(
                new RequestLine("PUT", "/api/repositories/my_repo").toString(),
                Collections.emptyList(),
                this.jsonBody()
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "Saves yaml to storage",
            this.storage.exists(new Key.From("my_repo.yaml")).join(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml"})
    void returnsBadRequestIfRepoYamlOrYmlAlreadyExists(final String extension) {
        this.storage.save(
            new Key.From(String.format("my_repo%s", extension)),
            new Content.From(new byte[]{})
        );
        MatcherAssert.assertThat(
            new CreateRepoSlice(this.storage, new FakeConfigFile(this.storage)).response(
                new RequestLine("PUT", "/api/repositories/my_repo").toString(),
                Collections.emptyList(),
                this.jsonBody()
            ),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
    }

    @Test
    void returnsBadRequestIfJsonIsNotValid() {
        MatcherAssert.assertThat(
            new CreateRepoSlice(this.storage, new FakeConfigFile(this.storage)).response(
                new RequestLine("PUT", "/api/repositories/my_repo").toString(),
                Collections.emptyList(),
                Flowable.fromArray(
                    ByteBuffer.wrap(
                        Json.createObjectBuilder()
                            .add("some", "key")
                            .build().toString().getBytes()
                    )
                )
            ),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
    }

    private Flowable<ByteBuffer> jsonBody() {
        return Flowable.fromArray(
            ByteBuffer.wrap(
                Json.createObjectBuilder()
                    .add("key", "my_repo")
                    .add("rclass", "local")
                    .add("packageType", "docker")
                    .add("dockerApiVersion", "V2")
                    .build().toString().getBytes()
            )
        );
    }

}
