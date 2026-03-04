package io.notifyhub.mcp.setup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.Console;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interactive CLI wizard for first-time NotifyHub MCP setup.
 * <p>
 * Runs BEFORE Spring Boot — pure Java + Jackson, no Spring dependency.
 * Guides the user through channel selection, credential input, and
 * generates the Claude Desktop / Claude Code configuration file.
 * <p>
 * Usage: {@code notifyhub-mcp --setup}
 */
public class SetupWizard {

    // ── ANSI colors ────────────────────────────────────────────────
    private static boolean COLOR = true;
    private static final String RST  = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM  = "\033[2m";
    private static final String GRN  = "\033[32m";
    private static final String YLW  = "\033[33m";
    private static final String CYN  = "\033[36m";
    private static final String RED  = "\033[31m";
    private static final String ORG  = "\033[38;5;208m";
    private static final String WHT  = "\033[37m";

    private static String c(String code) { return COLOR ? code : ""; }

    // ── I/O ────────────────────────────────────────────────────────
    private static final Scanner scanner = new Scanner(System.in);
    private static final Console console = System.console();

    // ── Channel definitions ────────────────────────────────────────
    private static final List<ChannelDef> CHANNELS = List.of(
        // ─── Popular ───
        new ChannelDef("email", "Email (SMTP)", "Popular", List.of(
            field("HOST",     "SMTP Host",            "smtp.gmail.com", false),
            field("PORT",     "SMTP Port",            "587",            false),
            field("USERNAME", "Username (email)",      null,             false),
            field("PASSWORD", "Password (app password)", null,          true),
            field("FROM",     "From address",          null,             false)
        )),
        new ChannelDef("sendgrid", "Email (SendGrid) — with open/bounce tracking", "Popular", List.of(
            field("API_KEY",   "SendGrid API Key (SG.xxx)", null, true),
            field("FROM",      "From address",               null, false),
            field("FROM_NAME", "From name (optional)",       null, false)
        )),
        new ChannelDef("slack", "Slack", "Popular", List.of(
            field("WEBHOOK_URL", "Webhook URL", null, false)
        )),
        new ChannelDef("telegram", "Telegram", "Popular", List.of(
            field("BOT_TOKEN", "Bot Token",  null, false),
            field("CHAT_ID",   "Chat ID",    null, false)
        )),
        new ChannelDef("discord", "Discord", "Popular", List.of(
            field("WEBHOOK_URL", "Webhook URL",           null, false),
            field("USERNAME",    "Bot username (optional)", "NotifyHub", false)
        )),

        // ─── Messaging ───
        new ChannelDef("sms", "SMS (Twilio)", "Messaging", List.of(
            field("ACCOUNT_SID", "Account SID",  null, false),
            field("AUTH_TOKEN",  "Auth Token",   null, true),
            field("FROM_NUMBER", "From number (+E.164)", null, false)
        )),
        new ChannelDef("whatsapp", "WhatsApp (Twilio)", "Messaging", List.of(
            field("ACCOUNT_SID", "Account SID",  null, false),
            field("AUTH_TOKEN",  "Auth Token",   null, true),
            field("FROM_NUMBER", "From number (whatsapp:+...)", null, false)
        )),
        new ChannelDef("teams", "Microsoft Teams", "Messaging", List.of(
            field("WEBHOOK_URL", "Webhook URL", null, false)
        )),
        new ChannelDef("google_chat", "Google Chat", "Messaging", List.of(
            field("WEBHOOK_URL", "Webhook URL", null, false)
        )),

        // ─── Social ───
        new ChannelDef("twitter", "Twitter / X", "Social", List.of(
            field("API_KEY",             "API Key",              null, true),
            field("API_SECRET",          "API Secret",           null, true),
            field("ACCESS_TOKEN",        "Access Token",         null, true),
            field("ACCESS_TOKEN_SECRET", "Access Token Secret",  null, true)
        )),
        new ChannelDef("linkedin", "LinkedIn", "Social", List.of(
            field("ACCESS_TOKEN", "Access Token",                null, true),
            field("AUTHOR_ID",    "Author URN (urn:li:person:...)", null, false)
        )),
        new ChannelDef("instagram", "Instagram", "Social", List.of(
            field("ACCESS_TOKEN", "Access Token",  null, true),
            field("IG_USER_ID",   "IG User ID",    null, false)
        )),
        new ChannelDef("tiktok_shop", "TikTok Shop", "Social", List.of(
            field("APP_KEY",      "App Key",        null, true),
            field("APP_SECRET",   "App Secret",     null, true),
            field("ACCESS_TOKEN", "Access Token",   null, true),
            field("SHOP_ID",      "Shop ID",        null, false)
        )),
        new ChannelDef("facebook", "Facebook (Pages + Messenger)", "Social", List.of(
            field("PAGE_ACCESS_TOKEN", "Page Access Token",  null, true),
            field("PAGE_ID",           "Page ID",            null, false)
        )),

        // ─── Other ───
        new ChannelDef("push", "Push (Firebase)", "Other", List.of(
            field("SERVER_KEY", "FCM Server Key", null, true)
        )),
        new ChannelDef("notion", "Notion", "Other", List.of(
            field("API_KEY",     "Integration Token", null, true),
            field("DATABASE_ID", "Database ID",       null, false)
        )),
        new ChannelDef("twitch", "Twitch", "Other", List.of(
            field("CLIENT_ID",      "Client ID",      null, false),
            field("ACCESS_TOKEN",   "Access Token",   null, true),
            field("BROADCASTER_ID", "Broadcaster ID", null, false),
            field("SENDER_ID",      "Sender ID",      null, false)
        )),
        new ChannelDef("youtube", "YouTube Live", "Other", List.of(
            field("ACCESS_TOKEN",  "Access Token",  null, true),
            field("CHANNEL_ID",    "Channel ID",    null, false),
            field("LIVE_CHAT_ID",  "Live Chat ID",  null, false)
        ))
    );

