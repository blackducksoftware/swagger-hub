package ${discoveryPackage};

import java.util.HashMap;
import java.util.Map;

<#list imports as import>
import ${import};
</#list>

//this file should not be edited - if changes are necessary, the generator should be updated, then this file should be re-created
public class ApiDiscovery {
    public static final Map<HubPath, LinkResponse> links = new HashMap<>();

<#list links as link>
    public static final HubPath ${link.javaConstant} = new HubPath("${link.label}");
</#list>

<#list links as link>
    public static final ${link.linkType} ${link.javaConstant}_RESPONSE = new ${link.linkType}(${link.javaConstant}, ${link.resultClass}.class);
</#list>

    static {
    <#list links as link>
        links.put(${link.javaConstant}, ${link.javaConstant}_RESPONSE);
    </#list>
    }

}
