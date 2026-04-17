package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.simulation.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;

import java.lang.reflect.Field;
import java.util.List;

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
                println("- " + field.getName() + ": " + value);
            } catch (IllegalAccessException e) {
                println("- " + field.getName() + ": <inaccessible>");
            }
        }
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
