package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.simulation.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtocolVarsCommand extends Command {
    public ProtocolVarsCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        IRoutingProtocol protocol = terminal.getNode().getRoutingProtocol();
        if (protocol == null) {
            println("No routing protocol is currently set.");
            return;
        }

        Class<?> clazz = protocol.getClass();
        Field[] fields = clazz.getDeclaredFields();

        println("Variables of " + clazz.getSimpleName() + ":");
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(protocol);
                List<String> formattedLines = printFormattedValue(value, "  ");

                if (formattedLines.isEmpty()) {
                    println("- " + field.getName() + ": null");

                } else if (formattedLines.size() == 1) {
                    println("- " + field.getName() + ": " + formattedLines.getFirst());

                } else {
                    println("- " + field.getName() + ":");
                    for (String line : formattedLines) {
                        println(line);
                    }
                }

            } catch (IllegalAccessException e) {
                println("- " + field.getName() + ": <inaccessible>");
            }
        }
    }

    private List<String> printFormattedValue(Object value, String indent) {
        List<String> lines = new ArrayList<>();
        if (value == null) {
            lines.add(indent + "null");
        } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                lines.add(indent + "{}");
            } else {
                lines.add(indent + "Map:");
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    List<String> subLines = printFormattedValue(entry.getValue(), indent + "  ");
                    if (subLines.size() == 1) {
                        lines.add(indent + "  " + entry.getKey() + ": " + subLines.get(0));
                    } else {
                        lines.add(indent + "  " + entry.getKey() + ":");
                        for (String subLine : subLines) {
                            lines.add(indent + "    " + subLine);
                        }
                    }
                }
            }
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                lines.add(indent + "[]");
            } else {
                lines.add(indent + "List:");
                for (Object item : list) {
                    List<String> subLines = printFormattedValue(item, indent + "  ");
                    if (subLines.size() == 1) {
                        lines.add(indent + "  " + subLines.get(0));
                    } else {
                        lines.addAll(subLines.stream().map(subLine -> indent + "  " + subLine).toList());
                    }
                }
            }
        } else {
            lines.add(indent + value);
        }
        return lines;
    }

    @Override
    public String getName() {
        return "protocolvars";
    }

    @Override
    protected List<String> getHelp() {
        return List.of("Displays ALL the variables and their values of the current routing protocol using reflection.");
    }
}
