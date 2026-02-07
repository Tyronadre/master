package de.tyro.mcnetwork.routing.exceptions;

import de.tyro.mcnetwork.routing.IP;

public class DestinationUnreachableException extends Exception {
    public final IP destination;


    public DestinationUnreachableException(IP destination) {
        this.destination = destination;
    }
}
