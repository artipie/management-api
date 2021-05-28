/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.ext.ContentAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Location;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.management.Users;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.reactivestreams.Publisher;

/**
 * Change user password slice.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class ApiChangeUserPassword implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/api/users/(?<user>[^/.]+)/password");

    /**
     * Artipie users.
     */
    private final Users users;

    /**
     * New API change password.
     * @param users Artipie users
     */
    public ApiChangeUserPassword(final Users users) {
        this.users = users;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String user = matcher.group("user");
        return new AsyncResponse(
            Single.just(body).to(ContentAs.STRING).map(
                encoded -> URLEncodedUtils.parse(encoded, StandardCharsets.UTF_8)
                    .stream()
                    .filter(pair -> pair.getName().equals("password"))
                    .map(NameValuePair::getValue)
                    .findFirst().orElseThrow()
            ).flatMapCompletable(
                pass -> Completable.fromFuture(
                    this.users.add(
                        new Users.User(user, Optional.empty()),
                        DigestUtils.sha256Hex(pass), Users.PasswordFormat.SHA256
                    ).toCompletableFuture()
                )
            ).toSingleDefault(
                new RsWithHeaders(
                    new RsWithStatus(RsStatus.FOUND),
                    new Location(String.format("/dashboard/%s", user))
                )
            ).to(SingleInterop.get())
        );
    }
}
