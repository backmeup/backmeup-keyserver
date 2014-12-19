package org.backmeup.keyserver.core;

import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.EncryptionProvider;
import org.backmeup.keyserver.core.crypto.HashProvider;
import org.backmeup.keyserver.core.crypto.KeyStretchingProvider;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.crypto.PasswordProvider;
import org.backmeup.keyserver.core.crypto.ProviderRegistry;
import org.backmeup.keyserver.model.KeyserverUtils;
import org.apache.commons.codec.binary.StringUtils;

public class EncryptionUtils {

    private EncryptionUtils() {

    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static byte[] concat(byte[] a, byte[] b, byte[] c) {
        byte[] result = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length + b.length, c.length);
        return result;
    }

    public static byte[] decryptByteArray(Keyring k, byte[] key, byte[] message) throws CryptoException {
        EncryptionProvider ep;
        try {
            ep = ProviderRegistry.getEncryptionProvider(k.getEncryptionAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return ep.decrypt(key, message);
    }

    public static String decryptString(Keyring k, byte[] key, byte[] message) throws CryptoException {
        EncryptionProvider ep;
        try {
            ep = ProviderRegistry.getEncryptionProvider(k.getEncryptionAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return StringUtils.newStringUtf8(ep.decrypt(key, message));
    }

    public static byte[] encryptByteArray(Keyring k, byte[] key, byte[] message) throws CryptoException {
        EncryptionProvider ep;
        try {
            ep = ProviderRegistry.getEncryptionProvider(k.getEncryptionAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return ep.encrypt(key, message);
    }

    public static byte[] encryptString(Keyring k, byte[] key, String message) throws CryptoException {
        EncryptionProvider ep;
        try {
            ep = ProviderRegistry.getEncryptionProvider(k.getEncryptionAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return ep.encrypt(key, StringUtils.getBytesUtf8(message));
    }

    public static String fmtKey(MessageFormat format, Object... inputs) {
        return format.format(inputs);
    }

    public static byte[] generateKey(Keyring k) throws CryptoException {
        EncryptionProvider ep;
        try {
            ep = ProviderRegistry.getEncryptionProvider(k.getEncryptionAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return ep.generateKey(k.getEncryptionKeyLength());
    }

    public static String generatePassword(Keyring k) throws CryptoException {
        PasswordProvider pp;
        try {
            pp = ProviderRegistry.getPasswordProvider(k.getPasswordAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return pp.getPassword(k.getPasswordLength());
    }

    public static byte[] hashByteArrayWithPepper(Keyring k, byte[] hashInput, String pepperApplication) throws CryptoException {
        HashProvider hp;
        try {
            hp = ProviderRegistry.getHashProvider(k.getHashAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return hp.hash(concat(hashInput, k.getPepper(pepperApplication)));
    }

    public static String hashStringWithPepper(Keyring k, String hashInput, String pepperApplication) throws CryptoException {
        return KeyserverUtils.toBase64String(hashByteArrayWithPepper(k, StringUtils.getBytesUtf8(hashInput), pepperApplication));
    }

    public static byte[][] split(byte[] a, int offset) {
        byte[][] arrs = new byte[2][];
        arrs[0] = Arrays.copyOf(a, offset);
        arrs[1] = Arrays.copyOfRange(a, offset, a.length);
        return arrs;
    }

    public static byte[] stretchStringWithPepper(Keyring k, String key, String pepperApplication) throws CryptoException {
        KeyStretchingProvider kp;
        try {
            kp = ProviderRegistry.getKeyStretchingProvider(k.getKeyStretchingAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        return kp.stretch(StringUtils.getBytesUtf8(key), k.getPepper(pepperApplication));
    }
}