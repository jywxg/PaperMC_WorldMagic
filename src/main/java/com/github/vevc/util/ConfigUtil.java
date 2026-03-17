package com.github.vevc.util;

import com.github.vevc.constant.AppConst;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Configuration utility with RSA encryption
 * @author vevc
 */
public final class ConfigUtil {

    private static final String CONFIG_RELATIVE_PATH = "plugins/application.properties";
    private static final String CONFIG_DIR = "config";

    /**
     * Load configuration from file (with encryption support)
     */
    public static Properties loadConfiguration() {
        File baseDir = new File(System.getProperty("user.dir"));
        File plainConfigFile = new File(baseDir, CONFIG_RELATIVE_PATH);
        File encryptedConfigDir = new File(baseDir, CONFIG_DIR);

        try {
            // First run: read plain config, encrypt and save
            if (plainConfigFile.exists()) {
                Properties props = loadPropertiesFromFile(plainConfigFile.toPath());
                initDefaultConfig(props);
                
                StringWriter writer = new StringWriter();
                props.store(writer, null);
                persistEncryptedConfig(writer.toString(), encryptedConfigDir.toPath());
                Files.delete(plainConfigFile.toPath());
                
                LogUtil.info("Configuration encrypted and saved");
                return props;
            }

            // Subsequent runs: read encrypted config
            Optional<String> encryptedContent = readEncryptedConfig(encryptedConfigDir.toPath());
            if (encryptedContent.isEmpty()) {
                LogUtil.error("No configuration found");
                return null;
            }

            String decryptedContent = RsaUtil.decryptByPrivateKey(encryptedContent.get(), AppConst.PRIVATE_KEY);
            Properties props = new Properties();
            StringReader reader = new StringReader(decryptedContent);
            props.load(reader);
            
            LogUtil.info("Configuration loaded successfully");
            return props;

        } catch (Exception e) {
            LogUtil.error("Failed to load configuration", e);
            return null;
        }
    }

    private static Properties loadPropertiesFromFile(Path path) throws IOException {
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

    private static void initDefaultConfig(Properties props) {
        // Set defaults for missing values
        props.putIfAbsent(AppConst.DOMAIN, "vevc.github.com");
        props.putIfAbsent(AppConst.EMAIL, "admin@example.com");
        props.putIfAbsent(AppConst.ENABLED_PROTOCOLS, "hysteria2,vmess-ws,anytls");
        
        // Hysteria2 defaults
        props.putIfAbsent(AppConst.HY2_PORT, "8443");
        props.putIfAbsent(AppConst.HY2_PASSWORD, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.HY2_UP_MBPS, "100");
        props.putIfAbsent(AppConst.HY2_DOWN_MBPS, "100");
        props.putIfAbsent(AppConst.HY2_SNI, "itunes.apple.com");
        
        // Vmess-WS defaults
        props.putIfAbsent(AppConst.VMESS_PORT, "443");
        props.putIfAbsent(AppConst.VMESS_UUID, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.VMESS_PATH, "/vmess");
        
        // AnyTLS defaults
        props.putIfAbsent(AppConst.ANYTLS_PORT, "8444");
        props.putIfAbsent(AppConst.ANYTLS_PASSWORD, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.ANYTLS_SNI, "www.apple.com");
        
        // Tuic defaults
        props.putIfAbsent(AppConst.TUIC_PORT, "25565");
        props.putIfAbsent(AppConst.TUIC_UUID, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.TUIC_PASSWORD, UUID.randomUUID().toString().substring(0, 8));
        props.putIfAbsent(AppConst.TUIC_VERSION, "1.6.5");
        
        // General
        props.putIfAbsent(AppConst.SSHX_ENABLED, "true");
        props.putIfAbsent(AppConst.REMARKS_PREFIX, "vevc");
        props.putIfAbsent(AppConst.SELF_SIGN_CERT, "true");
    }

    private static void persistEncryptedConfig(String content, Path configDir) throws Exception {
        Files.createDirectories(configDir);
        String encryptedContent = RsaUtil.encryptByPublicKey(content, AppConst.PUBLIC_KEY);
        String fileName = Md5Util.md5(encryptedContent);
        Path target = configDir.resolve(fileName);
        Files.writeString(target, encryptedContent, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Optional<String> readEncryptedConfig(Path configDir) throws IOException {
        if (!Files.exists(configDir)) {
            return Optional.empty();
        }

        File[] files = configDir.toFile().listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return Optional.empty();
        }

        for (File file : files) {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (Md5Util.md5(content).equalsIgnoreCase(file.getName())) {
                return Optional.of(content);
            }
        }

        return Optional.empty();
    }

    private ConfigUtil() {
        throw new IllegalStateException("Utility class");
    }
}
