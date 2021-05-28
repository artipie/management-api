/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.dashboard;

import io.reactivex.Single;
import java.util.Map;

/**
 * HTML page.
 * @since 0.2
 */
interface Page {

    /**
     * Render page to HTML string.
     * @param line Request line
     * @param headers Request headers
     * @return HTML string
     */
    Single<String> render(String line, Iterable<Map.Entry<String, String>> headers);
}
