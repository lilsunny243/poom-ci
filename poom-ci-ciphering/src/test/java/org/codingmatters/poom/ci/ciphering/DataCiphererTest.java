package org.codingmatters.poom.ci.ciphering;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;
import org.codingmatters.poom.ci.ciphering.descriptors.json.CipheredDataWriter;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DataCiphererTest {

    private KeyStore privateKeystore;
    private KeyStore publicKeystore;
    private JsonFactory jsonFactory = new JsonFactory();

    @Before
    public void setUp() throws Exception {
        this.privateKeystore = KeyStore.getInstance("pkcs12");
        this.privateKeystore.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("private-store.p12"), "changeit".toCharArray());
        assertTrue(this.privateKeystore.containsAlias("test-secret"));

        this.publicKeystore = KeyStore.getInstance("pkcs12");
        this.publicKeystore.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("public-store.p12"), "changeit".toCharArray());
        assertTrue(this.publicKeystore.containsAlias("test-secret"));
    }

    @Test
    public void certificateToPrivateKeyLookup() throws Exception {
        Certificate certificate = this.publicKeystore.getCertificate("test-secret");
        assertTrue(this.publicKeystore.isCertificateEntry("test-secret"));

        String privateAlias = this.privateKeystore.getCertificateAlias(certificate);

        assertThat(privateAlias, Matchers.is("test-secret"));
        assertTrue(this.privateKeystore.isKeyEntry(privateAlias));
    }

    @Test
    public void cipherData() throws Exception {
        byte[] dataToCipher = "undisclosable content, need to be ciphered so that only the private key can see clear throuht it.".getBytes();
        Certificate certificate = this.publicKeystore.getCertificate("test-secret");

        CipheredData ciphered = new DataCipherer(certificate, dataToCipher).cipher();

        try(ByteArrayOutputStream out = new ByteArrayOutputStream() ; JsonGenerator generator = jsonFactory.createGenerator(out)) {
            generator.useDefaultPrettyPrinter();

            new CipheredDataWriter().write(generator, ciphered);
            generator.flush();
            generator.close();
            System.out.println(out.toString());
        }
    }
}