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
package ddf.catalog.data.inject;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.InjectableAttributeRegistry;

public class InjectableAttributeRegistryImplTest {
    private static final String BASIC_TYPE = "ddf.metacard";

    private static final String OTHER_TYPE = "other.type";

    private InjectableAttributeRegistry registry;

    @Before
    public void setUp() {
        registry = new InjectableAttributeRegistryImpl();
    }

    @Test
    public void testRegisterGlobal() {
        final String attribute = "foo";
        registry.registerAttribute(attribute);

        final Set<String> expectedAttributes = newHashSet(attribute);
        assertThat(registry.injectableAttributes(BASIC_TYPE), is(expectedAttributes));
        assertThat(registry.injectableAttributes(OTHER_TYPE), is(expectedAttributes));
    }

    @Test
    public void testRegisterSpecificToMetacardType() {
        final String globalAttribute = "foo";
        final String specificAttribute = "specific";
        registry.registerAttribute(globalAttribute);
        registry.registerAttribute(specificAttribute, BASIC_TYPE);

        final Set<String> expectedAttributesBasicType = newHashSet(globalAttribute,
                specificAttribute);
        assertThat(registry.injectableAttributes(BASIC_TYPE), is(expectedAttributesBasicType));

        final Set<String> expectedAttributesGlobal = newHashSet(globalAttribute);
        assertThat(registry.injectableAttributes(OTHER_TYPE), is(expectedAttributesGlobal));
    }

    @Test
    public void testDeregisterAllGlobal() {
        final String global1 = "first";
        final String global2 = "second";
        registry.registerAttribute(global1);
        registry.registerAttribute(global2);

        final String specificAttribute = "specific";
        registry.registerAttribute(specificAttribute, BASIC_TYPE);

        registry.deregisterAttributes();

        assertThat(registry.injectableAttributes(OTHER_TYPE), is(empty()));

        final Set<String> expectedAttributesBasicType = newHashSet(specificAttribute);
        assertThat(registry.injectableAttributes(BASIC_TYPE), is(expectedAttributesBasicType));
    }

    @Test
    public void testDeregisterSpecificGlobal() {
        final String global1 = "first";
        final String global2 = "second";
        registry.registerAttribute(global1);
        registry.registerAttribute(global2);

        final String specificAttribute = "specific";
        registry.registerAttribute(specificAttribute, BASIC_TYPE);

        registry.deregisterAttribute(global1);

        final Set<String> expectedAttributesGlobal = newHashSet(global2);
        assertThat(registry.injectableAttributes("other.type"), is(expectedAttributesGlobal));

        final Set<String> expectedAttributesBasicType = newHashSet(global2, specificAttribute);
        assertThat(registry.injectableAttributes(BASIC_TYPE), is(expectedAttributesBasicType));
    }

    @Test
    public void testDeregisterAllForMetacardType() {
        final String global = "global";
        registry.registerAttribute(global);

        final String specific1 = "first";
        final String specific2 = "second";
        registry.registerAttribute(specific1, BASIC_TYPE);
        registry.registerAttribute(specific2, BASIC_TYPE);

        registry.registerAttribute(specific1, OTHER_TYPE);
        registry.registerAttribute(specific2, OTHER_TYPE);

        registry.deregisterAttributes(BASIC_TYPE);

        final Set<String> expectedAttributesBasicType = newHashSet(global);
        assertThat(registry.injectableAttributes(BASIC_TYPE), is(expectedAttributesBasicType));

        final Set<String> expectedAttributesOtherType = newHashSet(global, specific1, specific2);
        assertThat(registry.injectableAttributes(OTHER_TYPE), is(expectedAttributesOtherType));
    }

    @Test
    public void testDeregisterSpecificForMetacardType() {
        final String global = "global";
        registry.registerAttribute(global);

        final String specific1 = "first";
        final String specific2 = "second";
        registry.registerAttribute(specific1, BASIC_TYPE);
        registry.registerAttribute(specific2, BASIC_TYPE);

        registry.registerAttribute(specific1, OTHER_TYPE);
        registry.registerAttribute(specific2, OTHER_TYPE);

        registry.deregisterAttribute(specific1, BASIC_TYPE);

        final Set<String> expectedAttributesBasicType = newHashSet(global, specific2);
        assertThat(registry.injectableAttributes(BASIC_TYPE), is(expectedAttributesBasicType));

        final Set<String> expectedAttributesOtherType = newHashSet(global, specific1, specific2);
        assertThat(registry.injectableAttributes(OTHER_TYPE), is(expectedAttributesOtherType));
    }
}
