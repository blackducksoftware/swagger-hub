package ${discoveryPackage};

import java.util.HashMap;
import java.util.Map;

<#list imports as import>
import ${import};
</#list>

//this file should not be edited - if changes are necessary, the generator should be updated, then this file should be re-created
public class ApiDiscovery {
    public static final Map<String, LinkResponse> links = new HashMap<>();

<#list links as link>
    public static final String ${link.javaConstant} = "${link.label}";
</#list>

<#list links as link>
    <#if link.hasMultipleResults??>
        <#assign linkType="LinkMultipleResponses"> 
    <#else>
        <#assign linkType="LinkSingleResponse"> 
    </#if>
    public static final ${linkType} ${link.javaConstant}_RESPONSE = new ${linkType}(${link.javaConstant}, ${link.resultClass}.class);
</#list>

    static {
    <#list links as link>
        links.put(${link.javaConstant}, ${link.javaConstant}_RESPONSE);
    </#list>
    }

}
