/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.management.api;

import com.artipie.http.auth.Authentication;
import java.util.Map;
import java.util.Optional;

/**
 * Cookies.
 * @since 0.2
 */
public interface Cookies {

    /**
     * Extracts user from cookie headers.
     * @param headers Headers
     * @return User if session cookie found.
     */
    Optional<Authentication.User> user(Iterable<Map.Entry<String, String>> headers);

    /**
     * Fake {@link Cookies} implementation.
     * @since 0.2
     */
    final class Fake implements Cookies {

        /**
         * Optional of User.
         */
        private final Optional<Authentication.User> usr;

        /**
         * Ctor.
         * @param usr User
         */
        public Fake(final Optional<Authentication.User> usr) {
            this.usr = usr;
        }

        /**
         * Ctor.
         * @param name User name
         */
        public Fake(final String name) {
            this(Optional.of(new Authentication.User(name)));
        }

        /**
         * Ctor.
         */
        public Fake() {
            this(Optional.empty());
        }

        @Override
        public Optional<Authentication.User> user(
            final Iterable<Map.Entry<String, String>> headers
        ) {
            return this.usr;
        }

    }

}
