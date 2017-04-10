package saltaa;

/**
 * A useful subset of the 25 TweetNaCl functions is exposed 
 * through this interface. See README.md of this repository.
 * 
 * crypto_sign_x functions implement the ed25519 signature scheme.
 * See http://nacl.cr.yp.to/sign.html.
 * 
 * crypto_box_x functions implement public key authenticated encryption 
 * using x25519+xsalsa20+poly1305.
 * See http://nacl.cr.yp.to/box.html.
 * 
 * @author Frans Lundberg
 */
public interface SaltLib {
    
    /**
     * Returns the name of the SaltLib implementation.
     */
    public String getName();
 
    
    // ======== crypto_sign ========

    public static final int crypto_sign_PUBLICKEYBYTES = 32;
    public static final int crypto_sign_SECRETKEYBYTES = 32;
    public static final int crypto_sign_BYTES = 64;
    
    /**
     * The crypto_sign_keypair_not_random function takes a secret key and generates 
     * a corresponding public key. The secret key is in sk[0], sk[1], ..., sk[crypto_sign_SECRETKEYBYTES-1]
     * It puts the public key into pk[0], pk[1], ..., pk[crypto_sign_PUBLICKEYBYTES-1].
     * It then returns 0. 
     * 
     * Changes from original (crypto_sign_keypair): this function is deterministic, no random 
     * data is used. Note, the secret key can be random data.
     * 
     * @param pk Resulting public key.
     * @param sk Input secret key.
     * @throws IllegalArgumentException If one of the parameters is not of the correct size.
     */
    public void crypto_sign_keypair_not_random(byte[] pk, byte[] sk);
    
    /**
     * The crypto_sign function signs a message m[0], ..., m[mlen-1] using the signer's secret 
     * key sk[0], sk[1], ..., sk[crypto_sign_SECRETKEYBYTES-1], puts the signed message 
     * into sm[0], sm[1], ..., sm[smlen-1]. It then returns 0.
     * The length of sm is m.length + crypto_sign_BYTES.
     *
     * @throws IllegalArgumentException
     */
    public void crypto_sign(byte[] sm, byte[] m, byte[] sk);
    
    /**
     * The crypto_sign_open function verifies the signature in sm[0], ..., sm[smlen-1] 
     * using the signer's public key pk[0], pk[1], ..., pk[crypto_sign_PUBLICKEYBYTES-1]. 
     * The crypto_sign_open function puts the message into m[0], m[1], ..., m[mlen-1].
     * The caller must allocate at least sm.length bytes for m. 
     * If the signature fails verification, crypto_sign_open throws BadSignature, 
     * possibly after modifying m[0], m[1], etc. 
     * mlen is sm.length-crypto_sign_BYTES.
     * 
     * @throws BadSignatureException
     * @throws IllegalArgumentException
     */
    public void crypto_sign_open(byte[] m, byte[] sm, byte[] pk);
    
    
    // ======== crypto_box ========
    
    public static final int crypto_box_PUBLICKEYBYTES = 32;
    public static final int crypto_box_SECRETKEYBYTES = 32;
    public static final int crypto_box_BEFORENMBYTES = 32;
    public static final int crypto_box_NONCEBYTES = 24;
    public static final int crypto_box_ZEROBYTES = 32;
    public static final int crypto_box_BOXZEROBYTES = 16;
    
    /**
     * The crypto_box_keypair_not_random function takes a secret key and generates 
     * a corresponding public key. The caller puts the secret key is in 
     * sk[0], sk[1], ..., sk[crypto_box_SECRETKEYBYTES-1]. This function
     * puts the public key into pk[0], pk[1], ..., pk[crypto_box_PUBLICKEYBYTES-1].
     */
    public void crypto_box_keypair_not_random(byte[] pk, byte[] sk);
    
    /**
     * The first step of crypto_box; the x25519 key agreement. This function puts the 
     * shared secret key in k[0], k[1], ..., k[crypto_box_BEFORENMBYTES-1].
     * pk is the peer's public key (length crypto_box_PUBLICKEYBYTES). 
     * sk is the caller's secret key (length crypto_box_SECRETKEYBYTES).
     */
    public void crypto_box_beforenm(byte[] k, byte[] pk, byte[] sk);
    
    /**
     * The crypto_box_afternm function encrypts and authenticates a 
     * message m[0], ..., m[mlen-1] using the shared key 
     * k[0], k[1], ..., k[crypto_box_BEFORENMBYTES-1], 
     * and a nonce n[0], n[1], ..., n[crypto_box_NONCEBYTES-1]. 
     * The function puts the ciphertext into c[0], c[1], ..., c[mlen-1].
     * 
     * WARNING: Messages in the this NaCl API are 0-padded versions of messages in 
     * the C++ NaCl API.
     * Specifically: The caller must ensure, before calling this function, 
     * that the first crypto_box_ZEROBYTES bytes of the message m are all 0. 
     * Typical higher-level applications will work with the remaining bytes of the message; 
     * note, however, that mlen (m.length) counts all of the bytes, including the bytes 
     * required to be 0.
     * Similarly, ciphertexts in this API are 0-padded versions of messages in 
     * the C++ NaCl API. Specifically: The crypto_box function ensures that the first 
     * crypto_box_BOXZEROBYTES bytes of the ciphertext c are all 0. 
     */
    public void crypto_box_afternm(byte[] c, byte[] m, byte[] n, byte[] k);
    
    /**
     * The crypto_box_open function verifies and decrypts a ciphertext c[0], ..., c[clen-1] 
     * using the shared key k[0], k[1], ..., k[crypto_box_BEFORENMBYTES-1], 
     * and a nonce n[0], ..., n[crypto_box_NONCEBYTES-1]. 
     * The function puts the plaintext into m[0], m[1], ..., m[clen-1].
     * If the ciphertext fails verification, this function throws BadEncryptedData, 
     * possibly after modifying m[0], m[1], etc.
     * The caller must ensure, before calling the function, that the first 
     * crypto_box_BOXZEROBYTES bytes of the ciphertext c are all 0. 
     * The crypto_box_open function ensures (in case of success) that the first 
     * crypto_box_ZEROBYTES bytes of the plaintext m are all 0. 
     * 
     * @throws BadEncryptedDataException
     */
    public void crypto_box_open_afternm(byte[] m, byte[] c, byte[] n, byte[] k);


    // ======== crypto_hash ========
    
    public static final int crypto_hash_BYTES = 64;

    /** The crypto_hash function hashes a message m. It returns a hash h. The output
     * length h.size() is always crypto_hash_BYTES.
     * crypto_hash() is currently an implementation of SHA-512.
     * The crypto_hash function hashes a message m[0], m[1], ..., m[mlen-1]. 
     * It puts the hash into h[0], h[1], ..., h[crypto_hash_BYTES-1].
     */
    public void crypto_hash(byte[] h, byte[] m);
}
