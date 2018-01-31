package com.blackducksoftware.integration.swagger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefinitionLinks {
    private final Map<String, List<DefinitionLinkEntry>> namesToEntries = new HashMap<>();
    private final Map<String, Map<String, String>> namesToLinksToJavaConstants = new HashMap<>();
    private final Map<String, Map<String, Boolean>> namesToLinksToHasManyResults = new HashMap<>();
    private final Map<String, Map<String, String>> namesToLinksToResultClasses = new HashMap<>();

    public DefinitionLinks(final List<DefinitionLinkEntry> linkEntries) {
        linkEntries.forEach(linkEntry -> {
            final String definitionName = linkEntry.getDefinitionName();

            if (!namesToEntries.containsKey(definitionName)) {
                namesToEntries.put(definitionName, new ArrayList<>());
            }
            namesToEntries.get(definitionName).add(linkEntry);

            if (!namesToLinksToJavaConstants.containsKey(definitionName)) {
                namesToLinksToJavaConstants.put(definitionName, new HashMap<String, String>());
            }
            final String link = linkEntry.getLink();
            final String constant = convertLinkToJavaConstant(link);
            namesToLinksToJavaConstants.get(definitionName).put(link, constant);

            if (!namesToLinksToHasManyResults.containsKey(definitionName)) {
                namesToLinksToHasManyResults.put(definitionName, new HashMap<String, Boolean>());
            }
            namesToLinksToHasManyResults.get(definitionName).put(link, linkEntry.getCanHaveManyResults());

            if (!namesToLinksToResultClasses.containsKey(definitionName)) {
                namesToLinksToResultClasses.put(definitionName, new HashMap<String, String>());
            }
            final String resultClass = linkEntry.getResultClass();
            namesToLinksToResultClasses.get(definitionName).put(link, resultClass);
        });
    }

    public Map<String, String> getLinksToJavaConstants(final String definitionName) {
        return namesToLinksToJavaConstants.get(definitionName);
    }

    public boolean canHaveManyResults(final String definitionName, final String link) {
        return namesToLinksToHasManyResults.get(definitionName).get(link);
    }

    public String getResultClass(final String definitionName, final String link) {
        return namesToLinksToResultClasses.get(definitionName).get(link);
    }

    private String convertLinkToJavaConstant(final String link) {
        return link.replaceAll("[^A-Za-z0-9]", "_").toUpperCase() + "_LINK";
    }

}
