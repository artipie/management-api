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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.NotImplementedException;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Fake {@link Users} implementation.
 * @since 0.1
 * @todo #2:30min Implement methods and use this class in tests instead of Users.FromStorageYaml
 *  implementation, enable DeleteUserSliceTest#returnsNotFoundIfCredentialsAreEmpty() test method.
 *  `Users.FromStorageYaml` should stay in artipie and be removed from this repository.
 */
public final class FakeUsers implements Users {

    /**
     * Users.
     */
    private final Map<User, Password> users;

    /**
     * Primary ctor.
     * @param users Users and passwords
     */
    public FakeUsers(final Map<User, Password> users) {
        this.users = users;
    }

    /**
     * Empty users.
     */
    public FakeUsers() {
        this(Collections.emptyMap());
    }

    /**
     * Primary ctor.
     * @param name User name
     */
    public FakeUsers(final String name) {
        this(new HashMap<>(new MapOf<>(new MapEntry<>(new User(name), new Password()))));
    }

    @Override
    public CompletionStage<List<User>> list() {
        final CompletableFuture<List<User>> res = new CompletableFuture<>();
        res.complete(new ArrayList<>(this.users.keySet()));
        return res;
    }

    @Override
    public CompletionStage<Void> add(final User user, final String pswd,
        final PasswordFormat format) {
        this.users.put(user, new Password(pswd, format));
        return CompletableFuture.allOf();
    }

    @Override
    public CompletionStage<Void> remove(final String username) {
        this.users.keySet().stream().filter(user -> user.name().equals(username))
            .findFirst().ifPresent(this.users::remove);
        return CompletableFuture.allOf();
    }

    @Override
    public CompletionStage<Authentication> auth() {
        throw new NotImplementedException("Not implemented");
    }

    /**
     * Password.
     * @since 0.1
     */
    public static final class Password {

        /**
         * Password value.
         */
        private final String value;

        /**
         * Password format.
         */
        private final PasswordFormat frmt;

        /**
         * Ctor.
         * @param value Password value
         * @param format Password format
         */
        public Password(final String value, final PasswordFormat format) {
            this.value = value;
            this.frmt = format;
        }

        /**
         * Dummy password.
         */
        Password() {
            this("123", PasswordFormat.PLAIN);
        }

    }
}
