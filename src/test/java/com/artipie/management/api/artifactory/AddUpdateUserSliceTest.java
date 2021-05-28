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
import com.artipie.management.FakeUsers;
import com.artipie.management.Users;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.cactoos.list.ListOf;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link AddUpdateUserSlice}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class AddUpdateUserSliceTest {

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsBadRequestOnInvalidRequest(final RqMethod rqmeth) {
        MatcherAssert.assertThat(
            new AddUpdateUserSlice(new FakeUsers()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(rqmeth, "/some/api/david")
            )
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsBadRequestIfCredentialsAreEmpty(final RqMethod rqmeth) {
        MatcherAssert.assertThat(
            new AddUpdateUserSlice(new FakeUsers()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(rqmeth, "/api/security/users/empty"),
                Headers.EMPTY,
                new Content.From(
                    Json.createObjectBuilder().build()
                    .toString().getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsOkIfUserWasAddedToCredentials(final RqMethod rqmeth) {
        final String username = "mark";
        final String pswd = "abc123";
        final RequestLine rqline = new RequestLine(
            rqmeth,
            String.format("/api/security/users/%s", username)
        );
        final String ateam = "a-team";
        final String bteam = "b-team";
        final FakeUsers users = new FakeUsers("person");
        MatcherAssert.assertThat(
            "AddUpdateUserSlice response should be OK",
            new AddUpdateUserSlice(users).response(
                rqline.toString(), Headers.EMPTY,
                this.jsonBody(pswd, username, new ListOf<String>(ateam, bteam))
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "User with correct groups and email was added",
            users.user(username),
            new IsEqual<>(
                new Users.User(
                    username,
                    Optional.of(String.format("%s@example.com", username)),
                    new SetOf<String>("readers", ateam, bteam)
                )
            )
        );
        MatcherAssert.assertThat(
            "User with correct password should be added",
            users.pswd(username),
            new IsEqual<>(
                new FakeUsers.Password(DigestUtils.sha256Hex(pswd), Users.PasswordFormat.SHA256)
            )
        );
    }

    @ParameterizedTest
    @EnumSource(value = RqMethod.class, names = {"PUT", "POST"})
    void returnsOkIfUserWasUpdated(final RqMethod rqmeth) {
        final String username = "mike";
        final String newpswd = "qwerty123";
        final RequestLine rqline = new RequestLine(
            rqmeth,
            String.format("/api/security/users/%s", username)
        );
        final FakeUsers users = new FakeUsers(username);
        MatcherAssert.assertThat(
            "AddUpdateUserSlice response should be OK",
            new AddUpdateUserSlice(users).response(
                rqline.toString(), Headers.EMPTY,
                this.jsonBody(newpswd, username, Collections.emptyList())
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "Groups and email were updated",
            users.user(username),
            new IsEqual<>(
                new Users.User(
                    username,
                    Optional.of(String.format("%s@example.com", username)),
                    new SetOf<String>("readers")
                )
            )
        );
        MatcherAssert.assertThat(
            "Password was updated",
            users.pswd(username),
            new IsEqual<>(
                new FakeUsers.Password(DigestUtils.sha256Hex(newpswd), Users.PasswordFormat.SHA256)
            )
        );
    }

    private Flowable<ByteBuffer> jsonBody(final String pswd, final String name,
        final List<String> groups) {
        final JsonObjectBuilder json = Json.createObjectBuilder()
            .add("password", pswd)
            .add("email", String.format("%s@example.com", name));
        if (!groups.isEmpty()) {
            json.add("groups", Json.createArrayBuilder(groups).build());
        }
        return Flowable.fromArray(ByteBuffer.wrap(json.build().toString().getBytes()));
    }

}
