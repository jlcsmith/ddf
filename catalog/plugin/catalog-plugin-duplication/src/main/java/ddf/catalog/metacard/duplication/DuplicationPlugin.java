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

import static ddf.catalog.data.impl.BasicTypes.VALIDATION_ERRORS;
import static ddf.catalog.data.impl.BasicTypes.VALIDATION_WARNINGS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class DuplicationPlugin implements PreIngestPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicationPlugin.class);

    private final CatalogFramework catalogFramework;

    private final FilterBuilder filterBuilder;

    private String[] failOnDuplicateAttributes;

    private String[] errorOnDuplicateAttributes;

    private String[] warnOnDuplicateAttributes;

    public DuplicationPlugin(CatalogFramework catalogFramework, FilterBuilder filterBuilder) {
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
    }

    /**
     * Setter for the list of attributes to test for duplication in the local catalog.  Resulting
     * attributes will cause an {@link StopProcessingException}
     *
     * @param attributeStrings
     */
    public void setFailOnDuplicateAttributes(String[] attributeStrings) {
        if (attributeStrings != null) {
            this.failOnDuplicateAttributes = Arrays.copyOf(attributeStrings,
                    attributeStrings.length);
        }
    }

    /**
     * Setter for the list of attributes to test for duplication in the local catalog.  Resulting
     * attributes will cause the {@link ddf.catalog.data.impl.BasicTypes#VALIDATION_ERRORS} attribute
     * to be set on the metacard.
     *
     * @param attributeStrings
     */
    public void setErrorOnDuplicateAttributes(String[] attributeStrings) {
        if (attributeStrings != null) {
            this.errorOnDuplicateAttributes = Arrays.copyOf(attributeStrings,
                    attributeStrings.length);
        }
    }

    /**
     * Setter for the list of attributes to test for duplication in the local catalog.  Resulting
     * attributes will cause the {@link ddf.catalog.data.impl.BasicTypes#VALIDATION_WARNINGS} attribute
     * to be set on the metacard.
     *
     * @param attributeStrings
     */
    public void setWarnOnDuplicateAttributes(String[] attributeStrings) {
        if (attributeStrings != null) {
            this.warnOnDuplicateAttributes = Arrays.copyOf(attributeStrings,
                    attributeStrings.length);
        }
    }

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {

        if (input != null && input.getMetacards() != null) {
            checkForDuplicates(input.getMetacards());
        }

        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input) throws PluginExecutionException {
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input) throws PluginExecutionException {
        return input;
    }

    private void checkForDuplicates(List<Metacard> metacards) throws StopProcessingException {

        if (ArrayUtils.isNotEmpty(failOnDuplicateAttributes)) {
            checkForDuplicates(metacards, failOnDuplicateAttributes, new FailAction());
        }
        if (ArrayUtils.isNotEmpty(warnOnDuplicateAttributes)) {
            checkForDuplicates(metacards, warnOnDuplicateAttributes, new ValidityAction(
                    VALIDATION_WARNINGS));
        }
        if (ArrayUtils.isNotEmpty(errorOnDuplicateAttributes)) {
            checkForDuplicates(metacards, errorOnDuplicateAttributes, new ValidityAction(
                    VALIDATION_ERRORS));
        }

    }

    /**
     * Internal interface to handle actions when a duplicate is found in the catalog
     */
    interface DuplicateAction {

        /**
         * Performs an action if the metacard is identified as a duplicate.
         *
         * @param metacard
         * @param duplicates
         * @throws StopProcessingException
         */
        void perform(Metacard metacard, List<String> duplicates) throws StopProcessingException;
    }

    static class ValidityAction implements DuplicateAction {
        String validityAttribute;

        ValidityAction(String validityAttribute) {
            this.validityAttribute = validityAttribute;
        }

        @Override
        public void perform(Metacard metacard, List<String> duplicates)
                throws StopProcessingException {
            // TODO: DDF-2047 add duplicate id to the metacard once metacard associations are complete

            Attribute attr = metacard.getAttribute(validityAttribute);
            List<Serializable> validationWarnings;
            if (attr != null && attr.getValues() != null) {
                validationWarnings = attr.getValues();
                validationWarnings.add(duplicateMessage(duplicates));
            } else {
                validationWarnings = new ArrayList<>();
                validationWarnings.add(duplicateMessage(duplicates));
                metacard.setAttribute(new AttributeImpl(validityAttribute, validationWarnings));
            }

        }

    }

    static class FailAction implements DuplicateAction {

        @Override
        public void perform(Metacard metacard, List<String> duplicates)
                throws StopProcessingException {

            throw new StopProcessingException(duplicateMessage(duplicates));

        }
    }

    public static String duplicateMessage(List<String> duplicates) {
        return String.format("Duplicate data found in catalog: {%s}",
                duplicates.stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.joining(", ")));
    }

    private void checkForDuplicates(List<Metacard> metacards, String[] attributeNames,
            DuplicateAction action) throws StopProcessingException {

        for (Metacard metacard : metacards) {
            List<String> duplicates = new ArrayList<>();

            List<Attribute> attributes = Stream.of(attributeNames)
                    .filter(attribute -> metacard.getAttribute(attribute) != null)
                    .map(attribute -> metacard.getAttribute(attribute))
                    .collect(Collectors.toList());
            if (!attributes.isEmpty()) {
                LOGGER.debug(
                        "Checking for duplicates for id {} against attributes [{}] with action {}",
                        metacard.getId(),
                        attributes.stream()
                                .map(Attribute::getName)
                                .collect(Collectors.joining(", ")),
                        action);

                SourceResponse response = query(attributes);
                if (response != null) {
                    response.getResults()
                            .forEach(result -> duplicates.add(result.getMetacard()
                                    .getId()));
                }
                if (!duplicates.isEmpty()) {
                    LOGGER.debug(duplicateMessage(duplicates));
                    action.perform(metacard, duplicates);
                }
            }
        }
    }

    private Filter[] buildFilters(List<Attribute> attributes) {
        List<Filter> filters = new ArrayList<>();
        attributes.forEach(attribute -> {

            attribute.getValues()
                    .forEach(value -> {
                        final Filter filter = filterBuilder.attribute(attribute.getName())
                                .equalTo()
                                .text(value.toString()
                                        .trim());

                        filters.add(filter);

                    });

        });

        return filters.toArray(new Filter[0]);
    }

    private SourceResponse query(List<Attribute> attributes) {

        final Filter filter = filterBuilder.anyOf(buildFilters(attributes));

        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(false);
        QueryRequest request = new QueryRequestImpl(query);

        SourceResponse response = null;
        try {
            response = catalogFramework.query(request);
        } catch (FederationException | SourceUnavailableException | UnsupportedQueryException e) {
            LOGGER.warn("Query failed ", e);
        }
        return response;
    }
}
