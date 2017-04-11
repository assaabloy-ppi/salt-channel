package saltchannel;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;

import saltaa.*;

/**
 * Salt Channel crypto lib, a thin layer on top of ....
 * Note: implementation is moving from stateful object to a set of functions (static).
 * 
 * @author Frans Lundberg
 */
public class CryptoLib {
    //public static final int SIGN_PUBLIC_KEY_BYTES = TweetNaCl.SIGN_PUBLIC_KEY_BYTES;
    //public static final int SIGNATURE_SIZE_BYTES = TweetNaCl.SIGNATURE_SIZE_BYTES;    

    private Rand rand;
    private static SaltLib salt = SaltLibFactory.getLib(SaltLibFactory.LibType.JAVA);
    
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
    
    // [passed: JAVA + NATIVE]
    public static KeyPair createEncKeys(Rand rand) {
        byte[] sec = new byte[SaltLib.crypto_box_SECRETKEYBYTES];
        byte[] pub = new byte[SaltLib.crypto_box_PUBLICKEYBYTES];
        rand.randomBytes(sec);
        salt.crypto_box_keypair_not_random(pub, sec);
        return new KeyPair(sec, pub);
    }
    
    /**
     * @deprecated Use function instead.
     */
    // [passed: JAVA + NATIVE]
    public KeyPair createEncKeys() {
        byte[] sec = new byte[SaltLib.crypto_box_SECRETKEYBYTES];
        byte[] pub = new byte[SaltLib.crypto_box_PUBLICKEYBYTES];
        this.rand.randomBytes(sec);
        salt.crypto_box_keypair_not_random(pub, sec);
        return new KeyPair(sec, pub);
    }
    
    /**
     * Creates a random signing KeyPair.
     */
    // [passed: JAVA + NATIVE ]    
    public static KeyPair createSigKeys(Rand rand) {
        byte[] sec = new byte[SaltLib.crypto_sign_SECRETKEYBYTES];
        byte[] pub = new byte[SaltLib.crypto_sign_PUBLICKEYBYTES];
        rand.randomBytes(sec);
        salt.crypto_sign_keypair_not_random(pub, sec);
        return new KeyPair(sec, pub);
    }
    
    /**
     * Creates a deterministic signing KeyPair given the secret key.
     */
    // [passed: JAVA + NATIVE]
    public static KeyPair createSigKeysFromSec(byte[] sec) {
        byte[] pub = new byte[SaltLib.crypto_sign_PUBLICKEYBYTES];
        salt.crypto_sign_keypair_not_random(pub, sec);
        return new KeyPair(sec, pub);
    }
    
    /**
     * Signs a message using TweetNaCl signing.
     */
    // [passed: JAVA + NATIVE]    
    public static byte[] sign(byte[] messageToSign, byte[] sigSecKey) {
        byte[] sm = new byte[messageToSign.length + SaltLib.crypto_sign_BYTES];
        salt.crypto_sign(sm, messageToSign, sigSecKey);
        return sm;
    }

    /**
     * Opens a signed messages, returns message.
     *
     * @throws BadSignatureException
     */
    // [passed: JAVA + NATIVE]        
    public static byte[] signOpen(byte[] signedMsg, byte[] sigSecKey) {
        byte[] m = new byte[signedMsg.length];
        byte[] m2 = new byte[signedMsg.length - SaltLib.crypto_sign_BYTES];
        salt.crypto_sign_open(m, signedMsg, sigSecKey);
        System.arraycopy(m, 0, m2, 0, signedMsg.length-SaltLib.crypto_sign_BYTES);
        return m2;
    }

    /**
     * Computes SHA-512 of message.
     */
    // [passed: JAVA + NATIVE]    
    public static byte[] sha512(byte[] message) {
        byte[] hash = new byte[SaltLib.crypto_hash_BYTES];
        salt.crypto_hash(hash, message);
        return hash;
    }

