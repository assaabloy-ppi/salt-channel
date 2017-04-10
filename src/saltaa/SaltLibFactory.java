package saltaa;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates SaltLib instances.
 * One instance of each library is created when first requested (lazily).
 * 
 * @author Frans Lundberg
 */
public class SaltLibFactory {
    private static LibHolder javaLib = new LibHolder();
    private static LibHolder nativeLib = new LibHolder();
    private static LibHolder bestLib = new LibHolder();
    private static final Object LIB_SYNC = new Object();

    public enum LibType { 
        JAVA, NATIVE, BEST
    }
    
    public static SaltLib getLib() {
        return getLib(LibType.BEST);
    }
    
    /**
     * Returns a list of all operational SaltLib implementations.
     */
    public static List<SaltLib> getAllOperationalLibs() {
        ArrayList<SaltLib> list = new ArrayList<SaltLib>();
        
        synchronized (LIB_SYNC) {
            initAll();
    
            if (javaLib.status == LibStatus.OK) {
                list.add(javaLib.lib);
            }
            
            if (nativeLib.status == LibStatus.OK) {
                list.add(nativeLib.lib);
            }
        }
        
        return list;
    }
    
    /**
     * Returns the lib requested or throws NoSuchLibException.
     */
    public static SaltLib getLib(LibType type) {
        synchronized (LIB_SYNC) {
            switch (type) {
            case BEST:
                initBest();
                return bestLib.lib;
            case JAVA:
                initJava();
                return javaLib.lib;
            case NATIVE:
                initNative();
                if (nativeLib.status == LibStatus.ERROR) {
                    throw new NoSuchLibException();
                }
                return nativeLib.lib;
            default:
                return getLib(LibType.BEST);
            }
        }
    }
    
    private static void initJava() {
        if (javaLib.status == LibStatus.NOT_INITED) {
            javaLib.lib = new JavaSaltLib();
            javaLib.status = LibStatus.OK;
        }
    }
    
    private static void initNative() {
        if (nativeLib.status == LibStatus.NOT_INITED) {
            try {
                nativeLib.lib = new NativeSaltLib();
                nativeLib.status = LibStatus.OK;
            } catch (Throwable t) {
                nativeLib.status = LibStatus.ERROR;
            }
        }
    }
    
    private static void initBest() {
        if (bestLib.status != LibStatus.NOT_INITED) {
            return;
        }
        
        if (javaLib.status == LibStatus.NOT_INITED) {
            initJava();
        }
        
        if (nativeLib.status == LibStatus.NOT_INITED) {
            initNative();
        }
        
        if (nativeLib.status == LibStatus.OK) {
            bestLib.lib = nativeLib.lib;
        } else {
            bestLib.lib = javaLib.lib;
        }
        
        bestLib.status = LibStatus.OK;
    }
    
    private static void initAll() {
        initJava();
        initNative();
        initBest();
    }
    
    private static enum LibStatus { NOT_INITED, ERROR, OK }
    
    /**
     * Lib reference and its status.
     */
    private static class LibHolder {
        LibStatus status;
        SaltLib lib;
        
        LibHolder() {
            status = LibStatus.NOT_INITED;
        }
    }
}
