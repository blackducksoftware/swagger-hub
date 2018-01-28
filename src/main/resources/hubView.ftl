package ${viewPackage};

public class ${className} {
<#list classFields as field>
    public ${field.type} ${field.name};
</#list>

}
