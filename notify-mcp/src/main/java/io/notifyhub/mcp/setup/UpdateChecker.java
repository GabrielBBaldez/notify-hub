package io.notifyhub.mcp.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.notifyhub.mcp.NotifyMcpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;

/**
 * Checks for new versions on GitHub Releases and self-updates the executable.
 * <p>
 * Works for both JAR and native image distributions.
 * Downloads to {@code ~/.notifyhub/} replacing the current binary.
 */
public class UpdateChecker {

    private static final String REPO = "GabrielBBaldez/notify-hub";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String CURRENT_VERSION = NotifyMcpServer.VERSION;

    private static final String RST  = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String GRN  = "\033[32m";
    private static final String YLW  = "\033[33m";
    private static final String CYN  = "\033[36m";
    private static final String RED  = "\033[31m";
    private static final String ORG  = "\033[38;5;208m";
    private static final String DIM  = "\033[2m";

    /**
     * Check for updates and optionally download.
     */
    public static void run() {
        System.out.println();
        System.out.println(ORG + "  NotifyHub MCP — Update Checker" + RST);
        System.out.println(DIM + "  Current version: v" + CURRENT_VERSION + RST);
        System.out.println();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            // 1. Check latest version from GitHub
            System.out.println("  Checking for updates...");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "NotifyHub-MCP/" + CURRENT_VERSION)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.out.println(RED + "  ✗ Could not check for updates (HTTP " + resp.statusCode() + ")" + RST);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode release = mapper.readTree(resp.body());

            String latestTag = release.path("tag_name").asText(""); // e.g. "v1.0.0"
            String latestVersion = latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;
            String releaseUrl = release.path("html_url").asText("");

            if (latestVersion.isBlank()) {
                System.out.println(YLW + "  No releases found." + RST);
                return;
            }

            int cmp = compareVersions(CURRENT_VERSION, latestVersion);

            if (cmp >= 0) {
                System.out.println(GRN + "  ✓ You're on the latest version (v" + CURRENT_VERSION + ")" + RST);
                System.out.println();
                return;
            }

            // 2. New version available
            System.out.println();
            System.out.println(ORG + BOLD + "  New version available: v" + latestVersion + RST);
            System.out.println(DIM + "  " + releaseUrl + RST);
            System.out.println();

            // Find the right asset for this OS
            String assetName = getAssetName();
            String downloadUrl = null;
            long assetSize = 0;

            for (JsonNode asset : release.path("assets")) {
                String name = asset.path("name").asText("");
                if (name.equals(assetName) || name.contains("notify-mcp") && name.endsWith(getOsSuffix())) {
                    downloadUrl = asset.path("browser_download_url").asText("");
                    assetSize = asset.path("size").asLong(0);
                    break;
                }
            }

            // Fallback: try the JAR
            if (downloadUrl == null) {
                for (JsonNode asset : release.path("assets")) {
                    String name = asset.path("name").asText("");
                    if (name.endsWith(".jar") && name.contains("notify-mcp")) {
                        downloadUrl = asset.path("browser_download_url").asText("");
                        assetSize = asset.path("size").asLong(0);
                        assetName = name;
                        break;
                    }
                }
            }

            if (downloadUrl == null) {
                System.out.println(YLW + "  No downloadable asset found for your platform." + RST);
                System.out.println("  Download manually: " + releaseUrl);
                return;
            }

            // 3. Ask to download
            String sizeStr = assetSize > 0 ? String.format(" (%.1f MB)", assetSize / 1_048_576.0) : "";
            System.out.println("  Asset: " + assetName + sizeStr);
            System.out.print(CYN + "  Download and install? [Y/n]: " + RST);

            String input = new java.util.Scanner(System.in).nextLine().trim();
            if (!input.isBlank() && !input.toLowerCase().startsWith("y")) {
                System.out.println(DIM + "  Skipped." + RST);
                return;
            }

            // 4. Download
            System.out.println("  Downloading...");
            Path installDir = Path.of(System.getProperty("user.home"), ".notifyhub");
            Files.createDirectories(installDir);
            Path targetFile = installDir.resolve(assetName);
            Path tempFile = installDir.resolve(assetName + ".tmp");

            HttpRequest dlReq = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "NotifyHub-MCP/" + CURRENT_VERSION)
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            HttpResponse<InputStream> dlResp = client.send(dlReq, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream is = dlResp.body()) {
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // 5. Replace current file
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            // Make executable on Unix
            if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
                targetFile.toFile().setExecutable(true);
            }

            System.out.println();
            System.out.println(GRN + BOLD + "  ✓ Updated to v" + latestVersion + "!" + RST);
            System.out.println("  " + DIM + "Installed at: " + targetFile + RST);
            System.out.println();
            System.out.println(BOLD + "  Restart Claude to use the new version." + RST);
            System.out.println();

        } catch (IOException | InterruptedException e) {
            System.out.println(RED + "  ✗ Update check failed: " + e.getMessage() + RST);
            System.out.println(DIM + "  Check manually: https://github.com/" + REPO + "/releases" + RST);
        }
    }

    /**
     * Compare two semver strings. Returns negative if v1 < v2, 0 if equal, positive if v1 > v2.
     */
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("[.\\-]");
        String[] parts2 = v2.split("[.\\-]");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? parseIntSafe(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseIntSafe(parts2[i]) : 0;
            if (p1 != p2) return p1 - p2;
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static String getAssetName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String osName;
        if (os.contains("win")) osName = "windows";
        else if (os.contains("mac")) osName = "macos";
        else osName = "linux";

        String archName;
        if (arch.contains("aarch64") || arch.contains("arm64")) archName = "aarch64";
        else archName = "x86_64";

        String ext = os.contains("win") ? ".exe" : "";
        return "notifyhub-mcp-" + osName + "-" + archName + ext;
    }

    private static String getOsSuffix() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return ".exe";
        if (os.contains("mac")) return "-macos";
        return "-linux";
    }
}
