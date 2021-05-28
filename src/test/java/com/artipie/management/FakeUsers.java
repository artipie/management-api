/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management;

import com.artipie.http.auth.Authentication;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Fake {@link Users} implementation.
 * @since 0.1
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
     * Ctor.
     * @param usrs Users
     */
    public FakeUsers(final Users.User... usrs) {
        this(Stream.of(usrs).collect(Collectors.toMap(user -> user, ignored -> new Password())));
    }

    /**
     * Ctor.
     * @param names User names
     */
    public FakeUsers(final String... names) {
        this(Stream.of(names).collect(Collectors.toMap(User::new, ignored -> new Password())));
    }

    @Override
    public CompletionStage<List<User>> list() {
        return CompletableFuture.completedFuture(new ArrayList<>(this.users.keySet()));
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
     * Get password by username.
     * @param name User name
     * @return Password info
     * @throws NoSuchElementException If user with given name is not found
     */
    public Password pswd(final String name) {
        return this.users.keySet().stream().filter(user -> user.name().equals(name))
            .findFirst().map(this.users::get).orElseThrow();
    }

    /**
     * Get user by username.
     * @param name User name
     * @return User info
     * @throws NoSuchElementException If user with given name is not found
     */
    public Users.User user(final String name) {
        return this.users.keySet().stream().filter(user -> user.name().equals(name))
            .findFirst().orElseThrow();
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

        @Override
        public boolean equals(final Object another) {
            final boolean res;
            if (this == another) {
                res = true;
            } else if (another == null || this.getClass() != another.getClass()) {
                res = false;
            } else {
                final Password password = (Password) another;
                res = password.value.equals(this.value) && password.frmt == this.frmt;
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value, this.frmt);
        }
    }
}
