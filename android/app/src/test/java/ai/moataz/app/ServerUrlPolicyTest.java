package ai.moataz.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServerUrlPolicyTest {
    @Test
    public void addsHttpForLanAddress() {
        assertEquals("http://192.168.1.20:2026", ServerUrlPolicy.normalize("192.168.1.20:2026/"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmbeddedCredentials() {
        ServerUrlPolicy.normalize("https://user:password@example.com");
    }

    @Test
    public void trustedOriginMatchesDefaultPorts() {
        assertTrue(ServerUrlPolicy.isTrustedOrigin("https://app.example.com", "https://app.example.com/"));
        assertFalse(ServerUrlPolicy.isTrustedOrigin("http://app.example.com", "https://app.example.com"));
        assertFalse(ServerUrlPolicy.isTrustedOrigin("https://other.example.com", "https://app.example.com"));
    }
}
