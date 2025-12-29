package com.widdo.nexus.examples;
/**
 * NexusExamplesApplication.
 * <p>
 * Nexus example启动类
 *
 * @author XYL
 * @date 2025/08/27 23:20
 * @since 0.0.1-SNAPSHOT
 */

import com.widdo.nexus.starter.annotation.EnableNexus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableNexus
@SpringBootApplication
public class NexusExamplesApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusExamplesApplication.class, args);
    }

}
