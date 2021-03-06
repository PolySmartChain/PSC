package org.tdf.common.crypto.jce;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

public final class SpongyCastleProvider {

    public static Provider getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final Provider INSTANCE;

        static {
            Provider p = Security.getProvider("SC");

            INSTANCE = (p != null) ? p : new BouncyCastleProvider();

            INSTANCE.put("MessageDigest.ETH-KECCAK-256", "org.tdf.common.crypto.cryptohash.Keccak256");
            INSTANCE.put("MessageDigest.ETH-KECCAK-512", "org.tdf.common.crypto.cryptohash.Keccak512");
        }
    }
}
