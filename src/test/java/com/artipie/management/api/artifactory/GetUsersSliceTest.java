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
import com.artipie.management.FakeUsers;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetUsersSlice}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class GetUsersSliceTest {

    /**
     * Artipie base url.
     */
    private static final String BASE = "http://artipie.com/";

    /**
     * Artipie yaml meta section.
     */
    private static final YamlMapping META = Yaml.createYamlMappingBuilder()
        .add("base_url", GetUsersSliceTest.BASE).build();

    @Test
    void returnsUsersList() {
        final String jane = "jane";
        final String john = "john";
        MatcherAssert.assertThat(
            new GetUsersSlice(
                new FakeUsers(jane, john),
                GetUsersSliceTest.META
            ),
            new SliceHasResponse(
                new RsHasBody(
                    Json.createArrayBuilder()
                        .add(this.getUserJson(jane))
                        .add(this.getUserJson(john))
                        .build().toString().getBytes(StandardCharsets.UTF_8)
                ),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }

    private JsonObject getUserJson(final String user) {
        return Json.createObjectBuilder()
            .add("name", user)
            .add("uri", String.format("%sapi/security/users/%s", GetUsersSliceTest.BASE, user))
            .add("realm", "Internal")
            .build();
    }

}
