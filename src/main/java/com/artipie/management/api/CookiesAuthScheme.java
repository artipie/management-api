/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.rq.RqHeaders;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.crypto.Cipher;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * API request cookies.
 *
 * @since 0.1
 */
public final class CookiesAuthScheme implements AuthScheme {

    /**
     * Auth scheme name.
     */
    private static final String SCHEME = "Cookie";

    @Override
    public CompletionStage<Result> authenticate(final Iterable<Map.Entry<String, String>> headers) {
        return CompletableFuture.completedFuture(
            CookiesAuthScheme.session(
                Optional.ofNullable(
                    CookiesAuthScheme.cookies(
                        new RqHeaders(headers, CookiesAuthScheme.SCHEME)
                    ).get("session")
                )
            )
        );
    }

    /**
     * Map of cookies.
     *
     * @param raw Raw strings of cookie headers
     * @return Cookies map
     */
    private static Map<String, String> cookies(final Iterable<String> raw) {
        final Map<String, String> map = new HashMap<>(0);
        for (final String value : raw) {
            for (final String pair : value.split(";")) {
                final String[] parts = pair.split("=", 2);
                final String key = parts[0].trim().toLowerCase(Locale.US);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    map.put(key, parts[1].trim());
                } else {
                    map.remove(key);
                }
            }
        }
        return map;
    }

    /**
     * Decode session id to user name.
     * <p>
     * Encoded session string is hex of user id encrypted with RSA public key.
     * See cipher and key spec format for more details.
     * </p>
     *
     * @param encoded Encoded string
     * @return User id
     */
    private static Result session(final Optional<String> encoded) {
        final String env = System.getenv("ARTIPIE_SESSION_KEY");
        final Optional<Authentication.User> user;
        if (env == null || encoded.isEmpty()) {
            user = Optional.empty();
        } else {
            final byte[] key;
            try {
                key = Files.readAllBytes(Paths.get(env));
                final KeySpec spec = new PKCS8EncodedKeySpec(key);
                final Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
                rsa.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA").generatePrivate(spec));
                user = Optional.of(
                    new Authentication.User(
                        new String(
                            rsa.doFinal(Hex.decodeHex(encoded.get().toCharArray())),
                            StandardCharsets.UTF_8
                        )
                    )
                );
            } catch (final IOException | DecoderException | GeneralSecurityException err) {
                Logger.error(
                    CookiesAuthScheme.class, "Failed to read session cookie: %[exception]s"
                );
                throw new IllegalStateException("Failed to read session cookie", err);
            }
        }
        return new AuthScheme.Result() {

            @Override
            public Optional<Authentication.User> user() {
                return user;
            }

            @Override
            public String challenge() {
                return CookiesAuthScheme.SCHEME;
            }
        };
    }
}
