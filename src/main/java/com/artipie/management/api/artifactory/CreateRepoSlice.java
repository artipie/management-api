/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.management.ConfigFiles;
import com.artipie.management.api.ContentAsJson;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.JsonObject;
import org.reactivestreams.Publisher;

/**
 * Artifactory create repo API slice, it accepts json and create new docker repository by
 * creating corresponding YAML configuration.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CreateRepoSlice implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN =
        Pattern.compile("/api/repositories/(?<first>[^/.]+)(?<second>/[^/.]+)?/?");

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Config file to support `yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * Ctor.
     * @param storage Artipie settings storage
     * @param configfile Config file to support `yaml` and `.yml` extensions
     */
    public CreateRepoSlice(final Storage storage, final ConfigFiles configfile) {
        this.storage = storage;
        this.configfile = configfile;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        // @checkstyle ReturnCountCheck (20 lines)
        return new AsyncResponse(
            Single.just(body).to(new ContentAsJson()).flatMap(
                json -> Single.fromFuture(
                    valid(json).map(
                        name -> {
                            final Key key = CreateRepoSlice.yamlKey(line, name);
                            return this.configfile.exists(key)
                                .thenCompose(
                                    exists -> {
                                        final CompletionStage<Response> res;
                                        if (exists) {
                                            res = CompletableFuture.completedStage(
                                                new RsWithStatus(RsStatus.BAD_REQUEST)
                                            );
                                        } else {
                                            res = this.storage.save(
                                                key,
                                                new Content.From(
                                                    CreateRepoSlice.yaml().toString()
                                                        .getBytes(StandardCharsets.UTF_8)
                                                )
                                            ).thenApply(ignored -> new RsWithStatus(RsStatus.OK));
                                        }
                                        return res;
                                    }
                                ).toCompletableFuture();
                        }
                    ).orElse(
                        CompletableFuture.completedFuture(new RsWithStatus(RsStatus.BAD_REQUEST))
                    )
                )
            )
        );
    }

    /**
     * Checks if json is valid (contains new repo key and supported setting) and
     * return new repo name.
     * @param json Json to read repo name from
     * @return True if json is correct
     */
    private static Optional<String> valid(final JsonObject json) {
        final Optional<String> res;
        final String key = json.getString("key", "");
        if (!key.isEmpty() && "local".equals(json.getString("rclass", ""))
            && "docker".equals(json.getString("packageType", ""))
            && "V2".equals(json.getString("dockerApiVersion", ""))) {
            res = Optional.of(key);
        } else {
            res = Optional.empty();
        }
        return res;
    }

    /**
     * User from request line.
     * @param line Line
     * @param repo Repo name
     * @return Username if present
     */
    private static Key yamlKey(final String line, final String repo) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new UnsupportedOperationException("Unsupported request");
        }
        return new Key.From(
            String.format(
                "%s%s.yaml",
                Optional.ofNullable(
                    matcher.group("second")
                ).map(present -> String.format("%s/", matcher.group("first"))).orElse(""),
                repo
            )
        );
    }

    /**
     * Creates yaml mapping for the new repository.
     * @return Yaml configuration
     */
    private static YamlMapping yaml() {
        return Yaml.createYamlMappingBuilder().add(
            "repo",
            Yaml.createYamlMappingBuilder()
                .add("type", "docker")
                .add("storage", "default")
                .add(
                    "permissions",
                    Yaml.createYamlMappingBuilder()
                        .add("*", Yaml.createYamlSequenceBuilder().add("*").build())
                        .build()
                ).build()
        ).build();
    }

}
