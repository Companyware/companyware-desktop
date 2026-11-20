package platform.api.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Recommended, optional layout for a consistent Companyware workspace view. */
public final class StandardView {
    public static final int OUTER_GAP = 16;
    public static final int TOP_GAP = 28;
    public static final int CONTENT_GAP = 12;
    public static final int HEADER_HEIGHT = 50;

    private StandardView() { }

    public static JPanel create(String title, JComponent content) {
        return create(title, content, null);
    }

    public static JPanel create(String title, JComponent content, JComponent headerAction) {
        JPanel result = new JPanel(new BorderLayout(0, CONTENT_GAP));
        result.setBorder(BorderFactory.createEmptyBorder(TOP_GAP, OUTER_GAP, OUTER_GAP, OUTER_GAP));

        JPanel header = new JPanel(new BorderLayout(CONTENT_GAP, 0));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        header.add(heading, BorderLayout.CENTER);
        if (headerAction != null) {
            Dimension actionSize = headerAction.getPreferredSize();
            header.add(Box.createRigidArea(actionSize), BorderLayout.WEST);
            header.add(headerAction, BorderLayout.EAST);
        }
        header.setPreferredSize(new Dimension(10, HEADER_HEIGHT));
        result.add(header, BorderLayout.NORTH);
        result.add(content, BorderLayout.CENTER);
        return result;
    }
}