    // [passed: JAVA + NATIVE]  
     public static byte[] computeSharedKey(byte[] myPriv, byte[] peerPub) {
        if (myPriv.length != SaltLib.crypto_box_SECRETKEYBYTES) {
            throw new IllegalArgumentException("bad length of myPriv, " + myPriv.length);
        }
        
        if (peerPub.length != SaltLib.crypto_box_PUBLICKEYBYTES) {
            throw new IllegalArgumentException("bad length of peerPub, " + peerPub.length);
        }
        
        byte[] sharedKey = new byte[SaltLib.crypto_box_SHAREDKEYBYTES];
        salt.crypto_box_beforenm(sharedKey, peerPub, myPriv);
        return sharedKey;
    }
    
    // [passed: JAVA + NATIVE]      
    public static byte[] createSaltChannelV1Signature(KeyPair sigKeyPair, byte[] myEk, byte[] peerEk) {
        byte[] secretSigningKey = sigKeyPair.sec();
        
        if (secretSigningKey.length != SaltLib.crypto_sign_SECRETKEYBYTES) {
            throw new IllegalArgumentException("bad signing key length, " + secretSigningKey.length);
        }
        
        byte[] messageToSign = new byte[2 * SaltLib.crypto_box_PUBLICKEYBYTES];
        System.arraycopy(myEk, 0, messageToSign, 0, myEk.length);
        System.arraycopy(peerEk, 0, messageToSign, myEk.length, peerEk.length);
        
        byte[] signedMessage = new byte[messageToSign.length + SaltLib.crypto_sign_BYTES];
        salt.crypto_sign(signedMessage, messageToSign, secretSigningKey);

        byte[] mySignature = new byte[SaltLib.crypto_sign_SIGNATUREBYTES];
        System.arraycopy(signedMessage, 0, mySignature, 0, mySignature.length);
        
        return mySignature;
    }

    /**
     * Checks a signature. peerEk and myEk concatenated is the message that was signed.
     * 
     * @throws ComException if signature not valid.
     */
    // [passed: JAVA + NATIVE]     
    public static void checkSaltChannelV1Signature(byte[] peerSigPubKey, byte[] myEk,
            byte[] peerEk, byte[] signature) {
        // To use NaCl's crypto_sign_open, we create 
        // a signed message: signature+message concatenated.
        
        byte[] signedMessage = new byte[SaltLib.crypto_sign_SIGNATUREBYTES + 2 * SaltLib.crypto_box_PUBLICKEYBYTES];
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
             byte[] m = new byte[signedMessage.length];
             salt.crypto_sign_open(m, signedMessage, peerSigPubKey);
        } catch (BadSignatureException e) {
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

    // [passed: JAVA + NATIVE]   
    public static byte[] encrypt(byte[] key, byte[] nonce, byte[] clear) {
        byte[] m = new byte[SaltLib.crypto_box_INTERNALOVERHEADBYTES + clear.length];
        byte[] c = new byte[m.length];
        System.arraycopy(clear, 0, m, SaltLib.crypto_box_INTERNALOVERHEADBYTES, clear.length);
        salt.crypto_box_afternm(c, m, nonce, key);
        return Arrays.copyOfRange(c, SaltLib.crypto_box_OVERHEADBYTES, c.length);
    }
    
    /**
     * @throws ComException
     */
    // [passed: JAVA + NATIVE]       
    public static byte[] decrypt(byte[] key, byte[] nonce, byte[] encrypted) {
        if (encrypted == null) {
            throw new Error("encrypted == null");
        }
        
        byte[] clear;
        byte[] c = new byte[SaltLib.crypto_box_OVERHEADBYTES + encrypted.length];
        byte[] m = new byte[c.length];
        System.arraycopy(encrypted, 0, c, SaltLib.crypto_box_OVERHEADBYTES, encrypted.length);
        if (c.length < 32) {
            throw new ComException("ciphertext too small");
        }
        
        try {            
            salt.crypto_box_open_afternm(m, c, nonce, key);
        }
        catch (BadEncryptedDataException e)
        {
          throw new ComException("invalid encryption");
        }
        
        clear = Arrays.copyOfRange(m, SaltLib.crypto_box_INTERNALOVERHEADBYTES, m.length);
        return clear;
    }
}
