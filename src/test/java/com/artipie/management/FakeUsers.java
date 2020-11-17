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
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Fake {@link Users} implementation.
 * @since 0.1
 * @todo #2:30min Implement methods and use this class in tests instead of Users.FromStorageYaml
 *  implementation, enable DeleteUserSliceTest#returnsNotFoundIfCredentialsAreEmpty() test method.
 *  `Users.FromStorageYaml` should stay in artipie and be removed from this repository.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class FakeUsers implements Users {

    @Override
    public CompletionStage<List<User>> list() {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public CompletionStage<Void> add(final User user, final String pswd,
        final PasswordFormat format) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public CompletionStage<Void> remove(final String username) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public CompletionStage<Authentication> auth() {
        throw new NotImplementedException("Not implemented yet");
    }
}
