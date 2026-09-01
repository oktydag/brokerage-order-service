package com.brokerage.common.domain.valueobjects;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record RequestFingerprint(String value) implements Serializable {

    private static final String ALGORITHM = "SHA-256";
    private static final String SEPARATOR = "|";

    public RequestFingerprint {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("fingerprint must not be blank");
        }
    }

    public static RequestFingerprint over(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = digest.digest(String.join(SEPARATOR, parts).getBytes(StandardCharsets.UTF_8));
            return new RequestFingerprint(HexFormat.of().formatHex(hashed));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
