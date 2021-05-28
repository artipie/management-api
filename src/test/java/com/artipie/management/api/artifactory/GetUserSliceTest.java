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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.json.Json;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetUserSlice}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GetUserSliceTest {

    @Test
    void returnsNotFoundOnInvalidRequest() {
        MatcherAssert.assertThat(
            new GetUserSlice(new FakeUsers()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/some/api/david")
            )
        );
    }

    @Test
    void returnsNotFoundIfUserIsNotFoundInCredentials() {
        MatcherAssert.assertThat(
            new GetUserSlice(new FakeUsers()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/api/security/users/josh")
            )
        );
    }

    @Test
    void returnsJsonFoundIfUserFound() {
        final String username = "jerry";
        MatcherAssert.assertThat(
            new GetUserSlice(new FakeUsers(username)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        Json.createObjectBuilder()
                            .add("name", username)
                            .add(
                                "email",
                                String.format("%s@artipie.com", username)
                            )
                            .add("lastLoggedIn", "2020-01-01T01:01:01.000+01:00")
                            .add("realm", "Internal")
                            .add("groups", Json.createArrayBuilder(Collections.emptyList()).build())
                            .build().toString(),
                        StandardCharsets.UTF_8
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/security/users/%s", username))
            )
        );
    }

    @Test
    void returnsJsonWithGroupsWhenFound() {
        final String username = "mark";
        final Set<String> groups = new SetOf<>("readers", "newbies");
        MatcherAssert.assertThat(
            new GetUserSlice(new FakeUsers(new Users.User(username, Optional.empty(), groups))),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        Json.createObjectBuilder()
                            .add("name", username)
                            .add(
                                "email",
                                String.format("%s@artipie.com", username)
                            )
                            .add("lastLoggedIn", "2020-01-01T01:01:01.000+01:00")
                            .add("realm", "Internal")
                            .add("groups", Json.createArrayBuilder(groups).build())
                            .build().toString(),
                        StandardCharsets.UTF_8
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/security/users/%s", username))
            )
        );
    }

}
