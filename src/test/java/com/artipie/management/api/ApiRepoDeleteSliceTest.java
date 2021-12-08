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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ApiRepoDeleteSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Disabled
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
    void deletesRepoConfig(final String config) {
        final String user = "bob";
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
                    RqMethod.POST, String.format("/api/repos/%s?method=delete", user)
                ),
                Headers.EMPTY,
                ApiRepoDeleteSliceTest.body("bin")
            )
        );
        MatcherAssert.assertThat(
            "Config file was not removed",
            this.storage.list(Key.ROOT).join(),
            new IsEmptyCollection<>()
        );
    }

    private static Content body(final String reponame) {
        return new Content.From(
            URLEncoder.encode(
                String.format("repo=%s", reponame),
                StandardCharsets.US_ASCII
            ).getBytes()
        );
    }

}
