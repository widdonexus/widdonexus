package com.widdo.nexus.cli.shell;

import com.widdo.nexus.core.support.template.AbstractNexusAdvancedTemplate;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * InteractiveShell
 *
 * @author XYL
 * @date 2025/08/27 14:27
 * @since 0.0.1-SNAPSHOT
 */
@Component
public class InteractiveShell {

    private final AbstractNexusAdvancedTemplate template;
    private final PromptProvider promptProvider;

    public InteractiveShell(AbstractNexusAdvancedTemplate template) {
        this.template = template;
        this.promptProvider = () -> new AttributedString("nexus> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
    }

    @ShellMethod(key = "shell", value = "Enter interactive mode")
    public void enterInteractiveMode() {
        System.out.println("Entering interactive mode. Type 'exit' to quit.");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("nexus> ");
            String line = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                break;
            }

            if (!line.isEmpty()) {
                executeInteractiveCommand(line);
            }
        }
    }

    private void executeInteractiveCommand(String command) {
        try {
            if (command.toLowerCase().startsWith("match") ||
                    command.toLowerCase().startsWith("return")) {
                // 执行Cypher查询
                List<Map<String, Object>> results = template.executeCypher(
                        command, Collections.emptyMap(), List.class);

                renderResults(results);
            } else {
                System.out.println("Unknown command: " + command);
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void renderResults(List<Map<String, Object>> results) {
        // 简化版结果渲染
        if (results.isEmpty()) {
            System.out.println("No results");
            return;
        }

        for (Map<String, Object> row : results) {
            row.forEach((key, value) ->
                    System.out.println(key + ": " + value));
            System.out.println("---");
        }
    }
}