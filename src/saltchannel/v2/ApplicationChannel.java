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
 * An application message channel on top of an underlying ByteChannel (EncryptedChannelV2).
 * Adds a small header to messages (2-bytes header + time).
 * Also, this class decides how to encode application messages 
 * using AppPacket or MultiAppPacket.
 * 
 * @author Frans Lundberg
 */
public class ApplicationChannel implements ByteChannel {
    private ByteChannel channel;
    private TimeKeeper timeKeeper;
    private TimeChecker timeChecker;
    private M4Packet bufferedM4 = null;
    private LinkedBlockingQueue<byte[]> readQ;
    private PacketHeader lastReadHeader;
    
    public ApplicationChannel(ByteChannel channel, TimeKeeper timeKeeper, TimeChecker timeChecker) {
        this.channel = channel;
        this.timeKeeper = timeKeeper;
        this.timeChecker = timeChecker;
        this.readQ = new LinkedBlockingQueue<byte[]>();
        this.lastReadHeader = null;
    }

    @Override
    public byte[] read() throws ComException {
        if (readQ.size() > 0) {
            try {
                return readQ.take();
            } catch (InterruptedException e) {
                throw new Error("should not happen, size is > 0");
            }
        }
        
        byte[] bytes = channel.read();
        PacketHeader header = new PacketHeader(bytes, 0);
        this.lastReadHeader = header;
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
     * Returns the number of remaining application messages left to
     * read in the buffer. This is the same as the number of further message
     * of an MultiAppPacket that are buffered by this implementation.
     */
    public int available() {
        return readQ.size();
    }
    
    /**
     * Returns true if the last packet read with read() is the last
     * message of the application session.
     */
    public boolean isLast() {
        if (lastReadHeader == null) {
            return false;
        }
        
        return available() == 0 && lastReadHeader.lastFlag() == true;
    }
    
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
