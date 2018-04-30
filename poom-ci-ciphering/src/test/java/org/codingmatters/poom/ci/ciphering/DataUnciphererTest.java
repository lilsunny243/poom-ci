package org.codingmatters.poom.ci.ciphering;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;
import org.codingmatters.poom.ci.ciphering.descriptors.json.CipheredDataReader;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyStore;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DataUnciphererTest {

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
    public void uncipherData() throws Exception {
        String json = "{\n" +
                "  \"certificate\" : \"MIIDczCCAlugAwIBAgIEHBUlQTANBgkqhkiG9w0BAQsFADBqMQswCQYDVQQGEwJGUjEOMAwGA1UECBMFRG91YnMxETAPBgNVBAcTCEJlc2FuY29uMRIwEAYDVQQKEwlGbGV4aW8uZnIxDTALBgNVBAsTBHRlc3QxFTATBgNVBAMTDHRlc3Qtc2VjcmV0czAeFw0xODA0MzAwMzQxMzVaFw0xODA3MjkwMzQxMzVaMGoxCzAJBgNVBAYTAkZSMQ4wDAYDVQQIEwVEb3ViczERMA8GA1UEBxMIQmVzYW5jb24xEjAQBgNVBAoTCUZsZXhpby5mcjENMAsGA1UECxMEdGVzdDEVMBMGA1UEAxMMdGVzdC1zZWNyZXRzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuyPiIPrQvXSwfsG3NyeiiI3JB++AVmtfLMaNl7QT0arPLSjEu2b8xIm8xLx9sq82yWN786CaPE0XhMvBVuvJq21ToIaEimYYw5dKit4tBHdXrh+lxz6OBjAu8IpI6VKjAXkubxgQtzRI4Ab85fR6wnXKysm78a8426/QShrOzipdyJUJSpBwKWiRtZGLNiOdQHC3w/H5d63lCADQZX4lh2NmOgacN/dAjsbDHyKl9+DFlaZ7sRTuXPs94c9JIkr2bIQHNLFU+mkV9E/41gOig8huQDfY3jPZumYv/S0/JOOcqqJ4eFEJM7f91Tuerk9VdTuvZ/Ru+RySwfpiIr+MfQIDAQABoyEwHzAdBgNVHQ4EFgQULr6lMPXr1st0T9KhV8Ff4KCWFhMwDQYJKoZIhvcNAQELBQADggEBAAbLp9Iquc7k3b5Qs4JclphCQLp1pXQJmt/c/HSYWHa91CmdasA+SJegtD7IFUUtg5cUItSWdOs+IPWQp+8b6/n5lZFB//4EvRAbxCMzqbkDTgyicCngFVxldhb3yPhX5aGQMp8+TuGyzmpSingTWmuJ6JkjGRn6ZYUkzMxMzfdaMzDcH+mJWmXjf0XijRGiJ21gOlZo6V7Cfawn5OP+eBIjEoe9HnRIKDpBHMkGy1I8+dvRNkhYCuxUJ0YBA6jMYUs0jqjl8h+Yqb0I8R/58SdpZFN0GZ+3hkNC/C61CuapJWSEaMUCABPgbixqhHyql/7CJGV2SMRuL+XoVjKWbwY=\",\n" +
                "  \"iv\" : \"HUkd3i2Jv7XIdhEtiwJMrA==\",\n" +
                "  \"transportKeySpec\" : \"kOg5ZykAyrO4VNlhSlF9zGY7w+aGpE6b44JGsJBM/0f5MKKhTazqSHFkL9o2CDEZ/pHfgOxIe+kvyL5+IMDe/5QKwOTKhKtMDm5iUmWauiVpsn+1BBEvsnsmv2K6LBFzCYcfNcb6jAcP4G5fHc50osFxYsyZhKUAQmDbhqzjEMaX2/Z0TnOeqwylqDggj8++NmSrx8DiE4yV+ai8hMnOxIjcZBgyo3y8tlBXTJOCPYD6rN398VkfwjedHTRKezgGNr8GCPZTDhXbAHTcg/eBMwACwKeytR6Pir47GNfQpRPxG7ELrAVdO9tsfoMx5dmDlQEPAIED3/+aAjdLv4vHwA==\",\n" +
                "  \"data\" : \"qTsWOqKammYjRF51eooZq47wx1n6GHwZwmh9cLiyTC0uHfoWK8R5X6pSKXpabLX+sBR80SaGX1O9wwOi3fuaABXhQ1hmU+ckr0N4m66ET5uEvcW5wqWUnt1B1CI2jrXnfTNfHJiEZYGYc+UMM2DYgA==\"\n" +
                "}";

        CipheredData ciphered;
        try(JsonParser parser = this.jsonFactory.createParser(json.getBytes())) {
            ciphered = new CipheredDataReader().read(parser);
        }

        byte [] data = new DataUncipherer(this.privateKeystore, "changeit".toCharArray()).uncipher(ciphered);
        assertThat(new String(data), is("undisclosable content, need to be ciphered so that only the private key can see clear throuht it."));
    }
}