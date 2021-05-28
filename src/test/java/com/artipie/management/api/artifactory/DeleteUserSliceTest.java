/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeUsers;
import com.artipie.management.Users;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DeleteUserSlice}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class DeleteUserSliceTest {

    @Test
    void returnsBadRequestOnInvalidRequest() {
        MatcherAssert.assertThat(
            new DeleteUserSlice(new FakeUsers()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/some/api/david")
            )
        );
    }

    @Test
    void returnsNotFoundIfCredentialsAreEmpty() {
        MatcherAssert.assertThat(
            new DeleteUserSlice(new FakeUsers()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/api/security/users/empty")
            )
        );
    }

    @Test
    void returnsNotFoundIfUserIsNotFoundInCredentials() {
        MatcherAssert.assertThat(
            new DeleteUserSlice(new FakeUsers("john")),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/api/security/users/notfound")
            )
        );
    }

    @Test
    void returnsOkAndDeleteIfUserIsFoundInCredentials() {
        final Users users = new FakeUsers("jane");
        MatcherAssert.assertThat(
            "DeleteUserSlice response",
            new DeleteUserSlice(users),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        "User 'jane' has been removed successfully.", StandardCharsets.UTF_8
                    )
                ),
                new RequestLine(RqMethod.DELETE, "/api/security/users/jane")
            )
        );
        MatcherAssert.assertThat(
            "User should be deleted",
            users.list().toCompletableFuture().join(),
            new IsEmptyIterable<>()
        );
    }

}
