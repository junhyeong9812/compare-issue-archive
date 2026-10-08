package sync;

import java.time.Instant;

/** One note edit made on a device, possibly hours before it is uploaded. */
public record IncomingChange(String noteId, String folderId, String title, String body, Instant editedAt, boolean deleted) {}
