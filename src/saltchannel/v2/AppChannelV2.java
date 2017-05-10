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
    private M4Packet buffered = null;
    
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
    public void write(byte[]... messages1) throws ComException {
        byte[][] appMessages;
        int firstIndex;
        
        if (this.buffered == null) {
            appMessages = new byte[messages1.length][];
            firstIndex = 0;
        } else {
            appMessages = new byte[1 + messages1.length][];
            this.buffered.time = timeKeeper.getTime();
            appMessages[0] = this.buffered.toBytes();
            firstIndex = 1;
        }
        
        for (int i = firstIndex; i < appMessages.length; i++) {
            AppPacket p = new AppPacket();
            p.appData = messages1[i - firstIndex];
            p.time = timeKeeper.getTime();
            appMessages[i] = new byte[p.getSize()];
            p.toBytes(appMessages[i], 0);
        }
        
        channel.write(appMessages);
    }
    
    /**
     * Used by framework to set M4, so M4 can be sent together with 
     * first application mesages.
     */
    void setBufferedMessage(M4Packet m4) {
        this.buffered = m4;
    }
}
