/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.management.ConfigFiles;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.reactivestreams.Publisher;

/**
 * Slice for routing {@code POST} requests by parsing content and checking
 * methods which are written in this content.
 * @since 0.5
 */
public final class ApiRepoPostRtSlice implements Slice {
    /**
     * URI path pattern.
     */
    static final Pattern PTN = Pattern.compile("/api/repos/(?<user>[^/.]+)");

    /**
     * Config file to support `.yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     * @param configfile Config file to support `.yaml` and `.yml` extensions
     */
    public ApiRepoPostRtSlice(final Storage storage, final ConfigFiles configfile) {
        this.storage = storage;
        this.configfile = configfile;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> content
    ) {
        return new AsyncResponse(
            new PublisherAs(content)
                .asciiString()
                .thenApply(
                    form -> {
                        final Response res;
                        final String action = ApiRepoPostRtSlice.value(form, "action");
                        if (Action.UPDATE.value().equals(action)) {
                            res = new ApiRepoUpdateSlice(this.configfile)
                                .response(
                                    line,
                                    headers,
                                    new Content.From(form.getBytes(StandardCharsets.US_ASCII))
                                );
                        } else if (Action.DELETE.value().equals(action)) {
                            res = new ApiRepoDeleteSlice(this.storage, this.configfile)
                                .response(
                                    line,
                                    headers,
                                    new Content.From(form.getBytes(StandardCharsets.US_ASCII))
                                );
                        } else {
                            res = StandardRs.NOT_FOUND;
                        }
                        return res;
                    }
                )
        );
    }

    /**
     * Type of action of request.
     * @since 0.5
     */
    private enum Action {
        /**
         * Update method.
         */
        UPDATE("update"),
        /**
         * Delete method.
         */
        DELETE("delete");

        /**
         * String value.
         */
        private final String name;

        /**
         * Ctor.
         * @param name String value
         */
        Action(final String name) {
            this.name = name;
        }

        /**
         * Method string.
         * @return Method string.
         */
        public String value() {
            return this.name;
        }
    }

    /**
     * Obtain value from payload, payload is a query string (url-encoded):
     * <code>name1=value%26and&name2=value2</code>.
     * @param payload Payload to parse
     * @param name Parameter name to obtain
     * @return Parameter value
     * @checkstyle StringLiteralsConcatenationCheck (10 lines)
     */
    private static String value(final String payload, final String name) {
        Optional<String> res = Optional.empty();
        final List<NameValuePair> params;
        params = URLEncodedUtils.parse(payload, StandardCharsets.US_ASCII);
        for (final NameValuePair param : params) {
            if (param.getName().equals(name)) {
                res = Optional.of(param.getValue());
            }
        }
        return res.orElseThrow(
            () -> new IllegalStateException(
                String.format("Failed to find value of '%s' in body", name)
            )
        );
    }
}
