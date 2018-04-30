package org.codingmatters.poom.ci.ciphering;

import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public class DataCipherer implements DataCiphering {

    private final SecureRandom rng = new SecureRandom();

    private final Certificate certificate;
    private final byte[] data;

    public DataCipherer(Certificate certificate, byte[] data) {
        this.certificate = certificate;
        this.data = data;
    }

    private SecretKey generateKey(String algorithm, int keysize) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
        keyGenerator.init(keysize, rng);
        return keyGenerator.generateKey();
    }

    private IvParameterSpec generateIV(final int ivSizeBytes) {
        final byte[] iv = new byte[ivSizeBytes];
        this.rng.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public CipheredData cipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, CertificateEncodingException {
        Cipher aesCBC = Cipher.getInstance(DATA_TRANSFORMATION);

        SecretKey transportKey = this.generateKey(TRANSPORT_KEY_ALGORITHM, 128);
        IvParameterSpec iv = this.generateIV(aesCBC.getBlockSize());
        aesCBC.init(Cipher.ENCRYPT_MODE, transportKey, iv);

        ByteArrayOutputStream cipheredData = new ByteArrayOutputStream();
        try (CipherOutputStream cos = new CipherOutputStream(cipheredData, aesCBC)) {
            cos.write(this.data);
        }

        Cipher rsaCipher = Cipher.getInstance(TRANSPORT_KEY_TRANSFORMATION);
        rsaCipher.init(Cipher.ENCRYPT_MODE, this.certificate);
        ByteArrayOutputStream cipheredKey = new ByteArrayOutputStream();
        try (CipherOutputStream cos = new CipherOutputStream(cipheredKey, rsaCipher)) {
            cos.write(transportKey.getEncoded());
        }

        return CipheredData.builder()
                .certificate(this.certificate.getEncoded())
                .iv(iv.getIV())
                .transportKeySpec(cipheredKey.toByteArray())
                .data(cipheredData.toByteArray())
                .build();
    }
}
