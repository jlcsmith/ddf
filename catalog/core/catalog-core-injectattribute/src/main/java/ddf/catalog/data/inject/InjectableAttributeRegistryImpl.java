/*
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
 *
 */
package ddf.catalog.data.inject;

import static org.apache.commons.lang.Validate.notNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import ddf.catalog.data.InjectableAttributeRegistry;

public class InjectableAttributeRegistryImpl implements InjectableAttributeRegistry {
    private final Set<String> globalInjections = new CopyOnWriteArraySet<>();

    private final Map<String, Set<String>> metacardTypeInjections = new ConcurrentHashMap<>();

    @Override
    public void registerAttribute(String attributeName) {
        notNull(attributeName, "The attribute name cannot be null.");
        globalInjections.add(attributeName);
    }

    @Override
    public void registerAttribute(String attributeName, String metacardTypeName) {
        notNull(attributeName, "The attribute name cannot be null.");
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        metacardTypeInjections.computeIfAbsent(metacardTypeName, k -> new CopyOnWriteArraySet<>())
                .add(attributeName);
    }

    @Override
    public void deregisterAttribute(String attributeName) {
        notNull(attributeName, "The attribute name cannot be null.");
        globalInjections.remove(attributeName);
    }

    @Override
    public void deregisterAttribute(String attributeName, String metacardTypeName) {
        notNull(attributeName, "The attribute name cannot be null.");
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        metacardTypeInjections.getOrDefault(metacardTypeName, Collections.emptySet())
                .remove(attributeName);
    }

    @Override
    public void deregisterAttributes() {
        globalInjections.clear();
    }

    @Override
    public void deregisterAttributes(String metacardTypeName) {
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        metacardTypeInjections.remove(metacardTypeName);
    }

    @Override
    public Set<String> injectableAttributes(String metacardTypeName) {
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        final Set<String> injections = new HashSet<>(globalInjections);
        injections.addAll(metacardTypeInjections.getOrDefault(metacardTypeName,
                Collections.emptySet()));
        return injections;
    }
}
