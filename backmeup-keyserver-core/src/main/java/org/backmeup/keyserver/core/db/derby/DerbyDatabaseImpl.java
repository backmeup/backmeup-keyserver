package org.backmeup.keyserver.core.db.derby;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.core.db.sql.SQLDatabaseImpl;

/**
 * Database implementation for Derby keyserver database.
 * Needs a special disconnect handling for saving changes to files.
 * @author wolfgang
 *
 */
public class DerbyDatabaseImpl extends SQLDatabaseImpl {
    @Override
    public synchronized void disconnect() throws DatabaseException {
        super.disconnect();

        try (Connection c = DriverManager.getConnection("jdbc:derby:;shutdown=true")) {
            // do nothing because of shutdown
        } catch (SQLException e) {
            if (!"XJ015".equals(e.getSQLState())) {
                throw new DatabaseException(e);
            }
        }
    }
}