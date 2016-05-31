/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.xml.adapter;

import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttributeRegistry;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;

public class MetacardTypeAdapter extends XmlAdapter<String, MetacardType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTypeAdapter.class);

    private final List<MetacardType> types;

    private final AttributeRegistry attributeRegistry;

    private final InjectableAttributeRegistry injectableAttributeRegistry;

    public MetacardTypeAdapter(List<MetacardType> types, AttributeRegistry attributeRegistry,
            InjectableAttributeRegistry injectableAttributeRegistry) {
        this.types = types;
        this.attributeRegistry = attributeRegistry;
        this.injectableAttributeRegistry = injectableAttributeRegistry;
    }

    public MetacardTypeAdapter() {
        this(null, null, null);
    }

    @Override
    public String marshal(MetacardType type) throws CatalogTransformerException {
        if (type == null) {
            throw new CatalogTransformerException(
                    "Could not transform XML into Metacard.  Invalid MetacardType");
        }
        return type.getName();
    }

    @Override
    public MetacardType unmarshal(String typeName) throws CatalogTransformerException {
        LOGGER.debug("typeName: '{}'", typeName);
        LOGGER.debug("types: {}", types);

        if (StringUtils.isEmpty(typeName) || CollectionUtils.isEmpty(types) || typeName.equals(
                BASIC_METACARD.getName())) {
            return injectAttributes(BASIC_METACARD);
        }

        LOGGER.debug("Searching through registered metacard types {} for '{}'.", types, typeName);
        for (MetacardType type : types) {
            if (typeName.equals(type.getName())) {
                return injectAttributes(type);
            }
        }

        LOGGER.debug("Metacard type '{}' is not registered.  Using metacard type of '{}'.",
                typeName,
                BASIC_METACARD.getName());

        return injectAttributes(BASIC_METACARD);
    }

    private MetacardType injectAttributes(MetacardType original) {
        String metacardTypeName = original.getName();

        Set<AttributeDescriptor> injectAttributes =
                injectableAttributeRegistry.injectableAttributes(metacardTypeName)
                        .stream()
                        .map(attributeRegistry::lookup)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());

        if (injectAttributes.isEmpty()) {
            return original;
        } else {
            return new MetacardTypeImpl(original, injectAttributes, metacardTypeName);
        }
    }
}
