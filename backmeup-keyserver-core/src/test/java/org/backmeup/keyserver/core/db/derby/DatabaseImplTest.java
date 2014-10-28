package org.backmeup.keyserver.core.db.derby;

import static org.junit.Assert.*;

import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.core.db.derby.DatabaseImpl;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DatabaseImplTest {
    private static Database db;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        db = new DatabaseImpl();
        db.connect();
        assertTrue(db.isConnected());
        assertTrue(((DatabaseImpl) db).checkForTable());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (db.isConnected()) {
            db.disconnect();
            assertFalse(db.isConnected());
        }
    }

    @Before
    public void setUp() throws Exception {
        assertTrue(db.isConnected());
        ((DatabaseImpl) db).dropTable();
        ((DatabaseImpl) db).prepareTable();
    }

    @Test
    public void testGetEntry() throws DatabaseException {
        KeyserverEntry i = new KeyserverEntry("test");
        i.setValue(new byte[] { 0, 1, 2 });
        i.setKeyringId(1);
        db.putEntry(i);

        KeyserverEntry e = db.getEntry("test");
        assertNotNull(e);
        assertEquals("test", e.getKey());
        assertArrayEquals(new byte[] { 0, 1, 2 }, e.getValue());
        assertEquals(1, e.getKeyringId());
        assertEquals(1, e.getVersion());
        assertEquals(i.getCreatedAt(), e.getCreatedAt());
        assertNotNull(e.getLastModified());
        assertNull(e.getTTL());

        e = db.getEntry("not there");
        assertNull(e);
    }

    @Test
    public void testPutNewVersion() throws DatabaseException {
        KeyserverEntry i = new KeyserverEntry("test");
        i.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(i);

        KeyserverEntry e = db.getEntry("test");
        assertNotNull(e);
        assertEquals("test", e.getKey());
        assertArrayEquals(new byte[] { 0, 1, 2 }, e.getValue());
        assertEquals(1, e.getVersion());
        assertNotNull(e.getLastModified());
        assertNull(e.getTTL());

        i.expire();
        db.updateTTL(i);

        e = db.getEntry("test");
        assertNull(e);

        i.setValue(new byte[] { 3, 4 });
        db.putEntry(i);

        KeyserverEntry e2 = db.getEntry("test");
        assertNotNull(e2);
        assertEquals("test", e2.getKey());
        assertArrayEquals(new byte[] { 3, 4 }, e2.getValue());
        assertEquals(2, e2.getVersion());
        assertEquals(e2.getLastModified(), i.getLastModified());
        assertNull(e2.getTTL());
    }

    @Test
    public void testGetEntryWithVersion() throws DatabaseException {
        KeyserverEntry i = new KeyserverEntry("test");
        i.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(i);

        i.setValue(new byte[] { 3, 4 });
        db.putEntry(i);

        KeyserverEntry e = db.getEntry("test", 1);
        assertNotNull(e);
        assertEquals("test", e.getKey());
        assertEquals(1, e.getVersion());
        assertArrayEquals(new byte[] { 0, 1, 2 }, e.getValue());

        e = db.getEntry("test", 2);
        assertNotNull(e);
        assertEquals("test", e.getKey());
        assertEquals(2, e.getVersion());
        assertArrayEquals(new byte[] { 3, 4 }, e.getValue());
    }

    @Test
    public void testPutEntry() throws DatabaseException {
        KeyserverEntry e = new KeyserverEntry("test");
        e.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(e);
    }

    @Test
    public void testDuplicateEntry() throws DatabaseException {
        KeyserverEntry e = new KeyserverEntry("duplicate");
        e.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(e);

        try {
            db.putEntry(e);
        } catch (DatabaseException de) {
            assertTrue(de.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException);
            assertTrue(de.getMessage().contains("ENTRY_EKEY"));
        }
    }

}
