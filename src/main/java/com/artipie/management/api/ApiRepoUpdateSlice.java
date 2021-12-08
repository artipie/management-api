/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
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
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import org.cactoos.scalar.Unchecked;
import org.reactivestreams.Publisher;

/**
 * Patch repo API.
 * @since 0.1
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 * @checkstyle NPathComplexityCheck (500 lines)
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
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.NPathComplexity"})
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
                .thenApply(form -> URLDecoder.decode(form, StandardCharsets.US_ASCII)).thenCompose(
                    form -> {
                        final YamlMapping config = new Unchecked<>(
                            () -> Yaml.createYamlInput(ApiRepoUpdateSlice.value(form, "config"))
                                .readYamlMapping()
                        ).value();
                        final YamlMapping repo = config.yamlMapping("repo");
                        if (repo == null) {
                            throw new ArtipieException("Repo section is required");
                        }
                        final YamlNode type = repo.value("type");
                        if (type == null || !Scalar.class.isAssignableFrom(type.getClass())) {
                            throw new ArtipieException("Repository type required");
                        }
                        final YamlMapping ystor = repo.yamlMapping("storage");
                        final String sstor = repo.string("storage");
                        if (ystor == null && sstor == null) {
                            throw new ArtipieException("Repository storage is required");
                        }
                        YamlMappingBuilder yrepo = Yaml.createYamlMappingBuilder().add("type", type);
                        if (ystor == null) {
                            yrepo = yrepo.add("storage", sstor);
                        } else {
                            yrepo = yrepo.add("storage", ystor);
                        }
                        if (repo.value("permissions") != null) {
                            yrepo = yrepo.add("permissions", repo.value("permissions"));
                        }
                        if (repo.value("settings") != null) {
                            yrepo = yrepo.add("settings", repo.value("settings"));
                        }
                        final String name = ApiRepoUpdateSlice.value(form, "repo");
                        return this.configfile.save(
                            new Key.From(user, String.format("%s.yaml", name)),
                            new Content.From(
                                Yaml.createYamlMappingBuilder().add("repo", yrepo.build())
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
     * Obtain value from payload, payload is a query string (not url-encoded):
     * <code>name1=value1&name2=value2</code>.
     * @param payload Payload to parse
     * @param name Parameter name to obtain
     * @return Parameter value
     * @checkstyle StringLiteralsConcatenationCheck (10 lines)
     */
    private static String value(final String payload, final String name) {
        final int start = payload.indexOf(String.format("%s=", name)) + name.length() + 1;
        int end = payload.indexOf('&', start);
        if (end == -1) {
            end = payload.length();
        }
        return payload.substring(start, end);
    }
}
