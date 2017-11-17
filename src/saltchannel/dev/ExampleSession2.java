package saltchannel.dev;

import java.util.List;
import java.util.Locale;
import saltchannel.Tunnel;
import saltchannel.a1a2.A1Client;
import saltchannel.a1a2.A1Packet;
import saltchannel.a1a2.A2Packet;
import saltchannel.util.CryptoTestData;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;
import saltchannel.v2.SaltServerSession;

/**
 * Example session data, does A1-A2, ANY adress.
 * 
 * @author Frans Lundberg
 */
public class ExampleSession2 {
    private Tunnel tunnel;
    private KeyPair serverSigKeyPair;
    private KeyPair serverEncKeyPair;
    private LoggingByteChannel loggingByteChannel;
    private static final String P1 = A2Packet.SC2_PROT_STRING;
    private static final String P2 = "ECHO------";

    public static void main(String[] args) {
        new ExampleSession2().go();
    }
    
    public ExampleSession2() {
        tunnel = new Tunnel();
        serverSigKeyPair = CryptoTestData.bSig;
        serverEncKeyPair = CryptoTestData.bEnc;
    }
    
    private void go() {
        startServer();
        runClient();
        outputResult();
    }
    
    private void startServer() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                runServer();
            }
        });
        thread.setName(thread.getName() + "-" + this.getClass().getSimpleName() + "-server");
        thread.start();
    }
    
    private void runServer() {
        A2Packet a2 = new A2Packet.Builder().saltChannelProt(P1).prot(P2).build();
        SaltServerSession session = new SaltServerSession(serverSigKeyPair, tunnel.channel2());
        session.setA2(a2);
        session.setEncKeyPair(serverEncKeyPair);
        session.handshake();
    }
    
    private void runClient() {
        loggingByteChannel = new LoggingByteChannel(tunnel.channel1());
        A1Client client = new A1Client(loggingByteChannel);
        A1Packet a1 = client.getA1();
        a1.addressType = A1Packet.ADDRESS_TYPE_PUBKEY;
        a1.address = createAddress();
        
        A2Packet a2 = client.go();
        
        String p1a = P1;
        String p1b = a2.getProts()[0].p1();
        if (!p1a.equals(p1b)) {
            throw new AssertionError(p1a + " != " + p1b);
        }
        
        String p2a = P2;
        String p2b = a2.getProts()[0].p2();
        if (!p2a.equals(p2b)) {
            throw new AssertionError(p2a + " != " + p2b);
        }
    }
    
    public void outputResult() {
        boolean includeTime = false;
        StringBuilder b = new StringBuilder();
        List<LoggingByteChannel.Entry> entries = loggingByteChannel.getLog();
        
        b.append("======== " + this.getClass().getSimpleName() + " ========\n");
        b.append("\n");
        b.append("Example session data for Salt Channel v2.\n");
        b.append("An A1-A2 session; one 'prot' with P1='" + P1 + "' and P2='" + P2 + "'.\n");
        b.append("The *pubkey* type of address (AddressType 1) is used in A1.\n");
        b.append("As a simple example, the public key consists of 32 bytes, all set to 0x08.\n");
        b.append("\n");
        
        b.append("--- Log entries ----\n");
        b.append("\n");
        
        long t0 = entries.get(0).time;
        
        for (int i = 0; i < entries.size(); i++) {
            LoggingByteChannel.Entry entry = entries.get(i);
            long us = (entry.time - t0) / 1000;
            String formattedTime = String.format(Locale.US, "%06d", us);
            
            String sizeString = String.format(Locale.US, "%3d", entry.bytes.length);
            String sizeAndArrowString = entry.type == LoggingByteChannel.ReadOrWrite.READ ? 
                    "<-- " + sizeString
                  : sizeString + " -->";
            
            if (includeTime) {
                b.append(formattedTime + "   ");
            }
            
            b.append(sizeAndArrowString + "   " + entry.type.name() + "\n");
            b.append("    " + Hex.create(entry.bytes) + "\n");
        }
        
        b.append("\n");
        b.append("---- Other ----\n");
        b.append("\n");
        b.append(totalsString());
        b.append("\n");
        
        System.out.println(b.toString());
    }
    
    private String totalsString() {
        List<LoggingByteChannel.Entry> entries = loggingByteChannel.getLog();
        StringBuffer b = new StringBuffer();
        int total = 0;
        
        for (int i = 0; i < entries.size(); i++) {
            LoggingByteChannel.Entry entry = entries.get(i);            
            total += entry.bytes.length;
        }
        
        b.append("total bytes: " + total + "\n");
        b.append("\n");
        
        return b.toString();
    }
    
    private byte[] createAddress() {
        byte[] result = new byte[32];
        for (int i = 0; i < result.length; i++) {
            result[i] = 0x08;
        }
        return result;
    }
}

/*

OUTPUT 2017-10-17

======== ExampleSessionData2 ========

Example session data for Salt Channel v2.
An A1-A2 session; one 'prot' with P1='SCv2------' and P2='ECHO------'.
The *pubkey* type of address (AddressType 1) is used in A1.
As a simple example, the public key consists of 32 bytes, all set to 0x08.

--- Log entries ----

 37 -->   WRITE
    08000120000808080808080808080808080808080808080808080808080808080808080808
<--  23   READ
    098001534376322d2d2d2d2d2d4543484f2d2d2d2d2d2d

---- Other ----

total bytes: 60

*/

