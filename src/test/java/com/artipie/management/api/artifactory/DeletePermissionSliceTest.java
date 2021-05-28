/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

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
import com.artipie.management.FakeRepoPerms;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link DeletePermissionSlice}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class DeletePermissionSliceTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsBadRequestOnInvalidRequest() {
        MatcherAssert.assertThat(
            new DeletePermissionSlice(new FakeRepoPerms(), new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/some/api/permissions/pypi")
            )
        );
    }

    @Test
    void returnsNotFoundIfRepositoryDoesNotExists() {
        MatcherAssert.assertThat(
            new DeletePermissionSlice(new FakeRepoPerms(), new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/api/security/permissions/pypi")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml"})
    void deletesRepoPermissions(final String extension) {
        final String repo = "docker";
        final Key.From key = new Key.From(String.format("%s%s", repo, extension));
        this.storage.save(key, Content.EMPTY).join();
        final FakeRepoPerms permissions = new FakeRepoPerms(repo);
        MatcherAssert.assertThat(
            "Returns 200 OK",
            new DeletePermissionSlice(permissions, new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        String.format(
                            "Permission Target '%s' has been removed successfully.", repo
                        ).getBytes(StandardCharsets.UTF_8)
                    )
                ),
                new RequestLine(
                    RqMethod.DELETE, String.format("/api/security/permissions/%s", repo)
                )
            )
        );
        MatcherAssert.assertThat(
            "Removes permissions",
            permissions.repositories().toCompletableFuture().join(),
            new IsEmptyCollection<>()
        );
    }

}
