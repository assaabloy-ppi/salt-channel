package saltchannel.v2.packets;

import java.io.UnsupportedEncodingException;
import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of A2 message, low-level serialization / deserialization.
 * 
 * @author Frans Lundberg
 */
public class A2Packet implements Packet {
    public static final int PACKET_TYPE = 9;
    public Prot[] prots;
    
    public int getSize() {
        return 1 + 1 + prots.length * (2 * Prot.P_SIZE);
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (prots == null) {
            throw new IllegalStateException("prots == null not allowed");
        }
        
        if (prots.length > 127) {
            throw new IllegalStateException("too many Prot elements, " + prots.length);
        }
        
        Serializer s = new Serializer(destination, offset);
        s.writeUint4(PACKET_TYPE);
        s.writeBit(1);    // close == true
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        
        s.writeByte(prots.length);
        
        for (int i = 0; i < prots.length; i++) {
            Prot prot = prots[i];
            byte[] p1;
            try {
                p1 = prot.p1.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new Error("should not happen", e);
            }
            
            byte[] p2;
            try {
                p2 = prot.p2.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new Error("should not happen", e);
            }
            
            if (p1.length != Prot.P_SIZE) {
                throw new IllegalStateException("bad p1 size");
            }
            
            if (p2.length != Prot.P_SIZE) {
                throw new IllegalStateException("bad p2 size");
            }
        }
    }
    
    public static A2Packet fromBytes(byte[] source, int offset) {
        A2Packet p = new A2Packet();
        Deserializer d = new Deserializer(source, offset);
        
        int packetType = d.readUint4();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        boolean close = d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        if (!close) {
            throw new BadPeer("close flag must be set");
        }
        
        int count = d.readByte();
        if (count < 1) {
            throw new BadPeer("bad Count, was " + count);
        }
        
        p.prots = new Prot[count];
        
        for (int i = 0; i < count; i++) {
            Prot prot = new Prot();
            prot.p1 = d.readString(Prot.P_SIZE);
            prot.p2 = d.readString(Prot.P_SIZE);            
            p.prots[i] = prot;
        }
        
        return p;
    }
    
    /**
     * A2/Prot, two protocol indicators, each exactly 10 ASCII characters long.
     */
    public static class Prot {
        public static final int P_SIZE = 10;
        public String p1;
        public String p2;
        
        public Prot() {
        }
        
        public Prot(String p1, String p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }
}
