package com.blackducksoftware.integration.swagger.creator

import com.blackducksoftware.integration.swagger.ModelCreator

class FreemarkerComponent {
    private String baseClass
    private String viewPackage
    private Set<String> imports

    public FreemarkerComponent(Set<String> imports, FreemarkerComponentType freemarkerComponentType) {
        this.imports = imports
        setTypeAndPopulateImports(freemarkerComponentType)
    }

    public void setTypeAndPopulateImports(FreemarkerComponentType freemarkerComponentType) {
        if (FreemarkerComponentType.COMPONENT == freemarkerComponentType) {
            populateComponent(ModelCreator.COMPONENT_PACKAGE_SUFFIX)
        } else if (FreemarkerComponentType.RESPONSE == freemarkerComponentType) {
            populateComponent(ModelCreator.RESPONSE_PACKAGE_SUFFIX)
        } else if (FreemarkerComponentType.VIEW == freemarkerComponentType) {
            populateComponent(ModelCreator.VIEW_PACKAGE_SUFFIX)
        } else {
            throw new Exception("Unexpected freemarker type: ${freemarkerComponentType}");
        }
    }

    private void populateComponent(String packageSuffix) {
        viewPackage = ModelCreator.GENERATED_PACKAGE_PREFIX + packageSuffix
        baseClass = 'Hub' + packageSuffix[1..-1].capitalize()
        imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + baseClass);
    }
}
