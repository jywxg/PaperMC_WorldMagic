package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.LogUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SSHX web terminal service implementation
 * Provides browser-based SSH access via sshx.io
 * @author vevc
 */
public class SshxServiceImpl extends AbstractAppService {

    private static final String APP_NAME = "sshx";           // Binary name
    private static final String INFO_FILE = "s.txt";         // Link info file
    private static final int CLEANUP_DELAY_SECONDS = 600;    // 10 minutes

    private static final String SSHX_DOWNLOAD_URL = 
            "https://github.com/ekzhang/sshx/releases/latest/download/sshx-linux-%s";

    @Override
    protected String getAppDownloadUrl(String appVersion) {
        String arch = OS_IS_ARM ? "arm64" : "x86_64";
        return String.format(SSHX_DOWNLOAD_URL, arch.contains("arm") ? "arm64" : "x86_64");
    }

    @Override
    public void install(AppConfig appConfig) throws Exception {
        File workDir = this.initWorkDir();
        File destFile = new File(workDir, APP_NAME);

        // Download sshx binary
        String downloadUrl = this.getAppDownloadUrl(null);
        LogUtil.info("SSHX download url: " + downloadUrl);
        this.download(downloadUrl, destFile);
        this.setExecutePermission(destFile.toPath());
        LogUtil.info("SSHX installed successfully");
    }

    @Override
    public void startup() {
        File workDir = this.getWorkDir();
        File sshxBinary = new File(workDir, APP_NAME);
        File infoFile = new File(workDir, INFO_FILE);

        try {
            while (Files.exists(sshxBinary.toPath())) {
                ProcessBuilder pb = new ProcessBuilder(
                        sshxBinary.getAbsolutePath(),
                        "--shell", "/bin/sh",
                        "--no-open"
                );
                pb.directory(workDir);
                pb.redirectOutput(new File(workDir, "sshx.out"));
                pb.redirectError(new File(workDir, "sshx.err"));

                LogUtil.info("Starting SSHX server...");
                Process process = pb.start();

                // Capture output to get link
                Thread outputReader = new Thread(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        
                        File outFile = new File(workDir, "sshx.out");
                        if (outFile.exists()) {
                            String output = Files.readString(outFile.toPath(), StandardCharsets.UTF_8);
                            
                            // Extract sshx link
                            Pattern pattern = Pattern.compile("https://sshx\\.io/s/[A-Za-z0-9]+#?[A-Za-z0-9]*");
                            Matcher matcher = pattern.matcher(output);
                            
                            if (matcher.find()) {
                                String sshxLink = matcher.group();
                                
                                // Write info file
                                String content = "SSHX Link: " + sshxLink + "\n" +
                                        "Generated at: " + new java.util.Date() + "\n" +
                                        "Open in browser to access terminal\n";
                                Files.writeString(infoFile.toPath(), content);
                                LogUtil.info("SSHX link saved to .cache/s.txt");
                            }
                        }
                    } catch (Exception e) {
                        LogUtil.error("Failed to capture SSHX link", e);
                    }
                });
                outputReader.start();

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    LogUtil.info("SSHX process exited normally");
                    break;
                } else {
                    LogUtil.info("SSHX process exited with code: " + exitCode + ", restarting...");
                    TimeUnit.SECONDS.sleep(5);
                }
            }
        } catch (Exception e) {
            LogUtil.error("SSHX startup failed", e);
        }
    }

    @Override
    public void clean() {
        File workDir = this.getWorkDir();
        File infoFile = new File(workDir, INFO_FILE);
        File outFile = new File(workDir, "sshx.out");
        File errFile = new File(workDir, "sshx.err");
        File sshxBinary = new File(workDir, APP_NAME);

        try {
            // Clean after 10 minutes
            TimeUnit.SECONDS.sleep(CLEANUP_DELAY_SECONDS);

            Files.deleteIfExists(infoFile.toPath());
            Files.deleteIfExists(outFile.toPath());
            Files.deleteIfExists(errFile.toPath());
            Files.deleteIfExists(sshxBinary.toPath());

            LogUtil.info("SSHX evidence files cleaned");
        } catch (Exception e) {
            LogUtil.error("SSHX cleanup failed", e);
        }
    }
}
