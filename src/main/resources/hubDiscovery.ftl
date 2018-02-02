package ${discoveryPackage};

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

<#list imports as import>
import ${import};
</#list>
import com.google.gson.reflect.TypeToken;

//this file should not be edited - if changes are necessary, the generator should be updated, then this file should be re-created
public class ApiDiscovery {
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

}
