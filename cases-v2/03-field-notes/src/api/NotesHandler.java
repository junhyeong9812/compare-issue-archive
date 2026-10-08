package api;

import domain.Note;
import service.NoteService;

/** Web console: /notes */
public class NotesHandler {
    private final NoteService notes;

    public NotesHandler(NoteService notes) {
        this.notes = notes;
    }

    Response create(Request req) {
        Note n = notes.create(req.param("folder"), req.param("title"), req.param("body"));
        return new Response(201, Json.note(n));
    }

    Response edit(Request req, String id) {
        return new Response(200, Json.note(notes.edit(id, req.param("title"), req.param("body"))));
    }

    Response move(Request req, String id) {
        return new Response(200, Json.note(notes.move(id, req.param("folder"))));
    }

    Response get(String id) {
        return new Response(200, Json.note(notes.get(id)));
    }
}
