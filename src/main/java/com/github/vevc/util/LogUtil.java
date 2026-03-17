package com.github.vevc.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging utility
 * @author vevc
 */
public final class LogUtil {

    private static Logger logger;

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
    }

    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    public static void warning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    public static void error(String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }

    public static void error(String message, Throwable e) {
        if (logger != null) {
            logger.log(Level.SEVERE, message, e);
        }
    }

    private LogUtil() {
        throw new IllegalStateException("Utility class");
    }
}
