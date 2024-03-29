/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.dashboard;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.management.ConfigFiles;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.reactivex.Single;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * User page.
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class UserPage implements Page {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/dashboard/(?<user>[^/.]+)/?");

    /**
     * Template engine.
     */
    private final Handlebars handlebars;

    /**
     * Settings.
     */
    private final Storage storage;

    /**
     * Config file to support `yaml` and `.yml` extensions.
     */
    private final ConfigFiles configfile;

    /**
     * New page.
     * @param tpl Template loader
     * @param storage Settings storage
     * @param configfile Config file to support `yaml` and `.yml` extensions
     */
    public UserPage(final TemplateLoader tpl, final Storage storage, final ConfigFiles configfile) {
        this.handlebars = new Handlebars(tpl);
        this.storage = storage;
        this.configfile = configfile;
    }

    @Override
    public Single<String> render(final String line,
        final Iterable<Map.Entry<String, String>> headers) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String user = matcher.group("user");
        return new RxStorageWrapper(this.storage).list(new Key.From(user))
            .map(
                repos -> this.handlebars.compile("user").apply(
                    new MapOf<>(
                        new MapEntry<>("title", user),
                        new MapEntry<>("user", user),
                        new MapEntry<>(
                            "repos",
                            repos.stream()
                                .map(this.configfile::name)
                                .collect(Collectors.toList())
                        )
                    )
                )
            );
    }
}
