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
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.management.ConfigFiles;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.cactoos.scalar.Unchecked;
import org.reactivestreams.Publisher;

/**
 * Patch repo API.
 * @since 0.1
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 */
public final class ApiRepoUpdateSlice implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/api/repos/(?<user>[^/.]+)");

    /**
     * Config file to support `yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * New patch API.
     * @param configfile Config file to support `yaml` and `.yml` extensions
     */
    public ApiRepoUpdateSlice(final ConfigFiles configfile) {
        this.configfile = configfile;
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
        // @checkstyle LineLengthCheck (500 lines)
        return new AsyncResponse(
            new PublisherAs(body).asciiString().thenCompose(
                form -> {
                    final String name = ApiRepoUpdateSlice.value(form, "repo");
                    final Key key = new Key.From(
                        user,
                        String.format(
                            "%s.yaml",
                            name
                        )
                    );
                    final YamlMapping config = new Unchecked<>(
                        () -> Yaml.createYamlInput(ApiRepoUpdateSlice.value(form, "config"))
                            .readYamlMapping()
                    ).value();
                    return this.configfile.exists(key).thenCompose(
                        exist -> {
                            final CompletionStage<YamlMapping> res;
                            if (exist) {
                                res = SingleInterop.fromFuture(this.configfile.value(key)).to(new ContentAsYaml()).map(
                                    source -> {
                                        final YamlMapping patch = config.yamlMapping("repo");
                                        YamlMappingBuilder repo = Yaml.createYamlMappingBuilder();
                                        repo = repo.add("type", source.yamlMapping("repo").value("type"));
                                        if (patch.value("type") != null) {
                                            repo = repo.add("type", patch.value("type"));
                                        }
                                        repo = repo.add("storage", source.yamlMapping("repo").value("storage"));
                                        if (patch.value("storage") != null && Scalar.class.isAssignableFrom(patch.value("storage").getClass())) {
                                            repo = repo.add("storage", patch.value("storage"));
                                        }
                                        repo = repo.add("permissions", source.yamlMapping("repo").value("permissions"));
                                        if (patch.value("permissions") != null) {
                                            repo = repo.add("permissions", patch.value("permissions"));
                                        }
                                        repo = repo.add("settings", source.yamlMapping("repo").value("settings"));
                                        if (patch.value("permissions") != null) {
                                            repo = repo.add("settings", patch.value("settings"));
                                        }
                                        return Yaml.createYamlMappingBuilder()
                                            .add("repo", repo.build())
                                            .build();
                                    }
                                ).to(SingleInterop.get());
                            } else {
                                final YamlMapping repo = config.yamlMapping("repo");
                                final YamlNode type = repo.value("type");
                                if (type == null || !Scalar.class.isAssignableFrom(type.getClass())) {
                                    throw new IllegalStateException("Repository type required");
                                }
                                final YamlMapping ystor = repo.yamlMapping("storage");
                                final String sstor = repo.string("storage");
                                if (ystor == null && sstor == null) {
                                    throw new IllegalStateException("Repository storage is required");
                                }
                                YamlMappingBuilder yrepo = Yaml.createYamlMappingBuilder()
                                    .add("type", type)
                                    .add("permissions", repo.value("permissions"));
                                if (ystor == null) {
                                    yrepo = yrepo.add("storage", sstor);
                                } else {
                                    yrepo = yrepo.add("storage", ystor);
                                }
                                res = CompletableFuture.completedFuture(
                                    Yaml.createYamlMappingBuilder().add(
                                        "repo", yrepo.build()
                                    ).build()
                                );
                            }
                            return res;
                        }
                        ).thenCompose(yaml -> this.configfile.save(key, new Content.From(yaml.toString().getBytes(StandardCharsets.UTF_8))))
                        .thenApply(
                            ignore -> new RsWithHeaders(
                                new RsWithStatus(RsStatus.FOUND),
                                new Headers.From("Location", String.format("/dashboard/%s/%s", user, name))
                            )
                        );
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
