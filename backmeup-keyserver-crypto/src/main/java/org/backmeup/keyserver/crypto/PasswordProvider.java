package org.backmeup.keyserver.crypto;

import org.backmeup.keyserver.model.CryptoException;

public interface PasswordProvider {
    String getAlgorithm();

    String getPassword(int length) throws CryptoException;

    String getPassword(int length, boolean specialChars) throws CryptoException;
}
