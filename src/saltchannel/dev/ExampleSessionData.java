package saltchannel.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import saltchannel.ByteChannel;
import saltchannel.ComException;
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
    }
    
    public void outputResult() {
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
            
            b.append(formattedTime + "   " + sizeAndArrowString + "   " + entry.type.name() + "\n");
            b.append("    " + Hex.create(entry.bytes) + "\n");
        }
        
        b.append("\n");
        b.append("---- Other ----\n");
        b.append("\n");
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

    /**
     * Decorator for ByteChannel, stores read and written packages.
     */
    public static class LoggingByteChannel implements ByteChannel {
        private ByteChannel inner;
        private ArrayList<LoggingByteChannel.Entry> log;

        public LoggingByteChannel(ByteChannel channel) {
            this.log = new ArrayList<>();
            this.inner = channel;
        }
        
        public List<Entry> getLog() {
            return log;
        }
        
        public static enum ReadOrWrite {
            READ, WRITE, WRITE_WITH_PREVIOUS
        }
        
        public static class Entry {
            /** Time in nanos (from System.nanoTime()). */
            public long time;
            
            public ReadOrWrite type;
            
            public byte[] bytes;
        }

        @Override
        public byte[] read() throws ComException {
            byte[] result = inner.read();
            
            Entry entry = new Entry();
            entry.time = System.nanoTime();
            entry.type = ReadOrWrite.READ;
            entry.bytes = result;
            log.add(entry);
            
            return result;
        }

        @Override
        public void write(byte[]... messages) throws ComException {
            inner.write(messages);
            
            long time = System.nanoTime();
            
            for (int i = 0; i < messages.length; i++) {
                Entry entry = new Entry();
                entry.time = time;
                entry.type = i == 0 ? ReadOrWrite.WRITE : ReadOrWrite.WRITE_WITH_PREVIOUS;
                entry.bytes = messages[i];
                log.add(entry);
            }
        }
    }
}
