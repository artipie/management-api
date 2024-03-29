/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.common.RsJson;
import com.artipie.management.ConfigFiles;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.reactivestreams.Publisher;

/**
 * Repo list API.
 * @since 0.1
 */
@SuppressWarnings("PMD.ClassDataAbstractionCouplingCheck")
public final class ApiRepoListSlice implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/api/repos/(?<user>[^/.]+)");

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Config file to support `yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * New repo list API.
     * @param storage Artipie settings storage
     * @param configfile Config file to support `yaml` and `.yml` extensions
     */
    public ApiRepoListSlice(final Storage storage, final ConfigFiles configfile) {
        this.storage = storage;
        this.configfile = configfile;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String user = matcher.group("user");
        final RxStorage rxstorage = new RxStorageWrapper(this.storage);
        return new AsyncResponse(
            rxstorage.list(new Key.From(user))
            .map(
                repos -> {
                    final JsonObjectBuilder json = Json.createObjectBuilder()
                        .add("user", user);
                    final JsonArrayBuilder arr = Json.createArrayBuilder();
                    for (final Key key : repos) {
                        if (this.configfile.isYamlOrYml(key)) {
                            arr.add(this.configfile.name(key));
                        }
                    }
                    json.add("repositories", arr);
                    return json;
                }
            ).map(builder -> new RsJson(builder::build))
        );
    }
}
