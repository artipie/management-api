/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.dashboard;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Key;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.management.ConfigFiles;
import com.artipie.management.api.ContentAsYaml;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.TemplateLoader;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Repository page.
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RepoPage implements Page {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/dashboard/(?<key>[^/.]+/[^/.]+)/?");

    /**
     * Template engine.
     */
    private final Handlebars handlebars;

    /**
     * Config file to support `yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * New page.
     * @param tpl Template engine
     * @param configfile Config file to support `yaml` and `.yml` extensions
     */
    public RepoPage(final TemplateLoader tpl, final ConfigFiles configfile) {
        this.handlebars = new Handlebars(tpl);
        this.configfile = configfile;
    }

    @Override
    public Single<String> render(final String line,
        final Iterable<Map.Entry<String, String>> headers) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String name = matcher.group("key");
        this.handlebars.registerHelper("eq", ConditionalHelpers.eq);
        final String[] parts = name.split("/");
        final Key key = new Key.From(String.format("%s.yaml", name));
        // @checkstyle LineLengthCheck (30 lines)
        return SingleInterop.fromFuture(this.configfile.exists(key))
            .filter(exists -> exists)
            .flatMapSingleElement(
                ignore -> SingleInterop.fromFuture(this.configfile.value(key)).to(new ContentAsYaml()).map(
                    config -> {
                        final YamlMapping repo = config.yamlMapping("repo");
                        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
                        builder = builder.add("type", repo.value("type"));
                        if (repo.value("storage") != null
                            && Scalar.class.isAssignableFrom(repo.value("storage").getClass())) {
                            builder = builder.add("storage", repo.value("storage"));
                        }
                        builder = builder.add("permissions", repo.value("permissions"));
                        if (repo.value("settings") != null) {
                            builder = builder.add("settings", repo.value("settings"));
                        }
                        return Yaml.createYamlMappingBuilder().add("repo", builder.build()).build();
                    }
                ).map(
                    yaml -> this.handlebars.compile("repo").apply(
                        new MapOf<>(
                            new MapEntry<>("title", name),
                            new MapEntry<>("user", parts[0]),
                            new MapEntry<>("name", parts[1]),
                            new MapEntry<>("config", yaml.toString()),
                            new MapEntry<>("found", true),
                            new MapEntry<>("type", yaml.yamlMapping("repo").value("type").asScalar().value())
                        )
                    )
                )
            ).switchIfEmpty(
                Single.fromCallable(
                    () -> this.handlebars.compile("repo").apply(
                        new MapOf<>(
                            new MapEntry<>("title", name),
                            new MapEntry<>("user", parts[0]),
                            new MapEntry<>("name", parts[1]),
                            new MapEntry<>("found", false),
                            new MapEntry<>(
                                "type",
                                URLEncodedUtils.parse(
                                    new RequestLineFrom(line).uri(),
                                    StandardCharsets.UTF_8
                                ).stream()
                                    .filter(pair -> "type".equals(pair.getName()))
                                    .findFirst().map(NameValuePair::getValue)
                                    .orElse("maven")
                            )
                        )
                    )
                )
            );
    }
}
