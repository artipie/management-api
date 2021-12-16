/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeConfigFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ApiRepoPostRtSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ApiRepoPostRtSliceTest {
    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ValueSource(strings = {"action=unknown", "no_key=ignore"})
    void returnBadRequest(final String body) {
        MatcherAssert.assertThat(
            new ApiRepoPostRtSlice(this.storage, new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.POST, "/api/repos/user"),
                Headers.EMPTY,
                new Content.From(body.getBytes(StandardCharsets.US_ASCII))
            )
        );
    }

    @Test
    void returnFoundForUpdate() {
        final String body = String.format(
            "action=update&config=%s&repo=bin",
            URLEncoder.encode(
                ApiRepoUpdateSliceTest.yaml("bin", false),
                StandardCharsets.UTF_8
            )
        );
        MatcherAssert.assertThat(
            new ApiRepoPostRtSlice(this.storage, new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FOUND),
                new RequestLine(RqMethod.POST, "/api/repos/user"),
                Headers.EMPTY,
                new Content.From(body.getBytes(StandardCharsets.UTF_8))
            )
        );
    }

    @Test
    void returnFoundOkForDelete() {
        this.storage.save(new Key.From("user", "bin.yaml"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new ApiRepoPostRtSlice(this.storage, new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.POST, "/api/repos/user"),
                Headers.EMPTY,
                new Content.From("action=delete&repo=bin".getBytes(StandardCharsets.UTF_8))
            )
        );
    }
}
