package ${viewPackage};

<#if hasLinks??>
import java.lang.reflect.Type;
<#if hasMultipleResultsLink??>
import java.util.ArrayList;
</#if>
import java.util.HashMap;
import java.util.Map;

</#if>
import com.blackducksoftware.integration.hub.model.${baseClass};
<#if hasLinks?? && hasMultipleResultsLink??>
import com.google.gson.reflect.TypeToken;
</#if>

//this file should not be edited - if changes are necessary, the generator should be updated, then this file should be re-created
public class ${className} extends ${baseClass} {
<#if hasLinks??>
    public static final Map<String, Type> links = new HashMap<>();

<#list links as link>
    public static final String ${link.javaConstant} = "${link.label}";
</#list>

    static {
    <#list links as link>
        <#if link.hasMultipleResults??>
            <#assign linkType="new TypeToken<ArrayList<${link.resultClass}>>() {}.getType()">
        <#else>
            <#assign linkType="${link.resultClass}.class">
        </#if>
        links.put(${link.javaConstant}, ${linkType});
    </#list>
    }

</#if>
<#list classFields as field>
    public ${field.type} ${field.name};
</#list>

}
