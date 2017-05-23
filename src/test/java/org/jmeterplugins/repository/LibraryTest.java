package org.jmeterplugins.repository;

import org.junit.Test;

import static org.junit.Assert.*;

public class LibraryTest {
    @Test
    public void testFlow() throws Exception {
        Library lib1 = new Library("name", "9.9.9", "LINK");

        Library lib2 = new Library();
        lib2.setName("name2");
        lib2.setVersion("9.9.8");
        assertEquals("name2>=9.9.8", lib2.getFullName());

        lib2.setLink("LINK2");

        assertEquals("name2", lib2.getName());
        assertEquals("9.9.8", lib2.getVersion());
        assertEquals("LINK2", lib2.getLink());

        assertEquals(1, Library.versionComparator.compare(lib1, lib2));

        lib2.setVersion("9.9.10");
        assertEquals(-1, Library.versionComparator.compare(lib1, lib2));

        lib1.setVersion("9.9");
        assertEquals(-1, Library.versionComparator.compare(lib1, lib2));

        lib1.setVersion("1.1");
        lib2.setVersion("1.2");
        assertEquals(-1, Library.versionComparator.compare(lib1, lib2));

        lib2.setVersion("1.1");
        assertEquals(0, Library.versionComparator.compare(lib1, lib2));
    }
}