/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */

package com.artipie.management.api.artifactory;

import com.artipie.asto.Key;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class can print storage key items in different formats,
 * e.g. for Artifactory API and as list HTML page, etc.
 * @since 0.3
 */
public final class KeyList {

    /**
     * Root key.
     */
    private final Key root;

    /**
     * Key set.
     */
    private final Set<Key> keys;

    /**
     * Ctor.
     *
     * @param root Root key.
     */
    public KeyList(final Key root) {
        this.root = root;
        this.keys = new HashSet<>();
    }

    /**
     * Add key to the list.
     * @param key Key to add
     */
    public void add(final Key key) {
        this.keys.add(key);
        key.parent().ifPresent(this::add);
    }

    /**
     * Print sorted key list using specified output format.
     * @param format Output format, e.g. JSON or HTML format
     * @param <T> Format output type
     * @return Formatted result
     */
    public <T> T print(final KeysFormat<T> format) {
        final List<Key> list = new ArrayList<>(this.keys);
        list.sort(Comparator.comparing(Key::string));
        final PeekingIterator<Key> iter = Iterators.peekingIterator(list.iterator());
        while (iter.hasNext()) {
            final Key key = iter.next();
            if (key.parent().map(this.root::equals).orElse(false)) {
                format.add(
                    key,
                    iter.hasNext()
                        && iter.peek().parent().map(parent -> parent.equals(key)).orElse(false)
                );
            }
        }
        return format.result();
    }

    /**
     * Key output format, e.g. JSON or HTML.
     * @param <T> Format output type
     * @since 0.3
     */
    public interface KeysFormat<T> {

        /**
         * Add and accumulate item.
         * @param item Key item
         * @param parent True if item is a parent of another item
         */
        void add(Key item, boolean parent);

        /**
         * Build formatted output.
         * @return Formatted output
         */
        T result();
    }
}
