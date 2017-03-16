package saltchannel;

import java.security.SecureRandom;
import java.util.Random;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;

/**
 * Salt Channel crypto lib, a thin layer on top of TweetNaCl.
 * Note: implementation is moving from stateful object to a set of functions (static).
 * 
 * @author Frans Lundberg
 */
public class CryptoLib {
    public static final int SIGN_PUBLIC_KEY_BYTES = TweetNaCl.SIGN_PUBLIC_KEY_BYTES;
    public static final int SIGNATURE_SIZE_BYTES = TweetNaCl.SIGNATURE_SIZE_BYTES;
    
    private Rand rand;
    
    private CryptoLib(Rand rand) {
        this.rand = rand;
    }
    
    public Rand getRand() {
        return rand;
    }

    /**
     * Creates an instance of the crypto lib with a 
     * secure random source.
     */
    public static CryptoLib createSecure() {
        return new CryptoLib(createSecureRand());
    }
    
    /**
     * Creates a crypto lib instance with an insecure random number generator; so
     * do not use for other things than testing.
     */
    public static CryptoLib createInsecureAndFast() {
        final Random random = new Random();
        
        Rand rand = new Rand() {
            public void randomBytes(byte[] b) {
                random.nextBytes(b);
            }
        };
        
        return new CryptoLib(rand);
    }

    /**
     * Creates a crypto lib instance from the given source of randomness.
     */
    public static CryptoLib create(Rand rand) {
        return new CryptoLib(rand);
    }
    
    public static Rand createSecureRand() {
        // Note, for Java 1.7 (currently supported by this code), "new SecureRandom()"
        // is good. Once 1.8 is allowed, use code like: "SecureRandom.getInstanceStrong()"
        // instead.
        
        final Random random = new SecureRandom();
        
        Rand rand = new Rand() {
            @Override
            public void randomBytes(byte[] b) {
                random.nextBytes(b);
            }
        };
        
        return rand;
    }

    public static Rand createInsecureAndFastRand() {
        final Random random = new Random();
        
        Rand rand = new Rand() {
            public void randomBytes(byte[] b) {
                random.nextBytes(b);
            }
        };
        
        return rand;
    }
    
    public static KeyPair createEncKeys(Rand rand) {
        byte[] sec = new byte[TweetNaCl.BOX_SECRET_KEY_BYTES];
        byte[] pub = new byte[TweetNaCl.BOX_PUBLIC_KEY_BYTES];
        rand.randomBytes(sec);
        boolean isSeeded = true;
        TweetNaCl.crypto_box_keypair(pub, sec, isSeeded);
        return new KeyPair(sec, pub);
    }
    
    /**
     * @deprecated Use function instead.
     */
    public KeyPair createEncKeys() {
        byte[] sec = new byte[TweetNaCl.BOX_SECRET_KEY_BYTES];
        byte[] pub = new byte[TweetNaCl.BOX_PUBLIC_KEY_BYTES];
        this.rand.randomBytes(sec);
        boolean isSeeded = true;
        TweetNaCl.crypto_box_keypair(pub, sec, isSeeded);
        return new KeyPair(sec, pub);
    }
    
    /**
     * Creates a random signing KeyPair.
     */
    public static KeyPair createSigKeys(Rand rand) {
        byte[] sec = new byte[TweetNaCl.SIGN_SECRET_KEY_BYTES];
        byte[] pub = new byte[TweetNaCl.SIGN_PUBLIC_KEY_BYTES];
        rand.randomBytes(sec);
        boolean isSeeded = true;
        TweetNaCl.crypto_sign_keypair(pub, sec, isSeeded);
        return new KeyPair(sec, pub);
    }
    
    /**
     * Creates a deterministic signing KeyPair given the secret key.
     */
    public static KeyPair createSigKeysFromSec(byte[] sec) {
        byte[] pub = new byte[TweetNaCl.SIGN_PUBLIC_KEY_BYTES];
        boolean isSeeded = true;

        //TweetNaCl.crypto_sign_keypair(pub, sec, isSeeded);
        // fix of bug #2. Now use deterministic version of KeyPair derivation (deriving from 3rd parameter)
        TweetNaCl.crypto_sign_seed_keypair(pub, sec, sec);  
        return new KeyPair(sec, pub);
    }
    
