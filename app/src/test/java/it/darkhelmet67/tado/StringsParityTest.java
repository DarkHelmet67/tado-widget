package it.darkhelmet67.tado;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

/** Every translatable string must exist in English (values/) and Italian (values-it/). */
public class StringsParityTest {

    private static Set<String> translatableNames(String path) throws Exception {
        File file = new File("src/main/res/" + path + "/strings.xml");
        assertTrue(file + " not found", file.isFile());
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement();
        Set<String> names = new TreeSet<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) continue;
            Element e = (Element) children.item(i);
            if ("false".equals(e.getAttribute("translatable"))) continue;
            names.add(e.getTagName() + ":" + e.getAttribute("name"));
        }
        return names;
    }

    @Test
    public void italianHasEveryEnglishString() throws Exception {
        Set<String> missing = translatableNames("values");
        missing.removeAll(translatableNames("values-it"));
        assertEquals("Missing in values-it: " + missing, 0, missing.size());
    }

    @Test
    public void italianHasNoStaleStrings() throws Exception {
        Set<String> stale = translatableNames("values-it");
        stale.removeAll(translatableNames("values"));
        assertEquals("Only in values-it: " + stale, 0, stale.size());
    }
}
