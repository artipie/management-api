/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

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
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ApiRepoGetSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ApiRepoListSliceTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsRepoListForUser() {
        final String user = "bob";
        final String repo = "my-repo";
        final String pypi = "my-pypi";
        this.saveToStrg(user, repo, ".yaml");
        this.saveToStrg(user, pypi, ".yml");
        this.saveToStrg(user, "smth", ".txt");
        MatcherAssert.assertThat(
            new ApiRepoListSlice(this.storage, new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasBody(this.body(user, pypi, repo))
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/repos/%s", user))
            )
        );
    }

    @Test
    void returnsEmptyRepoListForUserWhenNoConfigs() {
        final String user = "alice";
        MatcherAssert.assertThat(
            new ApiRepoListSlice(this.storage, new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasBody(this.body(user))
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/repos/%s", user))
            )
        );
    }

    private void saveToStrg(final String user, final String repo, final String extension) {
        this.storage.save(
            new Key.From(String.format("%s/%s%s", user, repo, extension)),
            Content.EMPTY
        ).join();
    }

    private byte[] body(final String user, final String... repos) {
        final JsonObjectBuilder json = Json.createObjectBuilder()
            .add("user", user);
        final JsonArrayBuilder arr = Json.createArrayBuilder();
        for (final String repo : repos) {
            arr.add(
                new FakeConfigFile(this.storage)
                    .name(new Key.From(String.format("%s/%s", user, repo)))
            );
        }
        json.add("repositories", arr);
        return json.build().toString().getBytes();
    }
}
