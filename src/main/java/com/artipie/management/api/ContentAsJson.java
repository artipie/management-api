/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.asto.ext.ContentAs;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.reactivestreams.Publisher;

/**
 * Rx publisher transformer to json.
 * @since 0.1
 */
public final class ContentAsJson
    implements Function<Single<? extends Publisher<ByteBuffer>>, Single<? extends JsonObject>> {

    @Override
    public Single<? extends JsonObject> apply(final Single<? extends Publisher<ByteBuffer>> pub) {
        return new ContentAs<>(
            bytes -> {
                try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
                    return reader.readObject();
                }
            }
        ).apply(pub);
    }
}
