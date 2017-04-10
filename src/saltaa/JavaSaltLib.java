package saltaa;

import com.iwebpp.crypto.TweetNaclFast;

/**
 * Pure Java implementation of SaltLib based on https://github.com/InstantWebP2P/tweetnacl-java.
 */
public class JavaSaltLib implements SaltLib {

    @Override
    public String getName() {
        return "java-instantwebp2p";
    }
    
    @Override
    public void crypto_sign_keypair_not_random(byte[] pk, byte[] sk) {
        TweetNaclFast.crypto_sign_keypair(pk, sk, true);
    }

    @Override
    public void crypto_sign(byte[] sm, byte[] m, byte[] sk) {
        long dummy = 0;
        TweetNaclFast.crypto_sign(sm, dummy, m, 0, m.length, sk);
    }

    @Override
    public void crypto_sign_open(byte[] m, byte[] sm, byte[] pk) {
        long dummy = 0;
        int res = TweetNaclFast.crypto_sign_open(m, dummy, sm, 0, sm.length, pk);
        if (res != 0) {
            throw new BadSignatureException();
        }
        System.arraycopy(m, SaltLib.crypto_sign_BYTES, m, 0, sm.length-SaltLib.crypto_sign_BYTES);
    }

    @Override
    public void crypto_box_keypair_not_random(byte[] pk, byte[] sk) {
        TweetNaclFast.crypto_box_keypair_not_random(pk, sk);
    }

    @Override
    public void crypto_box_beforenm(byte[] k, byte[] pk, byte[] sk) {
        TweetNaclFast.crypto_box_beforenm(k, pk, sk);
    }

    @Override
    public void crypto_box_afternm(byte[] c, byte[] m, byte[] n, byte[] k) {
        int mlen = m.length;
        
        if (m.length < SaltLib.crypto_box_ZEROBYTES) {
            throw new IllegalArgumentException("m is too short");
        }
        
        int result = TweetNaclFast.crypto_box_afternm(c, m, mlen, n, k);
        if (result != 0) {
            throw new IllegalArgumentException("TweetNaclFast.crypto_box_afternm returned " + result);
        }
    }

    @Override
    public void crypto_box_open_afternm(byte[] m, byte[] c, byte[] n, byte[] k) {
        if (c.length < SaltLib.crypto_box_BOXZEROBYTES) {
            throw new IllegalArgumentException("c is too short, " + c.length);
        }
        
        int result = TweetNaclFast.crypto_box_open_afternm(m, c, c.length, n, k);
        if (result != 0) {
            throw new BadEncryptedDataException();
        }
    }

    @Override
    public void crypto_hash(byte[] h, byte[] m) {
        TweetNaclFast.crypto_hash(h, m);
    }
}
