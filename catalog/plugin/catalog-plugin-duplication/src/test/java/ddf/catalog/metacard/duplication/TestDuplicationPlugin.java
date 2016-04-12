/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * </p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.catalog.metacard.duplication;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

@RunWith(MockitoJUnitRunner.class)
public class TestDuplicationPlugin {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FilterBuilder mockFilterBuilder;

    @Mock
    private CatalogFramework mockFramework;

    private DuplicationPlugin plugin;

    private MetacardImpl metacard;

    private static final String ID = "test";

    private ArgumentCaptor<String> attributeValueCaptor = ArgumentCaptor.forClass(String.class);

    private static final String TAG1 = "1";

    private static final String TAG2 = "2";

    private Set tags = new HashSet<>(Arrays.asList(TAG1, TAG2));

    @Before
    public void setup()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {

        when(mockFilterBuilder.attribute(anyString())
                .equalTo()
                .text(attributeValueCaptor.capture())).thenReturn(mock(Filter.class));

        QueryResponse response = mock(QueryResponse.class);

        when(mockFramework.query(any(QueryRequest.class))).thenReturn(response);

        metacard = new MetacardImpl();
        metacard.setId(ID);
        metacard.setAttribute(new AttributeImpl(Metacard.CHECKSUM, "checksum-value"));
        metacard.setTags(tags);

        List<Result> results = Arrays.asList(new ResultImpl(metacard));

        when(response.getResults()).thenReturn(results);
        plugin = new DuplicationPlugin(mockFramework, mockFilterBuilder);
    }

    @Test
    public void testDelete() throws StopProcessingException, PluginExecutionException {
        DeleteRequest request = new DeleteRequestImpl("is");
        assertThat(plugin.process(request), is(request));
    }

    @Test
    public void testUpdate() throws StopProcessingException, PluginExecutionException {
        UpdateRequest request = new UpdateRequestImpl("id", mock(Metacard.class));
        assertThat(plugin.process(request), is(request));
    }

    @Test
    public void testCreateNullInput() throws StopProcessingException, PluginExecutionException {
        CreateRequest request = new CreateRequestImpl((Metacard) null);
        assertThat(plugin.process(request), is(request));
    }

    @Test
    public void testCreateNullConfiguration()
            throws StopProcessingException, PluginExecutionException {

        CreateRequest request = new CreateRequestImpl(mock(Metacard.class));

        plugin.setFailOnDuplicateAttributes(null);
        plugin.setWarnOnDuplicateAttributes(null);
        plugin.setErrorOnDuplicateAttributes(null);

        assertThat(plugin.process(request), is(request));
    }

    @Test
    public void testCreateBlankConfiguration()
            throws StopProcessingException, PluginExecutionException {

        CreateRequest request = new CreateRequestImpl(mock(Metacard.class));

        plugin.setFailOnDuplicateAttributes(new String[0]);
        plugin.setWarnOnDuplicateAttributes(new String[0]);
        plugin.setErrorOnDuplicateAttributes(new String[0]);

        assertThat(plugin.process(request), is(request));
    }

    @Test(expected = StopProcessingException.class)
    public void testCreateAndFailIngest() throws StopProcessingException, PluginExecutionException {

        CreateRequest request = new CreateRequestImpl(metacard);

        String[] attributes = {Metacard.CHECKSUM};
        plugin.setFailOnDuplicateAttributes(attributes);

        plugin.process(request);
    }

    @Test
    public void testCreateAndFailValidation()
            throws StopProcessingException, PluginExecutionException {

        CreateRequest request = new CreateRequestImpl(metacard);

        String[] attributes = {Metacard.CHECKSUM};
        plugin.setWarnOnDuplicateAttributes(attributes);
        plugin.setErrorOnDuplicateAttributes(attributes);

        request = plugin.process(request);

        Metacard resultMetacard = request.getMetacards()
                .get(0);
        assertThat((String) resultMetacard.getAttribute(BasicTypes.VALIDATION_WARNINGS)
                .getValue(), containsString(ID));
        assertThat((String) resultMetacard.getAttribute(BasicTypes.VALIDATION_ERRORS)
                .getValue(), containsString(ID));
    }

    @Test
    public void testCreateAndWarnValidationMultiValuedAttribute()
            throws StopProcessingException, PluginExecutionException, FederationException,
            UnsupportedQueryException, SourceUnavailableException {

        CreateRequest request = new CreateRequestImpl(metacard);
        ArgumentCaptor<QueryRequest> queryRequestCaptor =
                ArgumentCaptor.forClass(QueryRequest.class);

        String[] attributes = {Metacard.TAGS};
        plugin.setWarnOnDuplicateAttributes(attributes);

        request = plugin.process(request);
        verify(mockFramework).query(queryRequestCaptor.capture());
        assertThat(attributeValueCaptor.getAllValues(), hasItems(TAG1, TAG2));

        Metacard resultMetacard = request.getMetacards()
                .get(0);
        assertThat((String) resultMetacard.getAttribute(BasicTypes.VALIDATION_WARNINGS)
                .getValue(), containsString(ID));
    }

}