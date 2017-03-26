package saltchannel.v2;

import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.util.TimeKeeper;
import saltchannel.v2.packets.AppPacket;

/**
 * An app message channel on top of an underlying ByteChannel (EncryptedChannelV2).
 * Adds small header to messages.
 */
public class AppChannelV2 implements ByteChannel {
    private ByteChannel channel;
    private TimeKeeper timeKeeper;
    
    public AppChannelV2(ByteChannel channel, TimeKeeper timeKeeper) {
        this.channel = channel;
        this.timeKeeper = timeKeeper;
    }

    @Override
    public byte[] read() throws ComException {
        // TODO B. check p.time here!
        byte[] bytes = channel.read();
        AppPacket p = AppPacket.fromBytes(bytes, 0, bytes.length);
        return p.appData;
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        byte[][] appMessages = new byte[messages.length][];
        for (int i = 0; i < messages.length; i++) {
            AppPacket p = new AppPacket();
            p.appData = messages[i];
            p.time = timeKeeper.getTime();
            appMessages[i] = new byte[p.getSize()];
            p.toBytes(appMessages[i], 0);
        }
        
        channel.write(appMessages);
    }
}
