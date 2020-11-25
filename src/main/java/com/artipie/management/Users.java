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
package com.artipie.management;

import com.artipie.http.auth.Authentication;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Artipie credentials.
 * @since 0.1
 */
public interface Users {

    /**
     * Artipie users list.
     * @return Yaml as completion action
     */
    CompletionStage<List<User>> list();

    /**
     * Adds user to artipie users.
     * @param user User info
     * @param pswd Password
     * @param format Password format
     * @return Completion add action
     */
    CompletionStage<Void> add(User user, String pswd, PasswordFormat format);

    /**
     * Removes user from artipie users.
     * @param username User to delete
     * @return Completion remove action
     */
    CompletionStage<Void> remove(String username);

    /**
     * Provides authorization.
     *
     * @return Authentication instance
     */
    CompletionStage<Authentication> auth();

    /**
     * Password format.
     * @since 0.1
     */
    enum PasswordFormat {

        /**
         * Plain password format.
         */
        PLAIN,

        /**
         * Sha256 password format.
         */
        SHA256
    }

    /**
     * User.
     * @since 0.1
     * @todo (#2) - Consider extending this class by adding password and its format, but pay
     *  attention that actually password is not always required. Do not forget about tests.
     */
    final class User {

        /**
         * User name.
         */
        private final String uname;

        /**
         * User email.
         */
        private final Optional<String> mail;

        /**
         * User groups.
         */
        private final Set<String> ugroups;

        /**
         * Ctor.
         * @param name Name of the user
         * @param mail User email
         * @param groups User groups
         */
        public User(final String name, final Optional<String> mail, final Set<String> groups) {
            this.uname = name;
            this.mail = mail;
            this.ugroups = groups;
        }

        /**
         * Ctor.
         * @param name Username
         * @param email User email
         */
        public User(final String name, final Optional<String> email) {
            this(name, email, Collections.emptySet());
        }

        /**
         * Ctor.
         * @param name Username
         */
        public User(final String name) {
            this(name, Optional.empty(), Collections.emptySet());
        }

        /**
         * Get user name.
         * @return Name of the user
         */
        public String name() {
            return this.uname;
        }

        /**
         * Get user email.
         * @return Email of the user
         */
        public Optional<String> email() {
            return this.mail;
        }

        /**
         * Get user groups.
         * @return List of the user groups
         */
        public Set<String> groups() {
            return this.ugroups;
        }

        @Override
        public boolean equals(final Object other) {
            final boolean res;
            if (this == other) {
                res = true;
            } else if (other == null || getClass() != other.getClass()) {
                res = false;
            } else {
                final User user = (User) other;
                res = Objects.equals(this.uname, user.uname)
                    && Objects.equals(this.mail, user.mail)
                    && Objects.equals(this.ugroups, user.ugroups);
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.uname, this.mail, this.ugroups);
        }
    }
}
