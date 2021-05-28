/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.IsJson;
import com.artipie.management.Storages;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import javax.json.JsonValue;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Test for {@link GetStorageSlice}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class GetStorageSliceTest {

    /**
     * Patterns.
     */
    static final Map<String, Pattern> PATTERNS = Collections.unmodifiableMap(
        new MapOf<>(
            new MapEntry<>("flat", Pattern.compile("/(?:[^/.]+)(/.*)?")),
            new MapEntry<>("org", Pattern.compile("/(?:[^/.]+)/(?:[^/.]+)(/.*)?"))
        )
    );

    @ParameterizedTest
    @CsvSource({
        "flat,my-lib",
        "org,my-company/my-lib"
    })
    void shouldReturnExpectedData(final String layout, final String repo) {
        final Storage storage = this.example();
        MatcherAssert.assertThat(
            new GetStorageSlice(new Storages.Fake(storage), PATTERNS.get(layout)),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasBody(
                            new IsJson(
                                new JsonHas(
                                    "files",
                                    new JsonContains(
                                        this.entryMatcher("/foo/bar/1", "false"),
                                        this.entryMatcher("/foo/bar/baz", "true")
                                    )
                                )
                            )
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/storage/%s/foo/bar", repo))
            )
        );
    }

    private Matcher<? extends JsonValue> entryMatcher(final String uri, final String folder) {
        return new AllOf<>(
            Arrays.asList(
                new JsonHas("uri", new JsonValueIs(uri)),
                new JsonHas("folder", new JsonValueIs(folder))
            )
        );
    }

    private Storage example() {
        final Storage repo = new InMemoryStorage();
        repo.save(new Key.From("foo/bar/1"), Content.EMPTY).join();
        repo.save(new Key.From("foo/bar/baz/2"), Content.EMPTY).join();
        repo.save(new Key.From("foo/3"), Content.EMPTY).join();
        return repo;
    }
}
