package server.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

public class TransactionManager {
    private final SessionFactory sessionFactory;

    public TransactionManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void runTransaction(Consumer<Session> dbTransactionCode) {
        runTransaction(session -> {
            dbTransactionCode.accept(session);
            return null;
        });
    }

    public <T> T runTransaction(Function<Session, T> dbTransactionCode) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            T result = dbTransactionCode.apply(session);
            tx.commit();
            return result;
        } catch (Throwable th) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw th;
        }
    }
}
