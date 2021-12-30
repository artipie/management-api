/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/management-api/LICENSE.txt
 */
package com.artipie.management.repo;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.ArtipieException;

/**
 * Class for updating repo configuration.
 * @since 0.6
 */
public interface UpdateRepo {
    /**
     * Obtains repository configuration.
     * @return Repository configuration
     */
    YamlMapping repo();

    /**
     * Check repository configuration for required fields.
     * @since 0.6
     */
    class Valid implements UpdateRepo {
        /**
         * Storage yaml node.
         */
        private static final String STORAGE = "storage";

        /**
         * Raw configuration of the repository.
         */
        private final YamlMapping raw;

        /**
         * Ctor.
         * @param raw Raw configuration for validation
         */
        public Valid(final YamlMapping raw) {
            this.raw = raw;
        }

        @Override
        public YamlMapping repo() {
            final YamlNode type = this.raw.value("type");
            if (type == null || !Scalar.class.isAssignableFrom(type.getClass())) {
                throw new ArtipieException("Repository type required");
            }
            final YamlMapping ystor = this.raw.yamlMapping(Valid.STORAGE);
            final String sstor = this.raw.string(Valid.STORAGE);
            if (ystor == null && sstor == null) {
                throw new ArtipieException("Repository storage is required");
            }
            YamlMappingBuilder urepo = Yaml.createYamlMappingBuilder();
            for (final YamlNode key : this.raw.keys()) {
                final String node = key.asScalar().value();
                urepo = urepo.add(node, this.raw.value(node));
            }
            return urepo.build();
        }
    }
}
