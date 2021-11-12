/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.http.rq.RequestLineFrom;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for receiving value from the request line.
 * The request line should match pattern to get value.
 * @since 0.1
 */
public final class FromRqLine {

    /**
     * Request line.
     */
    private final String rqline;

    /**
     * Request line pattern to get value.
     */
    private final RqPattern ptrn;

    /**
     * Ctor.
     * @param rqline Request line
     * @param ptrn Request pattern for receiving value
     */
    FromRqLine(final String rqline, final RqPattern ptrn) {
        this.rqline = rqline;
        this.ptrn = ptrn;
    }

    /**
     * Get username if the request line matches pattern to get username.
     * @return Username from the request line.
     */
    Optional<String> get() {
        final Optional<String> username;
        final Matcher matcher = this.ptrn.pattern.matcher(
            new RequestLineFrom(this.rqline).uri().toString()
        );
        if (matcher.matches()) {
            username = Optional.of(matcher.group(1));
        } else {
            username = Optional.empty();
        }
        return username;
    }

    /**
     * Request pattern for receiving value from the
     * request line.
     */
    public enum RqPattern {
        /**
         * Username pattern.
         */
        USER("/api/security/users/(?<username>[^/.]+)"),

        /**
         * Repo pattern.
         */
        REPO("/api/security/permissions/(?<repo>[^/.]+)"),

        /**
         * Repos pattern.
         */
        REPOS("/api/security/permissions"),

        /**
         * Create repo pattern.
         */
        CREATE_REPO("/api/repositories/.*"),

        /**
         * Users info pattern.
         */
        USERS("/api/security/users");

        /**
         * Pattern.
         */
        private final Pattern pattern;

        /**
         * Ctor.
         * @param pattern Request pattern.
         */
        RqPattern(final String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        /**
         * Get request pattern to get value.
         * @return Request line pattern to get value.
         */
        public Pattern pattern() {
            return this.pattern;
        }
    }
}
