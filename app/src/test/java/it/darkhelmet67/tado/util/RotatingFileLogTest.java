package it.darkhelmet67.tado.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class RotatingFileLogTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void rotatesAndKeepsAtMostThreeFiles() throws Exception {
        RotatingFileLog log = new RotatingFileLog(folder.getRoot(), "t.log", 100, 3);
        String line = "0123456789012345678901234567890123456789"; // 40 chars + newline
        for (int i = 0; i < 20; i++) log.append(line + i);

        assertTrue(new File(folder.getRoot(), "t.log").isFile());
        assertTrue(new File(folder.getRoot(), "t.log.1").isFile());
        assertTrue(new File(folder.getRoot(), "t.log.2").isFile());
        assertFalse(new File(folder.getRoot(), "t.log.3").exists());
        for (File f : folder.getRoot().listFiles()) assertTrue(f.length() <= 100);
    }

    @Test
    public void exportIsOldestFirstAndEndsWithNewest() throws Exception {
        RotatingFileLog log = new RotatingFileLog(folder.getRoot(), "t.log", 30, 3);
        for (int i = 0; i < 6; i++) log.append("line-" + i + "-xxxxxxxxxxxx");
        File out = log.exportTo(new File(folder.getRoot(), "export.txt"));
        String text = new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8).trim();
        assertTrue(text.endsWith("line-5-xxxxxxxxxxxx"));
        assertTrue(text.indexOf("line-3") < text.indexOf("line-5"));
        assertEquals(-1, text.indexOf("line-0"));
    }
}
