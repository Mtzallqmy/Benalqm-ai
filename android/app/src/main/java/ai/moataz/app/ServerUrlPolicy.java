package ai.moataz.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

final class ServerUrlPolicy {
    private ServerUrlPolicy() {}

    static String normalize(String raw) {
        if (isBlank(raw)) throw new IllegalArgumentException("Server URL is required");
        String value = raw.trim();
        if (!value.contains("://")) value = "http://" + value;

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("Only http:// and https:// are supported");
            }
            if (isBlank(uri.getHost())) {
                throw new IllegalArgumentException("Server URL must include a host");
            }
            if (uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Credentials are not allowed in the server URL");
            }
            URI normalized = new URI(
                scheme,
                null,
                uri.getHost().toLowerCase(Locale.ROOT),
                uri.getPort(),
                cleanPath(uri.getPath()),
                null,
                null
            );
            String result = normalized.toString();
            return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("Invalid server URL", error);
        }
    }

    static boolean isTrustedOrigin(String serverUrl, String trustedOrigin) {
        if (isBlank(trustedOrigin)) return false;
        try {
            URI server = new URI(normalize(serverUrl));
            URI trusted = new URI(normalize(trustedOrigin));
            return originPort(server) == originPort(trusted)
                && server.getScheme().equalsIgnoreCase(trusted.getScheme())
                && server.getHost().equalsIgnoreCase(trusted.getHost());
        } catch (RuntimeException | URISyntaxException ignored) {
            return false;
        }
    }

    private static int originPort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String cleanPath(String path) {
        if (isBlank(path) || "/".equals(path)) return "";
        String cleaned = path;
        while (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        return cleaned;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
