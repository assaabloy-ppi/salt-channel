package saltchannel.dev;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import saltchannel.ByteChannel;
import saltchannel.Tunnel;
import saltchannel.util.CryptoTestData;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;
import saltchannel.util.TimeKeeper;
import saltchannel.v2.SaltClientSession;
import saltchannel.v2.SaltServerSession;

/**
 * Example session data 3; handshake, echo with AppPacket, 
 * echo+close with MultiAppPacket.
 * 
 * @author Frans Lundberg
 */
public class ExampleSession3 {
    private Tunnel tunnel;
    private KeyPair clientSigKeyPair;
    private KeyPair clientEncKeyPair;
    private KeyPair serverSigKeyPair;
    private KeyPair serverEncKeyPair;
    private LoggingByteChannel loggingByteChannel;
    private byte[] sessionKey;
    
    public static void main(String[] args) {
        new ExampleSession3().go();
    }
    
    public ExampleSession3() {
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
        session.setTimeKeeper(new OneTwoThreeTimeKeeper());
        session.handshake();
        ByteChannel appChannel = session.getChannel();
        
        byte[] data = appChannel.read();
        appChannel.write(false, data);    // echo once
        
        byte[] data1 = appChannel.read();
        byte[] data2 = appChannel.read();
        appChannel.write(true, data1, data2);    // echo two application messages back, lastFlag is true
    }
    
    private void runClient() {
        loggingByteChannel = new LoggingByteChannel(tunnel.channel1());
        SaltClientSession session = new SaltClientSession(clientSigKeyPair, loggingByteChannel);
        session.setEncKeyPair(clientEncKeyPair);
        session.setTimeKeeper(new OneTwoThreeTimeKeeper());
        session.handshake();
        ByteChannel appChannel = session.getChannel();
        
        appChannel.write(false, new byte[]{0x01, 0x05, 0x05, 0x05, 0x05, 0x05});
        byte[] response1 = appChannel.read();
        appChannel.write(false, 
                new byte[]{0x01, 0x04, 0x04, 0x04, 0x04}, 
                new byte[]{0x03, 0x03, 0x03, 0x03});
        byte[] response2 = appChannel.read();
        byte[] response3 = appChannel.read();
        
        if (!Arrays.equals(response1, new byte[]{0x01, 0x05, 0x05, 0x05, 0x05, 0x05})) {
            throw new AssertionError("response1 does not match expected");
        }
        
        if (!Arrays.equals(response2, new byte[]{0x01, 0x04, 0x04, 0x04, 0x04})) {
            throw new AssertionError("response2 does not match expected");
        }
        
        if (!Arrays.equals(response3, new byte[]{0x03, 0x03, 0x03, 0x03})) {
            throw new AssertionError("response3 does not match expected");
        }
        
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
        b.append("1. Handshake.\n");
        b.append("2. Client sends: 010505050505 in AppPacket and server echos the same data back.\n");
        b.append("3. Client sends the two application messages: 0104040404, 03030303\n");
        b.append("   in a MultiAppPacket and Server echos the same two messages back in\n");
        b.append("   a MultiAppPacket.\n");
        b.append("\n");
        b.append("Time fields are used. Each peer sends 1 in the first message, then 2, 3, ...\n");
        b.append("Thus the times fields are as follows: \n");
        b.append("    M1: 1 (client --> server)\n"); 
        b.append("    M2: 1 (client <-- server)\n");
        b.append("    M3: 2 (client <-- server)\n");
        b.append("    M4: 2 (client --> server)\n");
        b.append("    AppPacket: 3 (client --> server)\n");
        b.append("    AppPacket: 3 (client <-- server)\n");
        b.append("    MultiAppPacket: 4 (client --> server)\n");
        b.append("    MultiAppPacket: 4 (client <-- server)\n");
        b.append("The lastFlag is used by Server in the last message it sends.\n");
        b.append("0x01 means ECHO command, and 0x03 means CLOSE command.\n");
        b.append("\n");
        b.append("---- key pairs, secret key first ----\n");
        b.append("\n");
        b.append(keyPairString());
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
        b.append("session key: " + Hex.create(sessionKey) + "\n");
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
    
    /**
     * TimeKeeper that returns 1, 2, 3, .. as the time value.
     */
    static class OneTwoThreeTimeKeeper implements TimeKeeper {
        int time = 0;
    
        public int getFirstTime() {
            time = 1;
            return 1;
        }

        public int getTime() {
            time = time + 1;
            return time;
        }
    }
}

/*

OUTPUT 2017-11-01
=================

Output with TimeKeeper stuff updated.

======== ExampleSession3 ========

Example session data for Salt Channel v2.

1. Handshake.
2. Client sends: 010505050505 in AppPacket and server echos the same data back.
3. Client sends the two application messages: 0104040404, 03030303
   in a MultiAppPacket and Server echos the same two messages back in
   a MultiAppPacket.

Time fields are used. Each peer sends 1 in the first message, then 2, 3, ...
Thus the times fields are as follows: 
    M1: 1 (client --> server)
    M2: 1 (client <-- server)
    M3: 2 (client <-- server)
    M4: 2 (client --> server)
    AppPacket: 3 (client --> server)
    AppPacket: 3 (client <-- server)
    MultiAppPacket: 4 (client --> server)
    MultiAppPacket: 4 (client <-- server)
The lastFlag is used by Server in the last message it sends.
0x01 means ECHO command, and 0x03 means CLOSE command.

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

--- Log entries ----

 42 -->   WRITE
    534376320100010000008520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a
<--  38   READ
    020001000000de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f
<-- 120   READ
    06005f545037bc60f771254bb562a5545193c6cdd969b86e299a47a9b1f1c18666e5cf8b000742bad609bfd9bf2ef2798743ee092b07eb32f55c386d4c5f986a22a793f2886c407756e9c16f416ad6a039bec1f546c28e53e3cdd8b6a0b728e1b576dc73c0826fde10a8e8fa95dd840f27887fad9c43e523
120 -->   WRITE
    06002541b8476e6f38c121f9f4fb63d99c09b32fff053d58a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb93170c3dd24c413625f3a479a4a3aeef72b78938dd6342954f6c5deaa6046a2558dc4608c8eea2e95eee1d70053428193ab4b89efd6c6d731fe89281ffe7557f
 30 -->   WRITE
    0600fc874e03bdcfb575da8035aef06178ac0b9744d8a0971591abf2e4fb
<--  30   READ
    060045bfb5a275a3d9e175bfb1acf36cc10a5585b4d0ad354d9b5c56f755
 39 -->   WRITE
    060051f0396cdadf6e74adb417b715bf3e93cc27e6aef94d2852fd4229970630df2c34bb76ec4c
<--  39   READ
    06808ab0c2c5e3a660e3767d28d4bc0fda2d23fd515aaef131889c0a4b4b3ce8ccefcd95c2c5b9

---- Other ----

session key: 1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389
total bytes: 458
total bytes, handshake only: 320


*/

