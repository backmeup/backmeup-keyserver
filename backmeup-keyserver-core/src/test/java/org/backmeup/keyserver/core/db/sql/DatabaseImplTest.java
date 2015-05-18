package org.backmeup.keyserver.core.db.sql;

import static org.junit.Assert.*;

import java.util.List;

import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.core.db.sql.SQLDatabaseImpl;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.KeyserverException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DatabaseImplTest {
    private static Database db;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            Class<?> clazz = Class.forName(Configuration.getProperty("backmeup.keyserver.db.connector"));
            db = (Database) clazz.newInstance();
        } catch (java.lang.ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new KeyserverException("could not load database connector class", e);
        }
        db.connect();
        assertTrue(db.isConnected());
        assertTrue(((SQLDatabaseImpl) db).checkForTable());
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
        ((SQLDatabaseImpl) db).cleanup();
    }

    @Test
    public void testGetEntry() throws DatabaseException {
        KeyserverEntry i = new KeyserverEntry("test_entry");
        i.setValue(new byte[] { 0, 1, 2 });
        i.setKeyringId(1);
        db.putEntry(i);

        KeyserverEntry e = db.getEntry("test_entry");
        assertNotNull(e);
        assertEquals("test_entry", e.getKey());
        assertArrayEquals(new byte[] { 0, 1, 2 }, e.getValue());
        assertEquals(1, e.getKeyringId());
        assertEquals(1, e.getVersion());
        assertEquals(i.getCreatedAt(), e.getCreatedAt());
        assertNotNull(e.getLastModified());
        assertNull(e.getTTL());

        e = db.getEntry("test_not_there");
        assertNull(e);
    }

    @Test
    public void testPutNewVersion() throws DatabaseException {
        KeyserverEntry i = new KeyserverEntry("test_entry");
        i.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(i);

        KeyserverEntry e = db.getEntry("test_entry");
        assertNotNull(e);
        assertEquals("test_entry", e.getKey());
        assertArrayEquals(new byte[] { 0, 1, 2 }, e.getValue());
        assertEquals(1, e.getVersion());
        assertNotNull(e.getLastModified());
        assertNull(e.getTTL());

        i.expire();
        db.updateTTL(i);

        e = db.getEntry("test_entry");
        assertNull(e);

        i.setValue(new byte[] { 3, 4 });
        db.putEntry(i);

        KeyserverEntry e2 = db.getEntry("test_entry");
        assertNotNull(e2);
        assertEquals("test_entry", e2.getKey());
        assertArrayEquals(new byte[] { 3, 4 }, e2.getValue());
        assertEquals(2, e2.getVersion());
        assertEquals(e2.getLastModified(), i.getLastModified());
        assertNull(e2.getTTL());
    }

    @Test
    public void testGetEntryWithVersion() throws DatabaseException {
        KeyserverEntry i = new KeyserverEntry("test_entry");
        i.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(i);

        i.setValue(new byte[] { 3, 4 });
        db.putEntry(i);

        KeyserverEntry e = db.getEntry("test_entry", 1);
        assertNotNull(e);
        assertEquals("test_entry", e.getKey());
        assertEquals(1, e.getVersion());
        assertArrayEquals(new byte[] { 0, 1, 2 }, e.getValue());

        e = db.getEntry("test_entry", 2);
        assertNotNull(e);
        assertEquals("test_entry", e.getKey());
        assertEquals(2, e.getVersion());
        assertArrayEquals(new byte[] { 3, 4 }, e.getValue());
    }

    @Test
    public void testPutEntry() throws DatabaseException {
        KeyserverEntry e = new KeyserverEntry("test_entry");
        e.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(e);
    }

    @Test
    public void testDuplicateEntry() throws DatabaseException {
        KeyserverEntry e = new KeyserverEntry("test_duplicate");
        e.setValue(new byte[] { 0, 1, 2 });
        db.putEntry(e);

        try {
            db.putEntry(e);
        } catch (DatabaseException de) {
            assertTrue(de.getMessage().contains("ENTRY_EKEY") || de.getMessage().contains("entry_ekey"));
        }
    }
    
    @Test
    public void testSearchEntry() throws DatabaseException {
        KeyserverEntry e = new KeyserverEntry("test_search01");
        e.setValue(new byte[]{0});
        db.putEntry(e);
        e.setValue(new byte[]{1});
        db.putEntry(e);
        e = new KeyserverEntry("test_search02");
        e.setValue(new byte[]{0});
        db.putEntry(e);

        List<KeyserverEntry> entries = db.searchByKey("test_search%", false, false);
        assertEquals(2, entries.size());
        e = entries.get(0);
        assertEquals("test_search01", e.getKey());
        assertEquals(2, e.getVersion());
        e = entries.get(1);
        assertEquals("test_search02", e.getKey());
        assertEquals(1, e.getVersion());
        
        entries = db.searchByKey("test_search%", true, false);
        assertEquals(3, entries.size());
        e = entries.get(0);
        assertEquals("test_search01", e.getKey());
        assertEquals(2, e.getVersion());
        e = entries.get(1);
        assertEquals("test_search01", e.getKey());
        assertEquals(1, e.getVersion());
        e = entries.get(2);
        assertEquals("test_search02", e.getKey());
        assertEquals(1, e.getVersion());
    }

}
