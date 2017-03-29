package saltchannel.v2;

import java.util.Arrays;

import saltchannel.ComException;
import saltchannel.CryptoLib;
import saltchannel.util.Bytes;
import saltchannel.v2.packets.BadTicket;
import saltchannel.v2.packets.TicketEncryptedPacket;
import saltchannel.v2.packets.TicketPacket;

/** 
 * Handles the resume feature on the server-side.
 * Stores a bit map to avoid replay attacks.
 * All data is stored in memory. First simple implementation.
 * 
 * @author Frans Lundberg
 */
public class ResumeHandler {
    public static final int KEY_SIZE = 32;
    private final TicketBits ticketBits;
    private byte[] key;
    
    /**
     * Creates a new instance with a given wanted capacity.
     * The capacity is the maximum number of resume tickets that can
     * be handled. The memory consumption is wantedSize/8 bytes 
     * plus overhead.
     */
    public ResumeHandler(byte[] key, long firstTicketIndex, int wantedBitSize) {
        if (wantedBitSize < 0) {
            throw new IllegalArgumentException("negative wantedSize not allowed");
        }
        
        this.ticketBits = new TicketBits(firstTicketIndex, wantedBitSize);
        
        if (key.length != KEY_SIZE) {
            throw new IllegalArgumentException("bad key size");
        }
        
        this.key = key.clone();
    }
    
    public synchronized byte[] issueTicket(byte[] clientSigKey, byte[] sessionKey) {
        TicketEncryptedPacket p1 = new TicketEncryptedPacket();
        p1.ticketId = ticketBits.issue();
        p1.sessionKey = sessionKey;
        p1.clientSigKey = clientSigKey;
        
        byte[] nonce24 = createNonce24(p1.ticketId);
        byte[] encrypted = CryptoLib.encrypt(key, nonce24, p1.toBytes());
        
        TicketPacket p2 = new TicketPacket();
        p2.ticketType = TicketPacket.TICKET_TYPE_1;
        p2.nonce = to10Bytes(nonce24);
        p2.encrypted = encrypted;
        
        byte[] totalBytes = p2.toBytes();
        
        return totalBytes;
    }

    /**
     * Validates ticket, decrypts, checks with bitmap, returns the resulting session data.
     * If the ticket is valid the corresponding replay-protection bit is cleared.
     * 
     * @throws BadTicket if the ticket is not valid.
     */
    public synchronized TicketSessionData validateTicket(byte[] ticket) {
        TicketPacket p1;
        TicketEncryptedPacket p2;
        
        try {
            p1 = TicketPacket.fromBytes(ticket);
        } catch (BadTicket e) {
            throw e;
        } catch (Exception e) {
            throw new BadTicket(e.getMessage());
        }
        
        byte[] clear;
        
        try {
            clear = CryptoLib.decrypt(key, to24Bytes(p1.nonce), p1.encrypted);
        } catch (ComException e) {
            throw new BadTicket("could not decrypt");
        }
        
        try {
            p2 = TicketEncryptedPacket.fromBytes(clear, 0);
        } catch (BadTicket e) {
            throw e;
        } catch (Exception e) {
            throw new BadTicket(e.getMessage());
        }
        
        TicketSessionData result = new TicketSessionData();
        result.clientSigKey = p2.clientSigKey;
        result.sessionKey = p2.sessionKey;
        result.ticketId = p2.ticketId;
        
        synchronized (this) {
            boolean isValid = ticketBits.isValid(p2.ticketId);
            if (!isValid) {
                throw new BadTicket("invalid according to ticketBits");
            }
            
            ticketBits.clear(p2.ticketId);
        }
        
        return result;
    }
    
    public static class InvalidTicket extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public InvalidTicket(String message) {
            super(message);
        }
    }
    
    /**
     * Creates 24-byte long nonce. Only the first 10 bytes are used (are non-zero).
     */
    private byte[] createNonce24(long ticketId) {
        byte[] result = new byte[24];
        result[0] = 120;
        result[1] = 121;
        Bytes.longToBytesLE(ticketId, result, 2);
        return result;
    }
    
    private byte[] to10Bytes(byte[] nonce24) {
        return Arrays.copyOfRange(nonce24, 0, 10);
    }
    
    private byte[] to24Bytes(byte[] nonce10) {
        byte[] result = new byte[24];
        System.arraycopy(nonce10, 0, result, 0, nonce10.length);
        return result;
    }
}
