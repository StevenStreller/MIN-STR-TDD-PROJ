package de.hsh.service;

import de.hsh.dto.Event;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventServiceTest {

    private EventService eventService;
    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        eventService = new EventService();
        event1 = new Event(UUID.randomUUID(), "Konzert 1", new Date(), 50.0, 100, "organizer@mail.com");
        event2 = new Event(UUID.randomUUID(), "Konzert 2", new Date(), 60.0, 150, "organizer@mail.com");
    }

    @AfterAll
    static void tearDown() {
        String[] testFiles = {
                "empty_events.ser",
                "single_event.ser",
                "non_existent_file.ser",
                "corrupted_events.ser",
                "new_event.ser"
        };

        for (String filename : testFiles) {
            File file = new File(filename);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Datei entfernt: " + filename);
                } else {
                    System.out.println("Fehler beim Löschen der Datei: " + filename);
                }
            }
        }
    }

    @Test
    public void eventsListIsInitiallyEmpty() {
        assertTrue(eventService.getEvents().isEmpty(), "Die Event-Liste sollte zu Beginn leer sein.");
    }

    @Test
    public void addEventSuccessfully() {
        Event event = new Event(UUID.randomUUID(), "Test Event", new Date(), 100.0, 50, "organizer@mail.com");
        eventService.addEvent(event);

        assertFalse(eventService.getEvents().isEmpty(), "Die Event-Liste sollte nach dem Hinzufügen eines Events nicht leer sein.");
        assertTrue(eventService.getEvents().contains(event), "Das hinzugefügte Event sollte in der Liste enthalten sein.");
    }

    @Test
    public void addEventWhenListContainsNoMatchingEvent() {
        Event event1 = new Event(UUID.randomUUID(), "Test Event 1", new Date(), 100.0, 50, "organizer@mail.com");
        Event event2 = new Event(UUID.randomUUID(), "Test Event 2", new Date(), 150.0, 30, "organizer@mail.com");
        eventService.addEvent(event1);

        assertFalse(eventService.getEvents().contains(event2), "Die Event-Liste sollte das Event nicht enthalten, das nicht hinzugefügt wurde.");
    }

    @Test
    public void addMultipleEvents() {
        Event event1 = new Event(UUID.randomUUID(), "Test Event 1", new Date(), 100.0, 50, "organizer@mail.com");
        Event event2 = new Event(UUID.randomUUID(), "Test Event 2", new Date(), 150.0, 30, "organizer@mail.com");
        eventService.addEvent(event1);
        eventService.addEvent(event2);

        assertEquals(2, eventService.getEvents().size(), "Die Event-Liste sollte nach dem Hinzufügen von zwei Events genau zwei Events enthalten.");
        assertTrue(eventService.getEvents().contains(event1), "Die Event-Liste sollte das erste Event enthalten.");
        assertTrue(eventService.getEvents().contains(event2), "Die Event-Liste sollte das zweite Event enthalten.");
    }

    @Test
    void serializationAndDeserialization() {
        // Add events to the list
        eventService.addEvent(event1);
        eventService.addEvent(event2);

        // Serialize the event list
        String filename = "events.ser";
        eventService.serializeEvents(filename);

        // Create a new EventService and deserialize the list
        EventService newEventService = new EventService();
        newEventService.deserializeEvents(filename);

        // Check that the deserialized list contains the same events
        List<Event> deserializedEvents = newEventService.getEvents();
        assertEquals(2, deserializedEvents.size(), "Deserialized event list should have the same size as the original list");
        assertTrue(deserializedEvents.contains(event1), "Event 1 should be present in the deserialized list");
        assertTrue(deserializedEvents.contains(event2), "Event 2 should be present in the deserialized list");
    }

    @Test
    void deserializationWithCorruptedFile() {
        String filename = "corrupted_events.ser";
        File file = new File(filename);

        // Create a corrupted file
        try {
            if (file.exists()) file.delete();
            file.createNewFile();
            // Write some invalid content to the file (not a valid serialized object)
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write("Invalid data");
            }

            EventService newEventService = new EventService();

            RuntimeException exception = assertThrows(RuntimeException.class, () -> newEventService.deserializeEvents(filename));
            assertEquals("Deserialisierung fehlgeschlagen", exception.getMessage(), "Deserialization should fail with the correct message when the file is corrupted");
        } catch (IOException e) {
            fail("Failed to create corrupted file: " + e.getMessage());
        }
    }

    @Test
    void serializeEventsThrowsRuntimeExceptionOnIOException() {
        // Create a file that is not writable, e.g., a read-only file.
        String filename = "events.ser";
        File file = new File(filename);

        try {
            if (file.exists()) file.delete();
            file.createNewFile();

            // Make the file read-only to simulate an IOException during serialization
            file.setReadOnly();

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                eventService.serializeEvents(filename);
            });

            assertEquals("Serialisierung fehlgeschlagen", exception.getMessage(), "Expected RuntimeException with message 'Serialisierung fehlgeschlagen'");

        } catch (IOException e) {
            fail("Failed to create or modify file: " + e.getMessage());
        } finally {
            // Clean up: Make the file writable again and delete
            file.setWritable(true);
            file.delete();
        }
    }

}