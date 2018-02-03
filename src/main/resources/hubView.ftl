package ${viewPackage};

<#if hasLinksWithResults??>
import java.lang.reflect.Type;
  <#if hasMultipleResultsLink??>
import java.util.ArrayList;
  </#if>
import java.util.HashMap;
import java.util.Map;

</#if>
<#list imports as import>
import ${import};
</#list>
<#if hasLinks?? && hasMultipleResultsLink??>
import com.google.gson.reflect.TypeToken;
</#if>

//this file should not be edited - if changes are necessary, the generator should be updated, then this file should be re-created
public class ${className} extends ${baseClass} {
<#if hasLinksWithResults??>
    public static final Map<String, Type> links = new HashMap<>();

</#if>
<#if hasLinks??>
  <#list links as link>
    public static final String ${link.javaConstant} = "${link.label}";
  </#list>

</#if>
<#if hasLinksWithResults??>
    static {
    <#list links as link>
        <#if link.resultClass??>
            <#if link.hasMultipleResults??>
                <#assign linkType="new TypeToken<ArrayList<${link.resultClass}>>() {}.getType()">
            <#else>
                <#assign linkType="${link.resultClass}.class">
            </#if>
            links.put(${link.javaConstant}, ${linkType});
        </#if>
    </#list>
    }

</#if>
<#list classFields as field>
    public ${field.type} ${field.name};
</#list>

}
