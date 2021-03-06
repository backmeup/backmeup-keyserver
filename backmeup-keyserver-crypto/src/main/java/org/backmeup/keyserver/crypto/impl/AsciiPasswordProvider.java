package org.backmeup.keyserver.crypto.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.backmeup.keyserver.crypto.PasswordProvider;

public class AsciiPasswordProvider implements PasswordProvider {
    private static final String ASCII_LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String ASCII_UPPERCASE_CHARS = ASCII_LOWERCASE_CHARS.toUpperCase();
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL_CHARS = "#!%&/()=?{}[]+~-<>,;.:_^@";
    
    public AsciiPasswordProvider() {
    }

    @Override
    public String getAlgorithm() {
        return "ASCII";
    }

    @Override
    public synchronized String getPassword(int length) {
        return this.getPassword(length, true);
    }
    
    @Override
    public synchronized String getPassword(int length, boolean specialChars) {
        String chars = ASCII_LOWERCASE_CHARS + ASCII_UPPERCASE_CHARS + NUMBERS;
        if (specialChars) {
            chars += SPECIAL_CHARS;
        }
        return RandomStringUtils.random(length,  chars);
    }
}
