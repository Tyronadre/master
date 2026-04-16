package de.tyro.mcnetwork.simulation.exceptions;

import de.tyro.mcnetwork.simulation.IP;

public class DestinationUnreachableException extends Exception {
    public final IP destination;


    public DestinationUnreachableException(IP destination) {
        this.destination = destination;
    }
}
