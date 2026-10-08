package orm;

import db.Database;

import java.util.function.Consumer;
import java.util.function.Function;

/** Runs work in a fresh session and flushes it when the work returns normally. */
public class Transactions {
    private final Database db;

    public Transactions(Database db) {
        this.db = db;
    }

    public <R> R call(Function<Session, R> work) {
        Session session = new Session(db);
        R result = work.apply(session);
        session.flush();
        session.runAfterCommit();
        return result;
    }

    /** For queries: entities are loaded the same way but nothing is written back. */
    public <R> R read(Function<Session, R> work) {
        return work.apply(new Session(db));
    }

    public void run(Consumer<Session> work) {
        call(s -> {
            work.accept(s);
            return null;
        });
    }
}
