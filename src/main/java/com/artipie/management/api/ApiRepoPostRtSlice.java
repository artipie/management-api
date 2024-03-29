/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.management.ConfigFiles;
import com.artipie.management.Storages;
import com.artipie.management.misc.ValueFromBody;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
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
     * Artipie storages.
     */
    private final Storages storages;

    /**
     * Ctor.
     * @param storages Artipie storages
     * @param configfile Config file to support `.yaml` and `.yml` extensions
     */
    public ApiRepoPostRtSlice(final Storages storages, final ConfigFiles configfile) {
        this.storages = storages;
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
                        final ValueFromBody vals = new ValueFromBody(form);
                        final Optional<String> meth = vals.byName("action");
                        if (meth.isPresent() && Action.UPDATE.value().equals(meth.get())) {
                            res = new ApiRepoUpdateSlice(this.configfile)
                                .response(
                                    line, headers,
                                    new Content.From(
                                        vals.payload().getBytes(StandardCharsets.UTF_8)
                                    )
                                );
                        } else if (meth.isPresent() && Action.DELETE.value().equals(meth.get())) {
                            res = new ApiRepoDeleteSlice(this.storages, this.configfile)
                                .response(
                                    line, headers,
                                    new Content.From(
                                        vals.payload().getBytes(StandardCharsets.UTF_8)
                                    )
                                );
                        } else {
                            res = new RsWithStatus(RsStatus.BAD_REQUEST);
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
}
