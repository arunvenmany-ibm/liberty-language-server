/*******************************************************************************
* Copyright (c) 2022, 2025 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.model.propertiesfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.Messages;
import io.openliberty.tools.langserver.utils.ServerPropertyValues;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class PropertiesKeyInstance {

    private String propertyKey;
    private PropertiesEntryInstance propertyEntryInstance;
    private LibertyTextDocument textDocumentItem;

    public PropertiesKeyInstance(String propertyKeyInstanceString, PropertiesEntryInstance propertyEntryInstance, LibertyTextDocument textDocumentItem) {
        this.propertyKey = propertyKeyInstanceString;
        this.propertyEntryInstance = propertyEntryInstance;
        this.textDocumentItem = textDocumentItem;
    }

    /**
     * Returns the index of the end of the property key
     * @return index of the end of the property key
     */
    public int getEndPosition() {
        return propertyKey.length()-1;
    }

    public CompletableFuture<Hover> getHover(Position position) {
        String message = null;
        message = Messages.getPropDescription(propertyKey);

        // set hover range to highlight the entire key
        int line = position.getLine();
        Position rangeStart = new Position(line, 0);
        Position rangeEnd = new Position(line, propertyKey.length());
        Range range = new Range(rangeStart, rangeEnd);

        Hover hover = new Hover(new MarkupContent(MarkupKind.MARKDOWN, message), range);
        return CompletableFuture.completedFuture(hover);
    }

    public CompletableFuture<List<CompletionItem>> getCompletions(String enteredText, Position position) {
        List<String> matches = Messages.getMatchingKeys(enteredText, textDocumentItem);
        List<CompletionItem> results = matches.stream().map(s ->{
            int line = position.getLine();
            Position rangeStart = new Position(line, 0);
            Position rangeEnd = new Position(line, s.length());
            Range range = new Range(rangeStart, rangeEnd);
            Either<TextEdit, InsertReplaceEdit> edit = Either.forLeft(new TextEdit(range, s));
            CompletionItem completionItem=new CompletionItem();
            completionItem.setTextEdit(edit);
            completionItem.setLabel(s);
            return completionItem;
        }).toList();
        // set hover description as the 'detail' on the CompletionItem
        setDetailsOnCompletionItems(results, null, false);
        return CompletableFuture.completedFuture(results);
    }

    // Iterate through the passed in CompletionItem List and do the following:
    //
    // * Set the 'documentation' to the hover description. If the 'key' is null, use the 'label' from the CompletionItem to 
    // get the hover description. 
    // * Set the 'kind' to text. 
    // * Set the default to the first item in the list if the 'setDefault' parameter is true. 
    //
    protected void setDetailsOnCompletionItems(List<CompletionItem> items, String key, boolean setDefault) {
        boolean defaultSet = false;
        for (CompletionItem item : items) {
            if (setDefault && !defaultSet) {
                item.setPreselect(true);
                defaultSet = true;
            }
            String keyToUse = key == null ? item.getLabel() : key;
            String desc = Messages.getPropDescription(keyToUse);
            MarkupContent markdown = new MarkupContent(MarkupKind.MARKDOWN, desc);
            item.setDocumentation(markdown);
            // if setDefault is true, we are dealing with values which are considered text.
            // otherwise we are dealing with keys which are considered properties.
            if (setDefault) {
                item.setKind(CompletionItemKind.Text);
            } else {
                item.setKind(CompletionItemKind.Property);
            }
        }
    }

    public List<CompletionItem> getValidValues(String enteredValue, Position position) {
        List<String> validValues = ServerPropertyValues.getValidValues(textDocumentItem, propertyKey);
        List<String> enteredValuesLowerCase = new ArrayList<>();
        String filterValue;
        String prefix;
        int endPosition = getEndPosition() + 2;
        if (ServerPropertyValues.multipleCommaSeparatedValuesAllowed(propertyKey)) {
            if (enteredValue != null && enteredValue.contains(",")) {
                // has comma separated values
                prefix = enteredValue.substring(0, enteredValue.lastIndexOf(",") + 1);
                filterValue = enteredValue.substring(enteredValue.lastIndexOf(",") + 1);
                enteredValuesLowerCase = ServerPropertyValues.getCommaSeparatedValues(prefix).stream()
                        .map(String::toLowerCase).toList();
                // in case any of the entered value is invalid, do not show completion
                Set<String> invalidValuesEntered = new HashSet<>(enteredValuesLowerCase);
                validValues.forEach(v -> invalidValuesEntered.remove(v.toLowerCase()));
                if (!invalidValuesEntered.isEmpty()) {
                    return Collections.emptyList();
                }
            } else {
                // in case of no comma, set prefix as empty and use entered value to filter for property
                filterValue = enteredValue;
                prefix = "";
            }
        } else {
            // in case single value allowed, set prefix as empty and use entered value to filter for property
            // this else is required to make variable effectively final
            filterValue = enteredValue;
            prefix = "";
        }
        List<CompletionItem> results = generateCompletionItemsList(position, validValues, filterValue, enteredValuesLowerCase, prefix, endPosition);
        // Preselect the default.
        // This uses the first item in the List as default.
        // (Check ServerPropertyValues.java) Currently sorted to have confirmed/sensible values as default.
        setDetailsOnCompletionItems(results, propertyKey, true);
        return results;
    }

    private List<CompletionItem> generateCompletionItemsList(Position position, List<String> validValues, String filterValue, List<String> enteredValuesLowerCase, String prefix, int endPosition) {
        Stream<String> filteredCompletion = validValues.stream()
                .filter(s -> s.toLowerCase().contains(filterValue.trim().toLowerCase()));
        if (!enteredValuesLowerCase.isEmpty()) {
            // if there is already a value specified in case of comma separated field, filter out existing
            filteredCompletion = filteredCompletion.filter(c -> !enteredValuesLowerCase.contains(c.toLowerCase()));
        }
        return filteredCompletion.map(s -> getCompletionItem(position, prefix + s, endPosition)).toList();
    }

    private CompletionItem getCompletionItem(Position position, String s, int endPosition) {
        int line = position.getLine();
        Position rangeStart = new Position(line, endPosition);
        Position rangeEnd = new Position(line, endPosition + s.length());
        Range range = new Range(rangeStart, rangeEnd);
        Either<TextEdit, InsertReplaceEdit> edit = Either.forLeft(new TextEdit(range, s));
        CompletionItem completionItem = new CompletionItem();
        completionItem.setTextEdit(edit);
        completionItem.setLabel(s);
        return completionItem;
    }

    @Override
    public String toString() {
        return this.propertyKey;
    }
}
