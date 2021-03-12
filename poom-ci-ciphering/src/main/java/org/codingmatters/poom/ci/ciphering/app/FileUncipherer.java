package org.codingmatters.poom.ci.ciphering.app;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.ci.ciphering.DataUncipherer;
import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;
import org.codingmatters.poom.ci.ciphering.descriptors.json.CipheredDataReader;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class FileUncipherer {
    public static void main(String[] args) {
        if(args.length < 2) throw new RuntimeException("usage : <key store path> <ciphered data path>");

        char[] keystorePassword = System.console().readPassword("keystore password : ");
        KeyStore ks;
        try {
            ks = KeyStore.getInstance(new File(args[0]), keystorePassword);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("failed loading keystore : " + args[0], e);
        }
        CipheredData ciphered;


        try(JsonParser parser = new JsonFactory().createParser(new File(args[1]))) {
            ciphered = new CipheredDataReader().read(parser);
        } catch (IOException e) {
            throw new RuntimeException("failed reading ciphered data", e);
        }
        try {
            System.out.println(new String(new DataUncipherer(ks, keystorePassword).uncipher(ciphered)));
        } catch (CertificateException | IOException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("failed unciphering data", e);
        }
    }
}
