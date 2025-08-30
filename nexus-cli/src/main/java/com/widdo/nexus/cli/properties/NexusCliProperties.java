package com.widdo.nexus.cli.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * NexusCliProperties
 *
 * @author XYL
 * @date 2025/08/27 14:26
 * @since 0.0.1-SNAPSHOT
 */
@Data
@ConfigurationProperties(prefix = "nexus.cli")
public class NexusCliProperties {
    private String configFile = "~/.nexus/config.yml";
    private OutputFormat defaultOutput = OutputFormat.TABLE;
    private int timeout = 30;
    private boolean colorEnabled = true;

    public enum OutputFormat {
        TABLE, JSON, CSV, YAML
    }
}
