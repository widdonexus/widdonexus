package com.widdo.nexus.cli;

import com.widdo.nexus.cli.properties.NexusCliProperties;
import com.widdo.nexus.core.log.NexusLogger;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AdminCommand
 *
 * @author XYL
 * @date 2025/08/27 14:28
 * @since 0.0.1-SNAPSHOT
 */
@SpringBootApplication
@EnableConfigurationProperties(NexusCliProperties.class)
public class NexusCliApplication {

    private static final NexusLogger logger = NexusLogger.getLogger(NexusCliApplication.class);


    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(NexusCliApplication.class);
            app.setBannerMode(Banner.Mode.OFF);

            // 如果没有参数，启动交互式shell
            if (args.length == 0) {
                logger.info("Starting interactive shell...");
                args = new String[]{"--spring.shell.interactive.enabled=true"};
            }

            app.run(args);
        } catch (Exception ex) {
            System.err.println("Failed to start CLI: " + ex.getMessage());
            System.exit(1);
        }
    }

    @Bean
    public ApplicationRunner welcomeMessage() {
        return args -> {
            if (args.getNonOptionArgs().isEmpty()) {
                System.out.println("╔══════════════════════════════════════════════════╗");
                System.out.println("║               Widdo Nexus CLI v1.0.0             ║");
                System.out.println("║        The Command Line for Graph Database       ║");
                System.out.println("╚══════════════════════════════════════════════════╝");
                System.out.println("Type 'help' to see available commands");
                System.out.println();
            }
        };
    }

}
