package org.codingmatters.poom.ci.ciphering.app;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.ci.ciphering.DataCipherer;
import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;
import org.codingmatters.poom.ci.ciphering.descriptors.json.CipheredDataWriter;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class FileCipherer {
    public static void main(String[] args) {
        if(args.length < 2) {
            throw new RuntimeException("usage : <certificate file path> <data file path> {encrypted file path}");
        }

        File certificateFile = new File(args[0]);
        File dataFile = new File(args[1]);

        File cipheredFile = new File(dataFile.getParentFile(), dataFile.getName() + ".enc");
        if(args.length >= 3) {
            cipheredFile = new File(args[2]);
        }

        X509Certificate certificate;
        try(FileInputStream in = new FileInputStream(certificateFile)) {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) fact.generateCertificate(in);
        } catch (IOException | CertificateException e) {
            throw new RuntimeException("error reading certificate from " + certificateFile.getAbsolutePath(), e);
        }

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try(FileInputStream in = new FileInputStream(dataFile)) {
            byte [] buffer = new byte[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                data.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new RuntimeException("error reading data from " + dataFile.getAbsolutePath(), e);
        }

        CipheredData ciphered = null;
        try {
            ciphered = new DataCipherer(certificate, data.toByteArray()).cipher();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | CertificateEncodingException | IOException e) {
            throw new RuntimeException("error ciphering data", e);
        }

        JsonFactory jsonFactory = new JsonFactory();
        try(JsonGenerator generator = jsonFactory.createGenerator(new FileOutputStream(cipheredFile))) {
            generator.useDefaultPrettyPrinter();

            new CipheredDataWriter().write(generator, ciphered);
        } catch (IOException e) {
            throw new RuntimeException("error writing ciphered file to " + cipheredFile.getAbsolutePath(), e);
        }
    }
}
