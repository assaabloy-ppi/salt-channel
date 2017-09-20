package saltaa;

import org.libsodium.jni.SodiumJNI;

/**
 * libsodium-jni based implementation of SaltLib based on https://github.com/joshjdevl/libsodium-jni
 * 
 * @author Alex Reshniuk
 */
public class NativeSaltLib implements SaltLib {
    private static final Object LIB_SYNC = new Object();
    private static SodiumJNI SODIUMJNI_INSTANCE;
    
    public NativeSaltLib() {
        synchronized (LIB_SYNC) {
            if (SODIUMJNI_INSTANCE == null) {
                System.loadLibrary("sodiumjni");
                SODIUMJNI_INSTANCE = new SodiumJNI();
                int result = SodiumJNI.sodium_init();
                if (result != 0) {
                    throw new Error("Lib init error, SodiumJNI.sodium_init() returned " + result);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "libsodium-jni";
    }

    @Override
    public void crypto_sign_keypair_not_random(byte[] pk, byte[] sk) {
        byte[] seed = sk.clone();
		SodiumJNI.crypto_sign_seed_keypair(pk, sk, seed);   
    }

    @Override
    public void crypto_sign(byte[] sm, byte[] m, byte[] sk) {
        int[] dummy = new int[1];       
		SodiumJNI.crypto_sign(sm, dummy, m, m.length, sk);
    }

    @Override
    public void crypto_sign_open(byte[] m, byte[] sm, byte[] pk) {
        int[] dummy = new int[1];             
		int res = SodiumJNI.crypto_sign_open(m, dummy, sm, sm.length, pk);
        if (res != 0) {
            throw new BadSignatureException();
        }
    }

    @Override
    public void crypto_box_keypair_not_random(byte[] pk, byte[] sk) {
    	SodiumJNI.crypto_scalarmult_base(pk, sk);
    }

    @Override
    public void crypto_box_beforenm(byte[] k, byte[] pk, byte[] sk) {
    	SodiumJNI.crypto_box_beforenm(k, pk, sk);
    }

    @Override
    public void crypto_box_afternm(byte[] c, byte[] m, byte[] n, byte[] k) {
        int mlen = m.length;
        
        if (m.length < SaltLib.crypto_box_ZEROBYTES) {
            throw new IllegalArgumentException("m is too short");
        }
        
        int result = SodiumJNI.crypto_box_afternm(c, m, mlen, n, k);
        if (result != 0) {
            throw new IllegalArgumentException("SodiumJNI.crypto_box_afternm " + result);
        }    	
    }

    @Override
    public void crypto_box_open_afternm(byte[] m, byte[] c, byte[] n, byte[] k) {
        if (c.length < SaltLib.crypto_box_BOXZEROBYTES) {
            throw new IllegalArgumentException("c is too short, " + c.length);
        }
        
        int result = SodiumJNI.crypto_box_open_afternm(m, c, c.length, n, k);
        if (result != 0) {
            throw new BadEncryptedDataException();
        }
    }

    @Override
    public void crypto_hash(byte[] h, byte[] m) {
        SodiumJNI.crypto_hash_sha512(h, m, m.length);
    }    
    
   }
