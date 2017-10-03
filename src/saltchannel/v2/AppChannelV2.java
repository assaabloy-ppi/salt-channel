package saltchannel.v2;

import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.util.TimeChecker;
import saltchannel.util.TimeKeeper;
import saltchannel.v2.packets.AppPacket;
import saltchannel.v2.packets.M4Packet;

/**
 * An app message channel on top of an underlying ByteChannel (EncryptedChannelV2).
 * Adds small header to messages.
 */
public class AppChannelV2 implements ByteChannel {
    private ByteChannel channel;
    private TimeKeeper timeKeeper;
    private TimeChecker timeChecker;
    private M4Packet bufferedM4 = null;
    
    public AppChannelV2(ByteChannel channel, TimeKeeper timeKeeper, TimeChecker timeChecker) {
        this.channel = channel;
        this.timeKeeper = timeKeeper;
        this.timeChecker = timeChecker;
    }

    @Override
    public byte[] read() throws ComException {
        byte[] bytes = channel.read();
        AppPacket p = AppPacket.fromBytes(bytes, 0, bytes.length);
        timeChecker.checkTime(p.time);
        return p.appData;
    }
    
    @Override
    public void write(byte[]... messages) throws ComException {
        write(false, messages);
    }
    
    @Override
    public void write(boolean isLast, byte[]... messages) throws ComException {
        byte[][] appMessages;
        int firstIndex;
        
        if (this.bufferedM4 == null) {
            appMessages = new byte[messages.length][];
            firstIndex = 0;
        } else {
            appMessages = new byte[1 + messages.length][];
            this.bufferedM4.time = timeKeeper.getTime();
            appMessages[0] = this.bufferedM4.toBytes();
            firstIndex = 1;
        }
        
        for (int i = firstIndex; i < appMessages.length; i++) {
            AppPacket p = new AppPacket();
            p.appData = messages[i - firstIndex];
            p.time = timeKeeper.getTime();
            appMessages[i] = new byte[p.getSize()];
            p.toBytes(appMessages[i], 0);
        }
        
        channel.write(isLast, appMessages);
    }
    
    /**
     * Used by framework to set M4, so M4 can be sent together with 
     * first application messages.
     */
    void setBufferedM4(M4Packet m4) {
        this.bufferedM4 = m4;
    }
}