    /**
     * Signs a message using TweetNaCl signing.
     */
    public static byte[] sign(byte[] messageToSign, byte[] sigSecKey) {
        return TweetNaCl.crypto_sign(messageToSign, sigSecKey);
    }
    
    /**
     * Computes SHA-512 of message.
     */
    public static byte[] sha512(byte[] message) {
        byte[] hash = new byte[64];
        TweetNaCl.crypto_hash(hash, message, message.length);
        return hash;
    }

    public static byte[] computeSharedKey(byte[] myPriv, byte[] peerPub) {
        if (myPriv.length != TweetNaCl.BOX_SECRET_KEY_BYTES) {
            throw new IllegalArgumentException("bad length of myPriv, " + myPriv.length);
        }
        
        if (peerPub.length != TweetNaCl.BOX_PUBLIC_KEY_BYTES) {
            throw new IllegalArgumentException("bad length of peerPub, " + peerPub.length);
        }
        
        byte[] sharedKey = new byte[TweetNaCl.BOX_SHARED_KEY_BYTES];
        TweetNaCl.crypto_box_beforenm(sharedKey, peerPub, myPriv);
        return sharedKey;
    }
    
    public static byte[] createSaltChannelV1Signature(KeyPair sigKeyPair, byte[] myEk, byte[] peerEk) {
        byte[] secretSigningKey = sigKeyPair.sec();
        
        if (secretSigningKey.length != TweetNaCl.SIGN_SECRET_KEY_BYTES) {
            throw new IllegalArgumentException("bad signing key length, " + secretSigningKey.length);
        }
        
        byte[] messageToSign = new byte[2 * TweetNaCl.BOX_PUBLIC_KEY_BYTES];
        System.arraycopy(myEk, 0, messageToSign, 0, myEk.length);
        System.arraycopy(peerEk, 0, messageToSign, myEk.length, peerEk.length);
        
        byte[] signedMessage = TweetNaCl.crypto_sign(messageToSign, secretSigningKey);
        byte[] mySignature = new byte[TweetNaCl.SIGNATURE_SIZE_BYTES];
        System.arraycopy(signedMessage, 0, mySignature, 0, mySignature.length);
        
        return mySignature;
    }

    /**
     * Checks a signature. peerEk and myEk concatenated is the message that was signed.
     * 
     * @throws ComException if signature not valid.
     */
    public static void checkSaltChannelV1Signature(byte[] peerSigPubKey, byte[] myEk,
            byte[] peerEk, byte[] signature) {
        // To use NaCl's crypto_sign_open, we create 
        // a signed message: signature+message concatenated.
        
        byte[] signedMessage = new byte[TweetNaCl.SIGNATURE_SIZE_BYTES + 2 * TweetNaCl.BOX_PUBLIC_KEY_BYTES];
        int offset = 0;
        System.arraycopy(signature, 0, signedMessage, offset, signature.length);
        offset += signature.length;
        System.arraycopy(peerEk, 0, signedMessage, offset, peerEk.length);
        offset += peerEk.length;
        System.arraycopy(myEk, 0, signedMessage, offset, myEk.length);
        offset += myEk.length;
        
        if (offset != signedMessage.length) {
            throw new Error("bug, " + offset + ", " + signedMessage.length);
        }
        
        try {
            TweetNaCl.crypto_sign_open(signedMessage, peerSigPubKey);
        } catch (TweetNaCl.InvalidSignatureException e) {
            throw new InvalidSignature("invalid peer signature while doing handshake, "
                    + "peer's pub sig key=" + Hex.create(peerSigPubKey) + ", sm=" + Hex.create(signedMessage));
        }
    }
    
    public static class InvalidSignature extends ComException {
        private static final long serialVersionUID = 1L;

        public InvalidSignature(String message) {
            super(message);
        }
    };

}
