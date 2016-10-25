package saltchannel;

import java.security.SecureRandom;
import java.util.Random;

import saltchannel.util.ByteArrays;
import saltchannel.util.Hex;

/**
 * Thin layer on top of TweetNaCl.
 * Random byte generator can be injected. 
 * Provides some channel-related utility features.
 * The random generator must be replaceable; needed for faster tests
 * and for Android use.
 * 
 * @author Frans Lundberg
 */
public class TweetLib {
    public static final int SIGN_PUBLIC_KEY_BYTES = TweetNaCl.SIGN_PUBLIC_KEY_BYTES;
    private Rand rand;
    
    private TweetLib(Rand rand) {
        this.rand = rand;
    }

    /**
     * Creates an instance of the Tweet library with a 
     * secure random source.
     */
    public static TweetLib createSecure() {
        return new TweetLib(createSecureRand());
    }
    
    public static Rand createSecureRand() {
        final Random random;
        
        // Java 1.7:
        random = new SecureRandom();
        
        // Java 1.8:
        //try {
        //    random = SecureRandom.getInstanceStrong();
        //} catch (NoSuchAlgorithmException e) {
        //    throw new Error("could not create strong SecureRandom instance");
        //}
        
        Rand rand = new Rand() {
            @Override
            public void randomBytes(byte[] b) {
                random.nextBytes(b);
            }
        };
        
        return rand;
    }
    
    public static TweetLib create(Rand rand) {
        return new TweetLib(rand);
    }
    
    /**
     * Creates a fast lib, has an insecure random number generator; so
     * to not use it to create secure keys.
     */
    public static TweetLib createFast() {
        return new TweetLib(createRandFast());
    }
    
    public static Rand createRandFast() {
        final Random random = new Random();
        
        return new Rand() {
            @Override
            public void randomBytes(byte[] b) {
                random.nextBytes(b);
            }
        };
    }
    
    public KeyPair createEncKeys() {
        byte[] sec = new byte[TweetNaCl.BOX_SECRET_KEY_BYTES];
        byte[] pub = new byte[TweetNaCl.BOX_PUBLIC_KEY_BYTES];
        TweetNaCl.crypto_box_keypair_frans(pub, sec, rand);
        return new KeyPair(sec, pub);
    }
    
    public KeyPair createSigKeys() {
        byte[] sec = new byte[TweetNaCl.SIGN_SECRET_KEY_BYTES];
        byte[] pub = new byte[TweetNaCl.SIGN_PUBLIC_KEY_BYTES];
        TweetNaCl.crypto_sign_keypair_frans(pub, sec, rand);
        return new KeyPair(sec, pub);
    }
    
    /**
     * Creates a signing key pair from the secret signing key.
     * secretKey should be of size: 64 bytes.
     */
    public KeyPair signingKeys(String secretKeyAsHex) {
        return signingKeys(Hex.toBytes(secretKeyAsHex));
    }
    
    /**
     * Creates a signing key pair from the secret signing key.
     * secretKey should be of size: 64 bytes.
     */
    public KeyPair signingKeys(byte[] secretKey) {
        if (secretKey.length != TweetNaCl.SIGN_SECRET_KEY_BYTES) {
            throw new IllegalArgumentException("bad secretKey length");
        }
        
        byte[] publicKey = new byte[TweetNaCl.SIGN_PUBLIC_KEY_BYTES];
        TweetNaCl.crypto_sign_keypair(publicKey, secretKey, true);
        return new KeyPair(secretKey, publicKey);
    }

    public byte[] computeSharedKey(byte[] myPriv, byte[] peerPub) {
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
    
    /**
     * Returns an encrypted message. The first bytes is a random nonce.
     * 
     * @param fromEncKeyPair
     *          My encryption key pair.
     * @param toEncPubKey
     *          Receivers encryption public key.
     * @param message
     *          Message to encrypt.
     * @return The encrypted message with a nonce prefix.
     */
    public byte[] asymmetricEncrypt(KeyPair fromEncKeyPair, byte[] toEncPubKey, byte[] message) {
        byte[] nonce = new byte[TweetNaCl.BOX_NONCE_BYTES];
        this.rand.randomBytes(nonce);
        byte[] cipherText = TweetNaCl.crypto_box(message, nonce, toEncPubKey, fromEncKeyPair.sec());
        byte[] result = ByteArrays.concat(nonce, cipherText);
        return result;
    }
    
    /**
     * Decrypts a message encrypted with asymmetricEncrypt().
     * 
     * @throws InvalidCipherTextException
     */
    public byte[] asymmetricDecrypt(KeyPair toEncKeyPair, byte[] fromEncPubKey, byte[] encrypted) {
        byte[] nonce = ByteArrays.range(encrypted, 0, TweetNaCl.BOX_NONCE_BYTES);
        byte[] cipherText = ByteArrays.range(encrypted, TweetNaCl.BOX_NONCE_BYTES, encrypted.length);
        byte[] message = TweetNaCl.crypto_box_open(cipherText, nonce, fromEncPubKey, toEncKeyPair.sec());
        return message;
    }
    
    public byte[] sign(KeyPair sigKeyPair, byte[] message) {
        return TweetNaCl.crypto_sign(message, sigKeyPair.sec());
    }
    
    /**
     * Opens a signed message.
     * 
     * @throws TweetNaCl.InvalidSignatureException
     */
    public byte[] signOpen(byte[] fromPub, byte[] signed) {
        return TweetNaCl.crypto_sign_open(signed, fromPub);
    }
    
    public byte[] createSaltChannelSignature(KeyPair sigKeyPair, byte[] myEk, byte[] peerEk) {
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
    public void checkSaltChannelSignature(byte[] peerSigPubKey, byte[] myEk,
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
    
    /**
     * Interface for random number source.
     */
    public static interface Rand {
        
        /**
         * Sets the bytes in the array to random bytes.
         */
        public void randomBytes(byte[] b);
    }
    
    public static class InvalidSignature extends ComException {
        private static final long serialVersionUID = 1L;

        public InvalidSignature(String message) {
            super(message);
        }
    };
    
    public static void main(String[] args) {
        TweetLib lib = TweetLib.createSecure();
        KeyPair sigPair = lib.createSigKeys();
        
        System.out.println("---- Create sig keys ----");
        System.out.println("pub: " + Hex.create(sigPair.pub()));
        System.out.println("sec: " + Hex.create(sigPair.sec()));
    }
}
