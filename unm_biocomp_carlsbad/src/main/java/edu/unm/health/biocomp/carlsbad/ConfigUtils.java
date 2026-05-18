package edu.unm.health.biocomp.carlsbad;

import javax.servlet.ServletConfig;

/**
 * Utility to load configuration from Environment Variables, System Properties, or Servlet Config.
 */
public class ConfigUtils {
    public static String getConfig(ServletConfig conf, String name, String defaultValue) {
        String val = System.getenv(name);
        if (val != null && !val.isEmpty()) return val;
        
        val = System.getProperty(name);
        if (val != null && !val.isEmpty()) return val;
        
        val = conf.getInitParameter(name);
        if (val != null && !val.isEmpty()) return val;
        
        return defaultValue;
    }

    public static Integer getConfigInt(ServletConfig conf, String name, Integer defaultValue) {
        String val = getConfig(conf, name, null);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static Boolean getConfigBool(ServletConfig conf, String name, Boolean defaultValue) {
        String val = getConfig(conf, name, null);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }
}
