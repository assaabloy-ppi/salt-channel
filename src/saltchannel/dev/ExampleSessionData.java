package saltchannel.dev;

import java.util.List;
import java.util.Locale;
import saltchannel.ByteChannel;
import saltchannel.Tunnel;
import saltchannel.util.CryptoTestData;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;
import saltchannel.v2.SaltClientSession;
import saltchannel.v2.SaltServerSession;

/**
 * Example session data; used as an appendix to the
 * Salt Channel v2 specification.
 * An executable class that outputs data needed to reproduce a 
 * simple Salt Channel session.
 * 
 * @author Frans Lundberg
 */
public class ExampleSessionData {
    private Tunnel tunnel;
    private KeyPair clientSigKeyPair;
    private KeyPair clientEncKeyPair;
    private KeyPair serverSigKeyPair;
    private KeyPair serverEncKeyPair;
    private byte[] appRequest = new byte[]{0x01, 0x05, 0x05, 0x05, 0x05, 0x05};
    private byte[] appResponse;
    private LoggingByteChannel loggingByteChannel;
    private byte[] sessionKey;

    public static void main(String[] args) {
        new ExampleSessionData().go();
    }
    
    public ExampleSessionData() {
        tunnel = new Tunnel();
        clientSigKeyPair = CryptoTestData.aSig;
        clientEncKeyPair = CryptoTestData.aEnc;
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
        SaltServerSession session = new SaltServerSession(serverSigKeyPair, tunnel.channel2());
        session.setEncKeyPair(serverEncKeyPair);
        session.setBufferM2(true);
        session.handshake();
        ByteChannel appChannel = session.getChannel();
        appChannel.write(appChannel.read());    // echo once
    }
    
    private void runClient() {
        loggingByteChannel = new LoggingByteChannel(tunnel.channel1());
        SaltClientSession session = new SaltClientSession(clientSigKeyPair, loggingByteChannel);
        session.setEncKeyPair(clientEncKeyPair);
        session.setBufferM4(true);
        session.handshake();
        ByteChannel appChannel = session.getChannel();
        appChannel.write(appRequest);
        appResponse = appChannel.read();
        
        this.sessionKey = session.getSessionKey();
    }
    
    public void outputResult() {
        boolean includeTime = false;
        StringBuilder b = new StringBuilder();
        List<LoggingByteChannel.Entry> entries = loggingByteChannel.getLog();
        
        b.append("======== " + this.getClass().getSimpleName() + " ========\n");
        b.append("\n");
        b.append("Example session data for Salt Channel v2.\n");
        b.append("\n");
        b.append("---- key pairs, secret key first ----\n");
        b.append("\n");
        b.append(keyPairString());
        b.append("\n");
        
        b.append("--- Log entries, microsecond time ----\n");
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
        b.append("session key: " + Hex.create(sessionKey) + "\n");
        b.append("app request:  " + Hex.create(appRequest) + "\n");
        b.append("app response: " + Hex.create(appResponse) + "\n");
        b.append(totalsString());
        b.append("\n");
        
        System.out.println(b.toString());
    }
    
    private String keyPairString() {
        StringBuffer b = new StringBuffer();
        
        
        b.append("client signature key pair:\n");
        b.append("    " + Hex.create(clientSigKeyPair.sec()) + "\n");
        b.append("    " + Hex.create(clientSigKeyPair.pub()) + "\n");
        
        b.append("client encryption key pair:\n");
        b.append("    " + Hex.create(clientEncKeyPair.sec()) + "\n");
        b.append("    " + Hex.create(clientEncKeyPair.pub()) + "\n");
        
        b.append("server signature key pair:\n");
        b.append("    " + Hex.create(serverSigKeyPair.sec()) + "\n");
        b.append("    " + Hex.create(serverSigKeyPair.pub()) + "\n");
        
        b.append("server encryption key pair:\n");
        b.append("    " + Hex.create(serverEncKeyPair.sec()) + "\n");
        b.append("    " + Hex.create(serverEncKeyPair.pub()) + "\n");
        
        return b.toString();
    }
    
    private String totalsString() {
        List<LoggingByteChannel.Entry> entries = loggingByteChannel.getLog();
        StringBuffer b = new StringBuffer();
        int total = 0;
        int totalInHandshake = 0;
        
        for (int i = 0; i < entries.size(); i++) {
            LoggingByteChannel.Entry entry = entries.get(i);
            
            if (i < 4) {
                totalInHandshake += entry.bytes.length;
            }
            
            total += entry.bytes.length;
        }
        
        b.append("total bytes: " + total + "\n");
        b.append("total bytes, handshake only: " + totalInHandshake + "\n");
        b.append("\n");
        
        return b.toString();
    }
}

/*

OUTPUT 2017-05-18

======== ExampleSessionData ========

Example session data for Salt Channel v2.

---- key pairs, secret key first ----

client signature key pair:
    55f4d1d198093c84de9ee9a6299e0f6891c2e1d0b369efb592a9e3f169fb0f795529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b
    5529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b
client encryption key pair:
    77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a
    8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a
server signature key pair:
    7a772fa9014b423300076a2ff646463952f141e2aa8d98263c690c0d72eed52d07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b
    07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b
server encryption key pair:
    5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb
    de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f

--- Log entries, microsecond time ----

000000    42 -->   WRITE
    534376320100000000008520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a
001577   <--  38   READ
    020000000000de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f
009795   <-- 120   READ
    0600669544da0d2ec8a03766f53e0580bc3cc6cddb69b86e299a47a9b1f1c18666e5cf8b000742bad609bfd9bf2ef2798743ee092b07eb329899ab741476448b5f34e6513e1d3cec7469fbf03112a098acd397ab933c61a2319eb6e0b4561ed9ce010d998f5bc10d6d17f88cebf961d1377faccc8a781c2c
010933   120 -->   WRITE
    0600a342f9538471d266100bfc3b9e794f40b32ffd053d58a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb6856903f7f1006e43d7e21915f72e729a26bf6bc5f59bc7ed2e1456a8a5fc9ecc6e2cd3c48e0103769ccd6faa87e45b8b256207a2e341cd068d433c7296fb374
010933    30 -->   WRITE_WITH_PREVIOUS
    06005089769da0def9f37289f9e5ff6e78710b9747d8a0971591abf2e4fb
011272   <--  30   READ
    060082eb9d3660b82984f3c1c1051f8751ab5585b7d0ad354d9b5c56f755

---- Other ----

session key: 1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389
app request:  010505050505
app response: 010505050505
total bytes: 380
total bytes, handshake only: 320

*/

