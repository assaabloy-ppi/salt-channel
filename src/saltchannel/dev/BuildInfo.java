package saltchannel.dev;

/**
 * Information about the build. BuildInfoTemplate.txt is a the template for the 
 * generated file BuildInfo.java. Do not edit BuildInfo.java manually, edit the template.
 */
public class BuildInfo {
    // Note, Util.identity() is used to avoid compiler inlining.
    
    private BuildInfo() {}
    
    public static final String VERSION = identity("2.6.20200519");
    
    /**
     * Build time, for example: "160508.1435".
     */
    public static final String TIME = identity("20200519.1153");
        
    /**
     * Returns 's'; useful for avoiding compiler inlining of a final field.
     */
    public static String identity(String s) {
        return s;
    }
}

/*
Use something like the following in ANT build script:

<copy file="src/pot/dev/BuildInfoTemplate.txt" 
        toFile="src/pot/dev/BuildInfo.java"
        overwrite="true">
    <filterset begintoken="{{" endtoken="}}">
        <filter token="time" value="${time}"/>
        <filter token="version" value="${version}"/>
    </filterset>
</copy>
*/
