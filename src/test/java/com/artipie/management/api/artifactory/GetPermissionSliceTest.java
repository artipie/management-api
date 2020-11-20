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
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeRepoPerms;
import com.artipie.management.RepoPermissions;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetPermissionSlice}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GetPermissionSliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsBadRequestOnInvalidRequest() {
        MatcherAssert.assertThat(
            new GetPermissionSlice(this.storage, new FakeRepoPerms()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.GET, "/some/api/permissions/maven")
            )
        );
    }

    @Test
    void returnsNotFoundIfRepoDoesNotExists() {
        MatcherAssert.assertThat(
            new GetPermissionSlice(this.storage, new FakeRepoPerms()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/api/security/permissions/pypi")
            )
        );
    }

    @Test
    void returnsEmptyUsersIfNoPermissionsSet() {
        final String repo = "docker";
        this.storage.save(new Key.From(String.format("%s.yaml", repo)), Content.EMPTY);
        MatcherAssert.assertThat(
            new GetPermissionSlice(this.storage, new FakeRepoPerms(repo)),
            new SliceHasResponse(
                new RsHasBody(
                    this.response(
                        repo,
                        Json.createObjectBuilder().build(),
                        Json.createObjectBuilder().build(),
                        "**"
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/security/permissions/%s", repo))
            )
        );
    }

    @Test
    void returnsUsersAndPermissionsList() {
        final String repo = "maven";
        final String john = "john";
        final String readers = "readers";
        final String team = "team";
        final String mark = "mark";
        this.storage.save(new Key.From(String.format("%s.yaml", repo)), Content.EMPTY);
        MatcherAssert.assertThat(
            new GetPermissionSlice(
                this.storage,
                new FakeRepoPerms(
                    repo,
                    List.of(
                        new RepoPermissions.PermissionItem(john, new ListOf<>("read", "write")),
                        new RepoPermissions.PermissionItem(mark, "*"),
                        new RepoPermissions.PermissionItem(String.format("/%s", readers), "read"),
                        new RepoPermissions.PermissionItem(
                            String.format("/%s", team), new ListOf<>("write", "delete")
                        )
                    ),
                    Collections.singletonList(new RepoPermissions.PathPattern("**/*"))
                )
            ),
            new SliceHasResponse(
                new RsHasBody(
                    this.response(
                        repo,
                        Json.createObjectBuilder()
                            .add(john, Json.createArrayBuilder().add("r").add("w").build())
                            .add(mark, Json.createArrayBuilder().add("m").build())
                            .build(),
                        Json.createObjectBuilder()
                            .add(readers, Json.createArrayBuilder().add("r").build())
                            .add(team, Json.createArrayBuilder().add("w").add("d").build())
                            .build(),
                        "**/*"
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/security/permissions/%s", repo))
            )
        );
    }

    private byte[] response(final String repo, final JsonObject users, final JsonObject groups,
        final String pattern) {
        return Json.createObjectBuilder()
            .add("includesPattern", pattern)
            .add("repositories", Json.createArrayBuilder().add(repo).build())
            .add(
                "principals",
                Json.createObjectBuilder().add("users", users).add("groups", groups)
            ).build().toString().getBytes(StandardCharsets.UTF_8);
    }
}
