package saltchannel.v2;

import java.util.concurrent.LinkedBlockingQueue;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.util.TimeChecker;
import saltchannel.util.TimeKeeper;
import saltchannel.v2.packets.AppPacket;
import saltchannel.v2.packets.M4Packet;
import saltchannel.v2.packets.MultiAppPacket;
import saltchannel.v2.packets.Packet;
import saltchannel.v2.packets.PacketHeader;

/**
 * A message channel for the application layer to use after a successful
 * handshake has been completed.
 * 
 * The channel works on top of an underlying byte channel (an EncryptedChannelV2).
 * It adds a small header to the messages (2-bytes header + time).
 * Also, this class decides how to encode application messages 
 * using either AppPacket or MultiAppPacket.
 * 
 * @author Frans Lundberg
 */
public class ApplicationChannel implements ByteChannel {
    private ByteChannel channel;
    private TimeKeeper timeKeeper;
    private TimeChecker timeChecker;
    private M4Packet bufferedM4 = null;
    private LinkedBlockingQueue<byte[]> readQ;
    private boolean readLast = false;
    private EncryptedChannelV2 encryptedChannel;
    
    public ApplicationChannel(ByteChannel channel, TimeKeeper timeKeeper, TimeChecker timeChecker) {
        this.channel = channel;
        if (channel instanceof EncryptedChannelV2) {
            encryptedChannel = (EncryptedChannelV2) channel;
        } else {
            encryptedChannel = null;
        }
        
        this.timeKeeper = timeKeeper;
        this.timeChecker = timeChecker;
        this.readQ = new LinkedBlockingQueue<byte[]>();
    }

    @Override
    public byte[] read() throws ComException {
        //
        // Note, APP_PACKET and TYPE_MULTI_APP_PACKET do not contain the 
        // lastFlag; it is included in ENCRYPTED_MESSAGE.
        //
        
        if (readQ.size() > 0) {
            try {
                return readQ.take();
            } catch (InterruptedException e) {
                throw new Error("should not happen, size is > 0");
            }
        }
        
        byte[] bytes = channel.read();
        if (encryptedChannel != null) {
            this.readLast = encryptedChannel.lastFlag();
        }
        
        PacketHeader header = new PacketHeader(bytes, 0);
        int type = header.getType();
        byte[] result;
        
        if (type == Packet.TYPE_APP_PACKET) {
            AppPacket p = AppPacket.fromBytes(bytes, 0, bytes.length);
            timeChecker.checkTime(p.time);
            result = p.appData;
        } else if (type == Packet.TYPE_MULTI_APP_PACKET) {
            MultiAppPacket multi = MultiAppPacket.fromBytes(bytes, 0, bytes.length);
            timeChecker.checkTime(multi.time);
            int count = multi.appMessages.length;
            result = multi.appMessages[0];
            for (int i = 1; i < count; i++) {
                readQ.add(multi.appMessages[i]);
            }
        } else {
            throw new BadPeer("unexpected message type, " + type 
                    + ", expected AppPacket or MultiAppPacket");
        }
        
        return result;
    }
    
    /**
     * Returns the number of remaining application buffered application
     * messages. This is the same as the number of further messages
     * of an MultiAppPacket that are buffered by this implementation.
     */
    public int availableFromMultiAppPacket() {
        return readQ.size();
    }
    
    /**
     * Returns true if the last packet read with read() is the last
     * batch of messages of the application session.
     * If available() returns 0, the last message of the session was read.
     */
    public boolean lastFlag() {
        return this.readLast;
    }
    
    /**
     * @deprecated Deprecated from 2017-10-31, use write(isLast, messages) instead.
     */
    @Override
    public void write(byte[]... messages) throws ComException {
        write(false, messages);
    }
    
    @Override
    public void write(boolean isLast, byte[]... messages) throws ComException {
        // 
        // * Adds application header (AppPacket/MultiAppPacket).
        // * Adds (prepends) buffered M4 if needed.
        // * Writes to underlying layer (EncryptedChannelV2).
        //
        // messages:  input application messages
        // messages2: application messages with AppPacket/MultiAppPacket headers.
        // messages3: output messages to EncryptedChannelV2 layer, possible with buffered M4.
        //
        
        byte[][] messages2;
        byte[][] messages3;
        int currentTime = timeKeeper.getTime();
        
        boolean useMulti = MultiAppPacket.shouldUse(messages);
        if (useMulti) {
            messages2 = new byte[1][];
            MultiAppPacket multi = new MultiAppPacket();
            multi.appMessages = messages;
            multi.time = currentTime;
            messages2[0] = new byte[multi.getSize()];
            multi.toBytes(messages2[0], 0);
        } else {
            messages2 = new byte[messages.length][];
            for (int i = 0; i < messages.length; i++) {
                AppPacket p = new AppPacket();
                p.appData = messages[i];
                p.time = currentTime;
                messages2[i] = new byte[p.getSize()];
                p.toBytes(messages2[i], 0);
            }
        }
        
        if (this.bufferedM4 == null) {
            messages3 = messages2;
        } else {
            messages3 = new byte[1 + messages2.length][];
            this.bufferedM4.time = currentTime;
            messages3[0] = this.bufferedM4.toBytes();
            System.arraycopy(messages2, 0, messages3, 1, messages2.length);
            this.bufferedM4 = null;
        }
        
        channel.write(isLast, messages3);
    }
    
    /**
     * Used by framework to set M4, so M4 can be sent together with 
     * first application messages.
     */
    void setBufferedM4(M4Packet m4) {
        this.bufferedM4 = m4;
    }
}
