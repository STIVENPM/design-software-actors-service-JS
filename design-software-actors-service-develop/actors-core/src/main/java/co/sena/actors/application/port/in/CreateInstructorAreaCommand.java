package co.sena.actors.application.port.in;

import java.util.UUID;

/**
 * Create instructor area command (E1).
 */
public record CreateInstructorAreaCommand(
        UUID instructorId,
        String areaName,
        boolean primary) {
}