package platform.api.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.UIManager;
import org.junit.Test;

public class CompanywareIconsTest {
    @Test public void publicIconsAreDistinctThemeAwareVectors() {
        UIManager.put("Companyware.accent", new Color(81, 68, 181));
        Icon[] icons = { CompanywareIcons.OVERVIEW, CompanywareIcons.CREATE,
                CompanywareIcons.EDIT, CompanywareIcons.DOCUMENT, CompanywareIcons.IMAGE,
                CompanywareIcons.SEARCH, CompanywareIcons.MAIL, CompanywareIcons.USERS,
                CompanywareIcons.USER_ADD, CompanywareIcons.SETTINGS, CompanywareIcons.UPLOAD,
                CompanywareIcons.DELETE };
        Set<Integer> rendered = new HashSet<Integer>();
        for (Icon icon : icons) {
            assertEquals(30, icon.getIconWidth());
            assertEquals(30, icon.getIconHeight());
            int hash = renderHash(icon);
            assertTrue("Icon must draw visible pixels", hash != 1);
            rendered.add(hash);
        }
        assertEquals("Each semantic action needs its own symbol", icons.length, rendered.size());
    }

    @Test public void iconsResolveThemeColorWhenTheyArePainted() {
        UIManager.put("Companyware.accent", Color.BLUE);
        int blue = renderHash(CompanywareIcons.SEARCH);
        UIManager.put("Companyware.accent", Color.GREEN);
        int green = renderHash(CompanywareIcons.SEARCH);
        assertTrue("Theme changes must affect an existing icon", blue != green);
    }

    private int renderHash(Icon icon) {
        BufferedImage image = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(new JLabel(), graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        int hash = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) hash = 31 * hash + image.getRGB(x, y);
        }
        return hash;
    }
}
