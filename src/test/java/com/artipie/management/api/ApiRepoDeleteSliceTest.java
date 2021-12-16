/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeConfigFile;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ApiRepoDeleteSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ApiRepoDeleteSliceTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(strings = {"bin.yaml", "bin.yml"})
    void deletesRepoConfigAndFilesInRepo(final String config) {
        final String user = "bob";
        final Key left = new Key.From(user, "another_repo", "exist.jar");
        final Key repo = new Key.From(user, "bin");
        this.storage.save(new Key.From(repo, "one.txt"), Content.EMPTY).join();
        this.storage.save(new Key.From(repo, "two.txt"), Content.EMPTY).join();
        this.storage.save(left, Content.EMPTY).join();
        new TestResource("bin.yml").saveTo(this.storage, new Key.From(user, config));
        MatcherAssert.assertThat(
            "Repo config was not removed",
            new ApiRepoDeleteSlice(this.storage, new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(
                        new Headers.From(
                        "Location", String.format("/dashboard/%s", user)
                        )
                    )
                ),
                new RequestLine(
                    RqMethod.POST, String.format("/api/repos/%s", user)
                ),
                Headers.EMPTY,
                ApiRepoDeleteSliceTest.body("bin")
            )
        );
        MatcherAssert.assertThat(
            "Config file was not removed",
            this.storage.list(repo).join(),
            new IsEmptyCollection<>()
        );
        MatcherAssert.assertThat(
            "File from another repo does not exist",
            this.storage.exists(left).join(),
            new IsEqual<>(true)
        );
    }

    private static Content body(final String reponame) {
        return new Content.From(
            String.format("repo=%s&action=delete", reponame).getBytes(StandardCharsets.UTF_8)
        );
    }

}
