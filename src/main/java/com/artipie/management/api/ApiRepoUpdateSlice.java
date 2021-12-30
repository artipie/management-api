/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.management.ConfigFiles;
import com.artipie.management.misc.ValueFromBody;
import com.artipie.management.repo.UpdateRepo;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import org.cactoos.scalar.Unchecked;
import org.reactivestreams.Publisher;

/**
 * Patch repo API.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ApiRepoUpdateSlice implements Slice {
    /**
     * Config file to support `yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * New patch API.
     * @param configfile Config file to support `yaml` and `.yml` extensions
     */
    ApiRepoUpdateSlice(final ConfigFiles configfile) {
        this.configfile = configfile;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        final Matcher matcher = ApiRepoPostRtSlice.PTN.matcher(
            new RequestLineFrom(line).uri().getPath()
        );
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String user = matcher.group("user");
        // @checkstyle LineLengthCheck (500 lines)
        return new AsyncResponse(
            new PublisherAs(body).asciiString()
                .thenApply(ValueFromBody::new).thenCompose(
                    vals -> {
                        final YamlMapping repo = configsFromBody(vals).yamlMapping("repo");
                        if (repo == null) {
                            throw new ArtipieException("Repo section is required");
                        }
                        final String name = vals.byNameOrThrow("repo");
                        return this.configfile.save(
                            new Key.From(user, String.format("%s.yaml", name)),
                            new Content.From(
                                Yaml.createYamlMappingBuilder()
                                    .add("repo", new UpdateRepo.Valid(repo).repo())
                                    .build().toString().getBytes(StandardCharsets.UTF_8)
                            )
                        ).thenApply(nothing -> name);
                    }).handle(
                        (name, throwable) -> {
                            final Response res;
                            if (throwable == null) {
                                res = new RsWithHeaders(
                                    new RsWithStatus(RsStatus.FOUND),
                                    new Headers.From("Location", String.format("/dashboard/%s/%s", user, name))
                                );
                            } else if (throwable.getCause() instanceof ArtipieException) {
                                res = new RsWithBody(
                                    new RsWithStatus(RsStatus.BAD_REQUEST),
                                    String.format("Invalid yaml input:\n%s", throwable.getCause().getMessage()),
                                    StandardCharsets.UTF_8
                                );
                            } else {
                                res = new RsWithStatus(RsStatus.INTERNAL_ERROR);
                            }
                            return res;
                        }
                    )
            );
    }

    /**
     * Obtains config from body.
     * @param vals Values in body
     * @return Config content from body
     */
    private static YamlMapping configsFromBody(final ValueFromBody vals) {
        return new Unchecked<>(
            () -> Yaml.createYamlInput(
                vals.byNameOrThrow("config")
            ).readYamlMapping()
        ).value();
    }
}
