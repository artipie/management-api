/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.api.artifactory;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.management.FakeRepoPerms;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetPermissionsSlice}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class GetPermissionsSliceTest {
    /**
     * Artipie base url.
     */
    private static final String BASE = "http://artipie.com/";

    /**
     * Artipie yaml meta section.
     */
    private static final YamlMapping META = Yaml.createYamlMappingBuilder()
        .add("base_url", GetPermissionsSliceTest.BASE)
        .build();

    @Test
    void shouldReturnsPermissionsList() {
        final String read = "readSourceArtifacts";
        final String cache = "populateCaches";
        MatcherAssert.assertThat(
            new GetPermissionsSlice(
                new FakeRepoPerms(cache, read), GetPermissionsSliceTest.META
            ),
            new SliceHasResponse(
                new RsHasBody(
                    Json.createArrayBuilder()
                        .add(this.permJson(cache))
                        .add(this.permJson(read))
                        .build().toString().getBytes(StandardCharsets.UTF_8)
                ),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }

    private JsonObject permJson(final String name) {
        return Json.createObjectBuilder()
            .add("name", name)
            .add(
                "uri", String.format(
                    "%sapi/security/permissions/%s", GetPermissionsSliceTest.BASE, name
                )
            ).build();
    }
}
