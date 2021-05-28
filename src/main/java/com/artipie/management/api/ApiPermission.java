/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permission;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.management.api.artifactory.FromRqLine;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Permissions for API and dashboard endpoints.
 * Accepts HTTP request line as action and checks that request is allowed for the user.
 *
 * @since 0.1
 */
public final class ApiPermission implements Permission {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN_PATH =
        Pattern.compile("(?:/api/\\w+|/dashboard)?/(?<user>[^/.]+)(?:/.*)?");

    /**
     * HTTP request line.
     */
    private final String line;

    /**
     * Ctor.
     *
     * @param line HTTP request line.
     */
    public ApiPermission(final String line) {
        this.line = line;
    }

    @Override
    public boolean allowed(final Authentication.User user) {
        final String path = new RequestLineFrom(this.line).uri().getPath();
        final Matcher matcher = PTN_PATH.matcher(path);
        return matcher.matches() && user.name().equals(matcher.group("user"))
            || Stream.of(FromRqLine.RqPattern.values()).map(FromRqLine.RqPattern::pattern)
                .anyMatch(pattern -> pattern.matcher(path).matches());
    }
}
