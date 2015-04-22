package org.backmeup.keyserver.core.crypto.impl;

import java.security.GeneralSecurityException;

import org.backmeup.keyserver.core.crypto.KeyStretchingProvider;
import org.backmeup.keyserver.model.CryptoException;

import com.lambdaworks.crypto.SCrypt;

public class SCryptKeyStretchingProvider implements KeyStretchingProvider {

    @Override
    public String getAlgorithm() {
        return "SCRYPT";
    }

    @Override
    public byte[] stretch(byte[] key, byte[] salt) throws CryptoException {
        try {
            return SCrypt.scrypt(key, salt, 1 << 14, 8, 1, 32);
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }
    }
}