    // ── Record types ───────────────────────────────────────────────

    record ChannelDef(String id, String name, String category, List<FieldDef> fields) {}
    record FieldDef(String envSuffix, String prompt, String defaultValue, boolean secret) {}

    private static FieldDef field(String envSuffix, String prompt, String def, boolean secret) {
        return new FieldDef(envSuffix, prompt, def, secret);
    }

    // ═══════════════════════════════════════════════════════════════
    //  MAIN ENTRY POINT
    // ═══════════════════════════════════════════════════════════════

    public static void run() {
        detectColorSupport();
        printBanner();

        // Step 1: License key
        String licenseKey = askLicenseKey();

        // Step 2: Select channels
        List<ChannelDef> selected = selectChannels();
        if (selected.isEmpty()) {
            println(c(YLW) + "No channels selected. Exiting." + c(RST));
            return;
        }

        // Step 3: Configure each channel
        Map<String, String> envVars = new LinkedHashMap<>();
        if (licenseKey != null && !licenseKey.isBlank()) {
            envVars.put("NOTIFY_LICENSE_KEY", licenseKey);
        }
        for (ChannelDef ch : selected) {
            configureChannel(ch, envVars);
        }

        // Step 4: Choose config target
        Path configPath = chooseConfigTarget();

        // Step 5: Write config
        writeConfig(configPath, envVars);

        // Step 6: Done
        printSuccess(configPath, selected);
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 1: BANNER
    // ═══════════════════════════════════════════════════════════════

    private static void printBanner() {
        println("");
        println(c(ORG) + "  ╔════════════════════════════════════════════╗" + c(RST));
        println(c(ORG) + "  ║" + c(BOLD) + "      NotifyHub MCP  —  Quick Setup        " + c(RST) + c(ORG) + "║" + c(RST));
        println(c(ORG) + "  ╚════════════════════════════════════════════╝" + c(RST));
        println("");
        println(c(DIM) + "  One command to configure all your notification channels." + c(RST));
        println(c(DIM) + "  Docs: https://gabrielbbaldez.github.io/notify-hub/docs.html" + c(RST));
        println("");
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 2: LICENSE KEY
    // ═══════════════════════════════════════════════════════════════

    private static String askLicenseKey() {
        println(c(BOLD) + "  License Key" + c(RST));
        println(c(DIM) + "  Enter your license key or press Enter to skip (community mode)." + c(RST));
        print(c(CYN) + "  Key: " + c(RST));
        String key = scanner.nextLine().trim();
        println("");
        if (!key.isBlank()) {
            println(c(GRN) + "  ✓ License key saved" + c(RST));
        } else {
            println(c(DIM) + "  → Community mode (no license)" + c(RST));
        }
        println("");
        return key.isBlank() ? null : key;
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 3: SELECT CHANNELS
    // ═══════════════════════════════════════════════════════════════

    private static List<ChannelDef> selectChannels() {
        println(c(BOLD) + "  Select Channels" + c(RST));
        println(c(DIM) + "  Enter numbers separated by commas (e.g. 1,3,4)" + c(RST));
        println("");

        String currentCategory = "";
        for (int i = 0; i < CHANNELS.size(); i++) {
            ChannelDef ch = CHANNELS.get(i);
            if (!ch.category().equals(currentCategory)) {
                currentCategory = ch.category();
                println(c(DIM) + "  ── " + currentCategory + " ──" + c(RST));
            }
            String num = String.format("%2d", i + 1);
            println("  " + c(ORG) + num + c(RST) + ". " + ch.name());
        }

        println("");
        print(c(CYN) + "  Channels: " + c(RST));
        String input = scanner.nextLine().trim();
        println("");

        if (input.isBlank()) return List.of();

        List<ChannelDef> selected = new ArrayList<>();
        for (String part : input.split("[,\\s]+")) {
            try {
                int idx = Integer.parseInt(part.trim()) - 1;
                if (idx >= 0 && idx < CHANNELS.size()) {
                    ChannelDef ch = CHANNELS.get(idx);
                    if (!selected.contains(ch)) {
                        selected.add(ch);
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        if (!selected.isEmpty()) {
            String names = selected.stream().map(ChannelDef::name).collect(Collectors.joining(", "));
            println(c(GRN) + "  ✓ Selected: " + c(RST) + names);
            println("");
        }

        return selected;
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 4: CONFIGURE EACH CHANNEL
    // ═══════════════════════════════════════════════════════════════

    private static void configureChannel(ChannelDef ch, Map<String, String> envVars) {
        println(c(BOLD) + "  ── " + ch.name() + " ──" + c(RST));

        for (FieldDef f : ch.fields()) {
            String envKey = "NOTIFY_CHANNELS_" + ch.id().toUpperCase() + "_" + f.envSuffix();

            String promptText = "  " + f.prompt();
            if (f.defaultValue() != null) {
                promptText += c(DIM) + " [" + f.defaultValue() + "]" + c(RST);
            }
            promptText += ": ";

            String value;
            if (f.secret() && console != null) {
                print(promptText);
                char[] pwd = console.readPassword();
                value = pwd != null ? new String(pwd).trim() : "";
            } else {
                print(promptText);
                value = scanner.nextLine().trim();
            }

            if (value.isEmpty() && f.defaultValue() != null) {
                value = f.defaultValue();
            }

            if (!value.isEmpty()) {
                envVars.put(envKey, value);
            }
        }

        println(c(GRN) + "  ✓ " + ch.name() + " configured" + c(RST));
        println("");
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 5: DETECT & CHOOSE CONFIG TARGET
    // ═══════════════════════════════════════════════════════════════

    private static Path chooseConfigTarget() {
        println(c(BOLD) + "  Configuration Target" + c(RST));
        println(c(DIM) + "  Select where to save. You can run --setup again for other editors." + c(RST));
        println("");

        String os = System.getProperty("os.name", "").toLowerCase();
        Path home = Path.of(System.getProperty("user.home"));

        List<ConfigTarget> targets = new ArrayList<>();

        // ── AI Assistants ──

        // 1. Claude Desktop
        Path claudeDesktop = findConfigPath(os, home,
                "Claude", "claude_desktop_config.json",
                "Claude", "claude_desktop_config.json",
                ".config/Claude", "claude_desktop_config.json");
        if (claudeDesktop != null) {
            targets.add(new ConfigTarget("Claude Desktop", claudeDesktop));
        }

        // 2. Claude Code (global)
        targets.add(new ConfigTarget("Claude Code (global)",
                home.resolve(".claude").resolve(".mcp.json")));

        // 3. Cursor
        Path cursorConfig = findConfigPath(os, home,
                ".cursor", "mcp.json",
                ".cursor", "mcp.json",
                ".cursor", "mcp.json");
        if (cursorConfig != null) {
            targets.add(new ConfigTarget("Cursor", cursorConfig));
        }

        // 4. Windsurf (Codeium)
        Path windsurfConfig = findConfigPath(os, home,
                ".codeium/windsurf", "mcp_config.json",
                ".codeium/windsurf", "mcp_config.json",
                ".codeium/windsurf", "mcp_config.json");
        if (windsurfConfig != null) {
            targets.add(new ConfigTarget("Windsurf", windsurfConfig));
        }

        // 5. VS Code + GitHub Copilot (workspace level)
        Path vscodeConfig = Path.of(".vscode", "mcp.json").toAbsolutePath().normalize();
        targets.add(new ConfigTarget("VS Code (Copilot)", vscodeConfig));

        // 6. Cline (VS Code extension)
        Path clineConfig = findConfigPath(os, home,
                ".cline", "cline_mcp_settings.json",
                ".cline", "cline_mcp_settings.json",
                ".cline", "cline_mcp_settings.json");
        if (clineConfig != null) {
            targets.add(new ConfigTarget("Cline", clineConfig));
        }

        // 7. Roo Code (VS Code extension)
        Path rooConfig = findConfigPath(os, home,
                ".roo", "mcp.json",
                ".roo", "mcp.json",
                ".roo", "mcp.json");
        if (rooConfig != null) {
            targets.add(new ConfigTarget("Roo Code", rooConfig));
        }

        // 8. Continue.dev
        Path continueConfig = findConfigPath(os, home,
                ".continue", "config.json",
                ".continue", "config.json",
                ".continue", "config.json");
        if (continueConfig != null) {
            targets.add(new ConfigTarget("Continue.dev", continueConfig));
        }

        // 9. Amazon Q Developer
        Path amazonQConfig = findConfigPath(os, home,
                ".amazonq", "mcp.json",
                ".amazonq", "mcp.json",
                ".amazonq", "mcp.json");
        if (amazonQConfig != null) {
            targets.add(new ConfigTarget("Amazon Q Developer", amazonQConfig));
        }

        // 10. LM Studio
        Path lmStudioConfig = findConfigPath(os, home,
                ".lmstudio", "mcp.json",
                ".lmstudio", "mcp.json",
                ".lmstudio", "mcp.json");
        if (lmStudioConfig != null) {
            targets.add(new ConfigTarget("LM Studio", lmStudioConfig));
        }

        // 11. Current directory (generic)
        Path localConfig = Path.of(".mcp.json").toAbsolutePath().normalize();
        targets.add(new ConfigTarget("Current directory (.mcp.json)", localConfig));

        // ── Display with detection ──

        println(c(DIM) + "  ── Detected editors ──" + c(RST));
        List<ConfigTarget> detected = new ArrayList<>();
        List<ConfigTarget> notDetected = new ArrayList<>();

        for (ConfigTarget t : targets) {
            if (Files.exists(t.path()) || Files.exists(t.path().getParent())) {
                detected.add(t);
            } else {
                notDetected.add(t);
            }
        }

        // Reorder: detected first, then others
        List<ConfigTarget> ordered = new ArrayList<>(detected);
        if (!notDetected.isEmpty()) {
            ordered.addAll(notDetected);
        }

        for (int i = 0; i < ordered.size(); i++) {
            ConfigTarget t = ordered.get(i);
            boolean exists = Files.exists(t.path());
            boolean parentExists = Files.exists(t.path().getParent());
            String status = exists ? c(GRN) + " (exists)" + c(RST)
                          : parentExists ? c(YLW) + " (detected)" + c(RST)
                          : "";

            if (i == detected.size() && !notDetected.isEmpty()) {
                println(c(DIM) + "  ── Other editors ──" + c(RST));
            }

            String num = String.format("%2d", i + 1);
            println("  " + c(ORG) + num + c(RST) + ". " + t.name() + status);
            println("      " + c(DIM) + t.path() + c(RST));
        }

        println("");
        print(c(CYN) + "  Choose [1]: " + c(RST));
        String input = scanner.nextLine().trim();
        int choice = 0;
        if (!input.isBlank()) {
            try { choice = Integer.parseInt(input) - 1; } catch (NumberFormatException ignored) {}
        }
        if (choice < 0 || choice >= ordered.size()) choice = 0;

        println(c(GRN) + "  ✓ " + ordered.get(choice).name() + c(RST));
        println("");
        return ordered.get(choice).path();
    }

    record ConfigTarget(String name, Path path) {}

    /**
     * Build a config path based on OS. Returns null only if the path
     * makes no sense (e.g. missing APPDATA on Windows).
     * <p>
     * For Windows, paths are relative to %APPDATA%.
     * For Mac/Linux, paths are relative to home directory.
     *
     * @param winDir    Windows subdirectory under APPDATA (or home if starts with ".")
     * @param winFile   Windows filename
     * @param macDir    Mac subdirectory under home
     * @param macFile   Mac filename
     * @param linuxDir  Linux subdirectory under home
     * @param linuxFile Linux filename
     */
    private static Path findConfigPath(String os, Path home,
                                        String winDir, String winFile,
                                        String macDir, String macFile,
                                        String linuxDir, String linuxFile) {
        if (os.contains("win")) {
            if (winDir.startsWith(".")) {
                return home.resolve(winDir).resolve(winFile);
            }
            String appData = System.getenv("APPDATA");
            if (appData == null) return home.resolve(winDir).resolve(winFile);
            return Path.of(appData, winDir, winFile);
        } else if (os.contains("mac")) {
            if (macDir.startsWith(".") || macDir.startsWith("Library")) {
                return home.resolve(macDir).resolve(macFile);
            }
            return home.resolve("Library").resolve("Application Support").resolve(macDir).resolve(macFile);
        } else {
            return home.resolve(linuxDir).resolve(linuxFile);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 6: WRITE CONFIG
    // ═══════════════════════════════════════════════════════════════

    private static void writeConfig(Path configPath, Map<String, String> envVars) {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Detect the executable path for the "command" field
            String command = detectExecutablePath();

            // Build the notify-hub MCP server entry
            Map<String, Object> serverEntry = new LinkedHashMap<>();
            serverEntry.put("command", command);

            // If running from JAR, need args; if native, just the command
            if (command.equals("java")) {
                String jarPath = detectJarPath();
                serverEntry.put("args", List.of("-jar", jarPath));
            } else {
                serverEntry.put("args", List.of());
            }

            serverEntry.put("env", envVars);

            // Read existing config or create new
            Map<String, Object> config;
            if (Files.exists(configPath)) {
                String existing = Files.readString(configPath);
                config = mapper.readValue(existing, new TypeReference<>() {});
            } else {
                config = new LinkedHashMap<>();
            }

            // Ensure mcpServers exists
            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) config.computeIfAbsent(
                "mcpServers", k -> new LinkedHashMap<>()
            );

            // Add/overwrite notify-hub entry
            mcpServers.put("notify-hub", serverEntry);

            // Write
            Files.createDirectories(configPath.getParent());
            String json = mapper.writeValueAsString(config);
            Files.writeString(configPath, json);

            println(c(GRN) + "  ✓ Configuration saved to:" + c(RST));
            println("    " + configPath);
            println("");

        } catch (IOException e) {
            println(c(RED) + "  ✗ Error writing config: " + e.getMessage() + c(RST));
            println(c(YLW) + "  Try running with administrator privileges." + c(RST));
            println("");

            // Fallback: print JSON to console so user can copy
            printFallbackConfig(envVars, mapper);
        }
    }

    private static void printFallbackConfig(Map<String, String> envVars, ObjectMapper mapper) {
        try {
            String command = detectExecutablePath();
            Map<String, Object> serverEntry = new LinkedHashMap<>();
            serverEntry.put("command", command);
            if (command.equals("java")) {
                serverEntry.put("args", List.of("-jar", detectJarPath()));
            } else {
                serverEntry.put("args", List.of());
            }
            serverEntry.put("env", envVars);

            Map<String, Object> config = new LinkedHashMap<>();
            Map<String, Object> servers = new LinkedHashMap<>();
            servers.put("notify-hub", serverEntry);
            config.put("mcpServers", servers);

            println(c(YLW) + "  Copy this JSON manually into your config:" + c(RST));
            println("");
            println(mapper.writeValueAsString(config));
            println("");
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════
    //  STEP 7: SUCCESS
    // ═══════════════════════════════════════════════════════════════

    private static void printSuccess(Path configPath, List<ChannelDef> channels) {
        println(c(GRN) + c(BOLD) + "  ══════════════════════════════════════════" + c(RST));
        println(c(GRN) + c(BOLD) + "   Setup Complete!" + c(RST));
        println(c(GRN) + c(BOLD) + "  ══════════════════════════════════════════" + c(RST));
        println("");
        println("  " + c(BOLD) + "Channels configured:" + c(RST) + " " + channels.size());
        for (ChannelDef ch : channels) {
            println("    " + c(GRN) + "✓" + c(RST) + " " + ch.name());
        }
        println("");
        println("  " + c(BOLD) + "Config:" + c(RST) + " " + configPath);
        println("");
        println(c(BOLD) + "  Next steps:" + c(RST));
        println("  1. Restart Claude Desktop (or Claude Code)");
        println("  2. Try: " + c(CYN) + "\"Send a test notification via NotifyHub\"" + c(RST));
        println("");
        println(c(DIM) + "  Need help? https://gabrielbbaldez.github.io/notify-hub/docs.html" + c(RST));
        println(c(DIM) + "  Reconfigure anytime: notifyhub-mcp --setup" + c(RST));
        println("");
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Detects whether we're running as a native image or from a JAR.
     * Returns "java" if JAR, or the absolute executable path if native.
     */
    private static String detectExecutablePath() {
        // Check if running as GraalVM native image
        String imageCode = System.getProperty("org.graalvm.nativeimage.imagecode");
        if (imageCode != null) {
            // Native image — the command IS the executable
            return ProcessHandle.current().info().command()
                    .map(cmd -> Path.of(cmd).toAbsolutePath().toString())
                    .orElse("notifyhub-mcp");
        }
        // Running as JAR
        return "java";
    }

    /**
     * Finds the path to the current JAR file.
     * Spring Boot fat JARs use a nested class loader, so ProtectionDomain
     * won't give us the outer JAR path. We use sun.java.command instead.
     */
    private static String detectJarPath() {
        // 1. Best: parse the actual java command that was used
        String javaCmd = System.getProperty("sun.java.command", "");
        if (javaCmd.endsWith(".jar")) {
            // e.g. "C:\path\to\notify-mcp-0.9.0.jar" or "/path/to/notify-mcp.jar"
            Path p = Path.of(javaCmd.trim());
            if (Files.exists(p)) return p.toAbsolutePath().toString();
        }
        // sun.java.command might be "path/to/jar.jar --setup"
        for (String part : javaCmd.split("\\s+")) {
            if (part.endsWith(".jar")) {
                Path p = Path.of(part);
                if (Files.exists(p)) return p.toAbsolutePath().toString();
            }
        }

        // 2. Fallback: scan java.class.path
        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(System.getProperty("path.separator"))) {
            if (entry.contains("notify-mcp") && entry.endsWith(".jar")) {
                return Path.of(entry).toAbsolutePath().toString();
            }
        }

        // 3. Last resort: ~/.notifyhub default install location
        Path defaultInstall = Path.of(System.getProperty("user.home"), ".notifyhub", "notifyhub-mcp.jar");
        if (Files.exists(defaultInstall)) return defaultInstall.toString();

        return "notifyhub-mcp.jar";
    }

    /**
     * Try to enable ANSI escape codes on Windows.
     */
    private static void detectColorSupport() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String term = System.getenv("TERM");
        String wtSession = System.getenv("WT_SESSION"); // Windows Terminal

        if (os.contains("win")) {
            // Windows Terminal and modern PowerShell support ANSI
            if (wtSession != null || (term != null && !term.equals("dumb"))) {
                COLOR = true;
            } else if (console == null) {
                // Not a real terminal (piped, IDE, etc.)
                COLOR = false;
            } else {
                // Try enabling ANSI — works on Win 10 1511+
                COLOR = true;
                try {
                    new ProcessBuilder("cmd", "/c", "echo", "\033[0m")
                            .inheritIO().start().waitFor();
                } catch (Exception e) {
                    COLOR = false;
                }
            }
        } else {
            // Unix — almost always supports ANSI
            COLOR = console != null || (term != null && !term.equals("dumb"));
        }

        // Environment override
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null) COLOR = false;
    }

    private static void println(String msg) {
        System.out.println(msg);
    }

    private static void print(String msg) {
        System.out.print(msg);
        System.out.flush();
    }
}
