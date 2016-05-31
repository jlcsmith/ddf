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
package ddf.catalog.data;

import java.util.Set;

/**
 * Manages 'injectable' attributes (i.e., attributes that should be injected into certain metacard
 * types in the system).
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface InjectableAttributeRegistry {
    /**
     * Registers a global 'injectable' attribute (i.e., an attribute that should be injected into
     * all metacard types).
     *
     * @param attributeName the name of the attribute, cannot be null
     * @throws IllegalArgumentException if {@code attributeName} is null
     */
    void registerAttribute(String attributeName);

    /**
     * Registers an 'injectable' attribute that should be injected into a specific metacard type.
     *
     * @param attributeName    the name of the attribute, cannot be null
     * @param metacardTypeName the name of the metacard type into which the attribute should be injected,
     *                         cannot be null
     * @throws IllegalArgumentException if either argument is null
     */
    void registerAttribute(String attributeName, String metacardTypeName);

    /**
     * Removes the given 'global' attribute from the registry.
     * <p>
     * Does nothing if no 'global' attribute by the name {@code attributeName} exists in the registry.
     *
     * @param attributeName the name of the attribute, cannot be null
     * @throws IllegalArgumentException if {@code attributeName} is null
     */
    void deregisterAttribute(String attributeName);

    /**
     * Removes the given metacard type-specific injectable attribute (i.e., this will not remove
     * global injectable attributes).
     *
     * @param attributeName    the name of the attribute, cannot be null
     * @param metacardTypeName the name of the metacard type, cannot be null
     * @throws IllegalArgumentException if either argument is null
     */
    void deregisterAttribute(String attributeName, String metacardTypeName);

    /**
     * Removes all 'global' attributes from the registry.
     */
    void deregisterAttributes();

    /**
     * Removes all metacard type-specific injectable attributes for the given metacard type.
     *
     * @param metacardTypeName the name of the metacard type, cannot be null
     * @throws IllegalArgumentException if {@code metacardTypeName} is null
     */
    void deregisterAttributes(String metacardTypeName);

    /**
     * Returns the names of the attributes that should be injected into the given metacard type.
     *
     * @param metacardTypeName the name of the metacard type, cannot be null
     * @return the set of names of the attributes that should be injected into the given metacard
     * type, or an empty set if none
     * @throws IllegalArgumentException if {@code metacardTypeName} is null
     */
    Set<String> injectableAttributes(String metacardTypeName);
}
