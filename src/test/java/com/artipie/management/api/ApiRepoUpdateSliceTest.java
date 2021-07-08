/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.management.FakeConfigFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link ApiRepoUpdateSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ApiRepoUpdateSliceTest {

    /**
     * User.
     */
    private static final String USER = "bob";

    /**
     * Repo.
     */
    private static final String REPO = "my-repo";

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createsRepoConfiguration(final boolean param) {
        final String yaml = yaml("maven", param);
        MatcherAssert.assertThat(
            "Returns FOUND",
            new ApiRepoUpdateSlice(new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FOUND),
                new RequestLine(
                    RqMethod.POST, String.format("/api/repos/%s", ApiRepoUpdateSliceTest.USER)
                ),
                new Headers.From(
                    "Location", String.format(
                        "/dashboard/%s", ApiRepoUpdateSliceTest.userRepo()
                    )
                ),
                new Content.From(body(ApiRepoUpdateSliceTest.REPO, yaml))
            )
        );
        MatcherAssert.assertThat(
            "Config is not correct",
            this.storage.value(
                new Key.From(String.format("%s.yaml", ApiRepoUpdateSliceTest.userRepo()))
            ).join(),
            new ContentIs(yaml, StandardCharsets.UTF_8)
        );
    }

    @ParameterizedTest
    @CsvSource({
        ".yaml,true",
        ".yaml,false",
        ".yml,true",
        ".yml,false"
    })
    void updatesRepoConfiguration(final String extension, final boolean param) {
        final String oldtype = "pypi";
        final String type = "docker";
        final String yaml = yaml(type, param);
        this.storage.save(
            new Key.From(String.format("%s%s", ApiRepoUpdateSliceTest.userRepo(), extension)),
            new Content.From(yaml(oldtype, param).getBytes())
        ).join();
        MatcherAssert.assertThat(
            "Returns FOUND",
            new ApiRepoUpdateSlice(new FakeConfigFile(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FOUND),
                new RequestLine(
                    RqMethod.POST,
                    String.format("/api/repos/%s", ApiRepoUpdateSliceTest.USER)
                ),
                new Headers.From(
                    "Location", String.format(
                        "/dashboard/%s", ApiRepoUpdateSliceTest.userRepo()
                )
                ),
                new Content.From(body(ApiRepoUpdateSliceTest.REPO, yaml))
            )
        );
        MatcherAssert.assertThat(
            "Config file is updated",
            this.storage.value(
                new Key.From(String.format("%s.yaml", ApiRepoUpdateSliceTest.userRepo()))
            ).join(),
            new ContentIs(yaml, StandardCharsets.UTF_8)
        );
    }

    private static byte[] body(final String reponame, final String yaml) {
        return URLEncoder.encode(
            String.format("repo=%s&config=%s", reponame, yaml),
            StandardCharsets.US_ASCII
        ).getBytes();
    }

    private static String userRepo() {
        return String.format("%s/%s", ApiRepoUpdateSliceTest.USER, ApiRepoUpdateSliceTest.REPO);
    }

    private static String yaml(final String type, final boolean full) {
        YamlMappingBuilder repo = Yaml.createYamlMappingBuilder()
            .add("type", type);
        if (full) {
            repo = repo.add(
                "storage",
                Yaml.createYamlMappingBuilder().add("type", "fs").add("path", "my/path").build()
            );
            repo = repo.add(
                "permissions", Yaml.createYamlMappingBuilder().add(
                    "john", Yaml.createYamlSequenceBuilder()
                        .add("read")
                        .add("write")
                        .build()
                ).build()
            );
            repo = repo.add("settings", "abc123");
        } else  {
            repo = repo.add("storage", "default");
        }
        return Yaml.createYamlMappingBuilder().add("repo", repo.build()).build().toString();
    }

}
