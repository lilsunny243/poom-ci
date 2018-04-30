package org.codingmatters.poom.ci.ciphering;

import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Optional;

public class DataUncipherer implements DataCiphering {
    private final KeyStore privateKeystore;
    private final char[] keyspass;

    public DataUncipherer(KeyStore privateKeystore, char[] keyspass) {
        this.privateKeystore = privateKeystore;
        this.keyspass = keyspass;
    }

    public byte[] uncipher(CipheredData ciphered) throws CertificateException, IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Optional<Key> privateKey = this.lookupPrivateKey(ciphered.certificate());

        if(privateKey.isPresent()) {
            SecretKey transportKey = this.readTransportKey(ciphered.transportKeySpec(), privateKey.get());
            IvParameterSpec iv = new IvParameterSpec(ciphered.iv());

            return uncipherData(transportKey, iv, ciphered.data());
        }
        return new byte[0];
    }

    private byte[] uncipherData(SecretKey transportKey, IvParameterSpec iv, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        Cipher aesCBC = Cipher.getInstance(DATA_TRANSFORMATION);
        aesCBC.init(Cipher.DECRYPT_MODE, transportKey, iv);

        try(ByteArrayInputStream in = new ByteArrayInputStream(data); CipherInputStream cis = new CipherInputStream(in, aesCBC)) {
            ByteArrayOutputStream uncipheredData = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for(int read = cis.read(buffer) ; read != -1 ; read = cis.read(buffer)) {
                uncipheredData.write(buffer, 0, read);
            }

            return uncipheredData.toByteArray();
        }
    }

    private SecretKey readTransportKey(byte[] transportKeySpec, Key privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        Cipher rsaCipher = Cipher.getInstance(TRANSPORT_KEY_TRANSFORMATION);
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);

        try(ByteArrayInputStream in = new ByteArrayInputStream(transportKeySpec); CipherInputStream cis = new CipherInputStream(in, rsaCipher)) {
            ByteArrayOutputStream transportKey = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for(int read = cis.read(buffer) ; read != -1 ; read = cis.read(buffer)) {
                transportKey.write(buffer, 0, read);
            }

            return new SecretKeySpec(transportKey.toByteArray(), TRANSPORT_KEY_ALGORITHM);
        }
    }

    private Optional<Key> lookupPrivateKey(byte[] certificate) throws CertificateException, IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try(ByteArrayInputStream in = new ByteArrayInputStream(certificate)) {
            Certificate cert = certFactory.generateCertificate(in);
            String alias = this.privateKeystore.getCertificateAlias(cert);
            if(alias != null) {
                return Optional.ofNullable(this.privateKeystore.getKey(alias, this.keyspass));
            }
        }
        return Optional.empty();
    }
}
