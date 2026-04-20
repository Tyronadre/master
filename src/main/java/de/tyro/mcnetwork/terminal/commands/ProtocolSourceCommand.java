package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.simulation.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ProtocolSourceCommand extends Command {
    public ProtocolSourceCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        IRoutingProtocol protocol = terminal.getNode().getRoutingProtocol();
        if (protocol == null) {
            println("No routing protocol is currently set.");
            return;
        }

        String className = protocol.getClass().getSimpleName();
        String packageName = protocol.getClass().getPackageName();

        // Convert package name to resource path
        String resourcePath = packageName.replace('.', '/') + "/" + className + ".java";

        String sourceCode = null;

        // First, try to load from resources (when running from JAR or built project)
        try (InputStream is = ProtocolSourceCommand.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                sourceCode = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // Try next method
        }

        // If not found in resources, try to read from disk (development mode)
        if (sourceCode == null) {
            String[] possiblePaths = {
                    "src/main/java/" + resourcePath,
                    "src\\main\\java\\" + resourcePath.replace('/', '\\'),
            };

            for (String path : possiblePaths) {
                try {
                    sourceCode = Files.readString(Paths.get(path));
                    break;
                } catch (IOException e) {
                    // Try next path
                }
            }
        }

        if (sourceCode == null) {
            println("Could not find source file for protocol: " + className);
            println("Expected resource path: " + resourcePath);
            return;
        }

        println("=".repeat(80));
        println("Source code of " + className + " (" + packageName + ")");
        println("=".repeat(80));
        println(sourceCode);
        println("=".repeat(80));
    }

    @Override
    public String getName() {
        return "protocolsource";
    }

    @Override
    protected List<String> getHelp() {
        return List.of(
                "Displays the complete source code of the current routing protocol.",
                "Usage: protocolsource"
        );
    }
}

