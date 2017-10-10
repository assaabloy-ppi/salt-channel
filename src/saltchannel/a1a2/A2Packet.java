package saltchannel.a1a2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;
import saltchannel.v2.packets.Packet;
import saltchannel.v2.packets.PacketHeader;

/**
 * Data of A2 message, low-level serialization / deserialization.
 * 
 * @author Frans Lundberg
 */
public class A2Packet implements Packet {
    public static final int PACKET_TYPE = 9;
    public static final int PROT_STRING_SIZE = Prot.P_SIZE;
    public static final String SC2_PROT_STRING = "SC2-------";
    public static final String UNSPECIFIED_PROT_STRING = "----------";
    
    public Prot[] prots;
    public boolean noSuchServer = false;

    /**
     * Returns protocol specification items, "Prots".
     */
    public Prot[] getProts() {
        return prots;
    }
    
    /**
     * Returns message type.
     */
    public int getType() {
        return PACKET_TYPE;
    }
    
    /**
     * Returns byte size of message.
     */
    public int getSize() {
        return PacketHeader.SIZE + 1 + prots.length * (2 * Prot.P_SIZE);
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (prots == null) {
            throw new IllegalStateException("prots == null not allowed");
        }
        
        if (prots.length > 127) {
            throw new IllegalStateException("too many Prot elements, " + prots.length);
        }
        
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        header.setBit(7, true);   // LastFlag
        header.setBit(0, this.noSuchServer);
        
        s.writeHeader(header);
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
            
            s.writeBytes(p1);
            s.writeBytes(p2);
        }
    }
    
    /**
     * @throws BadPeer
     *          If data is not formatted correctly.
     */
    public static A2Packet fromBytes(byte[] source, int offset) {
        A2Packet p = new A2Packet();
        Deserializer d = new Deserializer(source, offset);
        
        PacketHeader header = d.readHeader();
        int packetType = header.getType();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        boolean lastFlag = header.getBit(7);
        p.noSuchServer = header.getBit(0);
        
        if (!lastFlag) {
            throw new BadPeer("LastFlag must be set");
        }
        
        int count = d.readByte();
        if (count < 0) {
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
        private String p1;
        private String p2;
        
        public Prot() {
        }
        
        public Prot(String p1, String p2) {
            check(p1);
            check(p2);
            this.p1 = p1;
            this.p2 = p2;
        }
        
        public String p1() {
            return p1;
        }
        
        public String p2() {
            return p2;
        }
        
        public String toString() {
            return p1 + "/" + p2;
        }
        
        public static boolean isAllowed(char c) {
            // Letters, digits, dash, underscore or period.
            // As defined in Salt Channel v2 spec.
            
            return (c >= 'A' && c <= 'Z') 
                || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '-' || c == '_' || c == '.';
        }
        
        /**
         * Checks validity of a P-string.
         * 
         * @throws IllegalArgumentException if p is not valid.
         */
        public static void check(String p) {
            if (p == null) {
                throw new IllegalArgumentException("p-string == null not allowed");
            }
            
            if (p.length() != P_SIZE) {
                throw new IllegalArgumentException("p-string length not 10, was " + p.length());
            }
            
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                if (!isAllowed(c)) {
                    throw new IllegalArgumentException("illegal char in p, '" + p + "'");
                }
            }
        }
    }
    
    public static class Builder {
        private String p1 = SC2_PROT_STRING;
        private ArrayList<Prot> prots = new ArrayList<Prot>();
        
        /**
         * This must be SC2_PROT_STRING for Salt Channel v2.
         * This is also the default value if this method is not called.
         */
        public Builder saltChannelProt(String p1) {
            Prot.check(p1);
            this.p1 = p1;
            return this;
        }
        
        /**
         * Adds subprotocol 'p2' to the declared available protocols of the server.
         */
        public Builder prot(String p2) {
            prots.add(new Prot(p1, p2));
            return this;
        }
        
        public A2Packet build() {
            if (prots.size() == 0) {
                prot(UNSPECIFIED_PROT_STRING);
            }
            
            A2Packet a2 = new A2Packet();
            a2.prots = new A2Packet.Prot[prots.size()];
            for (int i = 0; i < a2.prots.length; i++) {
                a2.prots[i] = prots.get(i);
            }
            
            return a2;
        }
    }
}
