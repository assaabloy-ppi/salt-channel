package saltaa.nperf;

import java.util.ArrayList;
import java.util.Date;
import saltaa.SaltLib;
import saltaa.SaltLibFactory;

/**
 * Runs the tests.
 * 
 * @author Frans Lundberg
 */
public class NTestRunner {
    private SaltLib lib;
    private ArrayList<NTest> tests;
    private boolean hasRun = false;
    
    public NTestRunner(SaltLib lib) {
        this.lib = lib;
        createTests();
    }
    
    /**
     * Runs the tests.
     */
    public void run() {
        for (NTest t : tests) {
            runTest(t);
        }
        
        hasRun = true;
    }
    
    /**
     * Creates and returns a report.
     */
    public String report() {
        if (!hasRun) {
            throw new IllegalStateException("test have not been run, call run()");
        }
        
        StringBuffer b = new StringBuffer();
        addReportHeader(b);
        
        for (NTest test : tests) {
            addTestResult(b, test);
        }
        
        return b.toString();
    }

    /**
     * Runs the tests.
     */
    public static void main(String[] args) {
        for (SaltLib lib : SaltLibFactory.getAllOperationalLibs()) {
            try {
                runNPerf(lib);
            } catch (Throwable t) {
                System.out.println("ERROR when running " + lib.getName() + ".");
                t.printStackTrace(System.out);
            }
        }
    }
    
    private static void runNPerf(SaltLib lib) {
        NTestRunner r = new NTestRunner(lib);
        r.run();
        
        System.out.println("=========================================");
        System.out.println(r.report());
    }
    
    private void createTests() {
        this.tests = new ArrayList<NTest>();
        
        tests.add(new NTest() {
            byte[] sk = SaltTestData.aSigSec.clone();
            byte[] pk = new byte[SaltLib.crypto_box_PUBLICKEYBYTES];
            
            public String name() {
                return "crypto_box_keypair_not_random";
            }
            
            public void run() {
                lib.crypto_box_keypair_not_random(pk, sk);
            }
        });
        
        tests.add(new NTest() {
            byte[] sk = SaltTestData.aEncSec.clone();
            byte[] pk = SaltTestData.bEncPub.clone();
            byte[] k = new byte[SaltLib.crypto_box_BEFORENMBYTES];
            
            public String name() {
                return "crypto_box_beforenm";
            }
            
            public void run() {
                lib.crypto_box_beforenm(k, pk, sk);
            }
        });
        
        
        tests.add(new NTest() {
            byte[] sk = SaltTestData.aSigSec.clone();
            byte[] pk = new byte[SaltLib.crypto_sign_PUBLICKEYBYTES];
            
            public String name() {
                return "crypto_sign_keypair_not_random";
            }

            @Override
            public void run() {
                lib.crypto_sign_keypair_not_random(pk, sk);
            }
        });
        
        
        tests.add(new NTest() {
            byte[] m = new byte[1];
            byte[] sm = new byte[SaltLib.crypto_sign_BYTES + m.length];
            byte[] sk = SaltTestData.aSigSec;
            
            public String name() {
                return "crypto_sign_1";
            }

            @Override
            public void run() {
                lib.crypto_sign(sm, m, sk);
            }
        });
        
        tests.add(new NTest() {
            byte[] m = new byte[1];
            byte[] mExtended = new byte[SaltLib.crypto_sign_BYTES + m.length];
            byte[] sm = new byte[SaltLib.crypto_sign_BYTES + m.length];
            byte[] sk = SaltTestData.aSigSec;
            byte[] pk = SaltTestData.aSigPub;
            
            public String name() {
                return "crypto_sign_open_1";
            }
            
            public void init() {
                lib.crypto_sign(sm, m, sk);
            }

            @Override
            public void run() {
                lib.crypto_sign_open(mExtended, sm, pk);
            }
        });
    }
    
    private void runTest(NTest test) {
        long t0;
        long t1;
        long t2;
        long min;
        long diff;
        final long RUN_FOR = 1000;     // run for at least this many millis
        
        test.init();
        test.runCount = 0;
        
        t0 = System.nanoTime();
        min = Long.MAX_VALUE;
        
        while (true) {
            t1 = System.nanoTime();
            test.run();
            t2 = System.nanoTime();
            diff = t2 - t1;
            test.runCount++;
            
            if (diff < min) {
                min = diff;
            }
            
            if (t2 - t0 > RUN_FOR * 1000000L) {
                break;
            }
        }
        
        test.time = min * 1e-9;
    }
    

    private void addReportHeader(StringBuffer b) {
        b.append("RESULTS OF NPERF\n");
        b.append("\n");
        b.append("SaltLib: " + lib.getName() + "\n");
        b.append("NPerf software: " + this.getClass().getName() + "\n");
        b.append("Time: " + new Date() + "\n");
        b.append("os.name: " + System.getProperty("os.name") + "\n");
        b.append("java.version: " + System.getProperty("java.version") + "\n");
        b.append("\n");
    }
    
    private void addTestResult(StringBuffer b, NTest test) {
        double millis = test.time * 1000.0;
        
        b.append(test.name() + "\n");
        b.append("    " + String.format("%.2f (%d)\n", millis, test.runCount));
        b.append("\n");
    }
}
