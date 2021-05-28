/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeRepoPerms;
import com.artipie.management.RepoPermissions;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AddUpdatePermissionSlice}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AddUpdatePermissionSliceTest {

    @Test
    void returnsBadRequestOnInvalidRequest() {
        MatcherAssert.assertThat(
            new AddUpdatePermissionSlice(new FakeRepoPerms()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/some/api/permissions/maven")
            )
        );
    }

    @Test
    void updatesPermissionsAndPatterns() {
        final String repo = "maven";
        final FakeRepoPerms perms = new FakeRepoPerms(repo);
        MatcherAssert.assertThat(
            "Returns 200 OK",
            new AddUpdatePermissionSlice(perms),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.PUT, String.format("/api/security/permissions/%s", repo)),
                Headers.EMPTY,
                new Content.From(this.json(false).getBytes(StandardCharsets.UTF_8))
            )
        );
        MatcherAssert.assertThat(
            "Sets permissions for bob",
            perms.permissionsFor(repo, "bob"),
            Matchers.containsInAnyOrder("read", "write", "*")
        );
        MatcherAssert.assertThat(
            "Sets permissions for alice",
            perms.permissionsFor(repo, "alice"),
            Matchers.containsInAnyOrder("write", "read")
        );
        MatcherAssert.assertThat(
            "Sets permissions for john",
            perms.permissionsFor(repo, "john"),
            Matchers.containsInAnyOrder("*")
        );
        MatcherAssert.assertThat(
            "Sets patterns",
            perms.patterns(repo).toCompletableFuture().join().stream()
                .map(RepoPermissions.PathPattern::string).collect(Collectors.toList()),
            Matchers.contains("**", "maven/**")
        );
        MatcherAssert.assertThat(
            "Sets readers group",
            perms.permissionsFor(repo, "/readers"),
            Matchers.contains("read")
        );
        MatcherAssert.assertThat(
            "Sets dev-leads group",
            perms.permissionsFor(repo, "/dev-leads"),
            Matchers.contains("read", "write")
        );
    }

    @Test
    void validatesPatterns() {
        final String repo = "docker";
        MatcherAssert.assertThat(
            new AddUpdatePermissionSlice(new FakeRepoPerms()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, String.format("/api/security/permissions/%s", repo)),
                Headers.EMPTY,
                new Content.From(
                    String.join(
                        "\n",
                        "{",
                        " \"repo\": {",
                        "    \"include-patterns\": [\"some/path/**\u002F*.txt\"],",
                        "    \"repositories\": [\"local-repo\"],",
                        "    \"actions\": { \"users\" : { \"john\" : [\"admin\"] } }",
                        "  }",
                        "}"
                    ).getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }

    @Test
    void doesNotAddReadersGroupTwice() {
        final String repo = "maven";
        final FakeRepoPerms perms = new FakeRepoPerms(repo);
        MatcherAssert.assertThat(
            "Returns 200 OK",
            new AddUpdatePermissionSlice(perms),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.PUT, String.format("/api/security/permissions/%s", repo)),
                Headers.EMPTY,
                new Content.From(this.json(true).getBytes(StandardCharsets.UTF_8))
            )
        );
        MatcherAssert.assertThat(
            perms.permissions(repo).toCompletableFuture().join().stream()
                .filter(item -> item.username().equals("/readers")).count(),
            new IsEqual<>(1L)
        );
    }

    private String json(final boolean readers) {
        return String.join(
            "\n",
            "{",
            " \"name\": \"java-developers\",",
            " \"repo\": {",
            "    \"include-patterns\": [\"**\", \"maven/**\"],",
            "    \"exclude-patterns\": [\"\"],",
            "    \"repositories\": [\"local-rep1\", \"remote-rep1\", \"virtual-rep2\"],",
            "    \"actions\": {",
            "          \"users\" : {",
            "            \"bob\": [\"r\",\"write\",\"manage\"],",
            "            \"alice\" : [\"w\", \"read\"],",
            "            \"john\" : [\"admin\"]",
            "          },",
            "          \"groups\" : {",
            // @checkstyle AvoidInlineConditionalsCheck (1 line)
            readers ? "            \"readers\" : [\"read\"]," : "",
            "            \"dev-leads\" : [\"r\",\"write\"]",
            "          }",
            "    }",
            "  },",
            "\"build\": {",
            "    \"include-patterns\": [\"\"],",
            "    \"exclude-patterns\": [\"\"],",
            "    \"repositories\": [\"artifactory-build-info\"],",
            "    \"actions\": {",
            "          \"users\" : {",
            "            \"bob\": [\"read\",\"manage\"],",
            "            \"alice\" : [\"write\"]",
            "          },",
            "          \"groups\" : {",
            "            \"dev-leads\" : [\"manage\",\"read\",\"write\",\"annotate\",\"delete\"],",
            "            \"readers\" : [\"read\"]",
            "          }",
            "    }",
            "  }",
            "}"
        );
    }

}
