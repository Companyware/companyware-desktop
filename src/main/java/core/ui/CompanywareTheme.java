package core.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.formdev.flatlaf.FlatLightLaf;

/** Shared Companyware design tokens and Swing component helpers. */
public final class CompanywareTheme {
    public static final Color CANVAS = new Color(247, 247, 251);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SIDEBAR = new Color(244, 242, 250);
    public static final Color NAVIGATION_SELECTED = new Color(226, 221, 246);
    public static final Color TEXT = new Color(27, 31, 52);
    public static final Color MUTED = new Color(105, 108, 133);
    public static final Color BORDER = new Color(226, 224, 235);
    public static final Color BLUE = new Color(62, 158, 215);
    public static final Color ROYAL = new Color(75, 145, 212);
    public static final Color VIOLET = new Color(105, 83, 184);
    public static final Color SUCCESS = new Color(35, 168, 107);

    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 15);
    public static final Font LABEL = BODY.deriveFont(Font.BOLD, 13f);
    public static final Font TITLE = BODY.deriveFont(Font.BOLD, 30f);

    private CompanywareTheme() { }

    public static Color canvas() { return color("Companyware.canvas", CANVAS); }
    public static Color surface() { return color("Companyware.surface", SURFACE); }
    public static Color sidebar() { return color("Companyware.sidebar", SIDEBAR); }
    public static Color text() { return color("Companyware.text", TEXT); }
    public static Color muted() { return color("Companyware.muted", MUTED); }
    public static Color border() { return color("Companyware.border", BORDER); }
    public static Color primary() { return color("Companyware.primary", ROYAL); }
    public static Color accent() { return color("Companyware.accent", VIOLET); }
    public static Color navigationSelected() { return blend(sidebar(), accent(), .22f); }

    private static Color color(String key, Color fallback) {
        Color value = UIManager.getColor(key);
        return value == null ? fallback : value;
    }

    private static Color blend(Color base, Color accent, float amount) {
        float keep = 1f - amount;
        return new Color(Math.round(base.getRed() * keep + accent.getRed() * amount),
                Math.round(base.getGreen() * keep + accent.getGreen() * amount),
                Math.round(base.getBlue() * keep + accent.getBlue() * amount));
    }

    public static void install() {
        FlatLightLaf.install();
        UIManager.put("defaultFont", BODY);
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.arc", 12);
        UIManager.put("PasswordField.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.focusColor", ROYAL);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.focusWidth", 2);
    }

    public static Border cardBorder(int padding) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border()),
            BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    public static void primaryButton(AbstractButton button) {
        button.setBackground(explicit(primary()));
        button.setForeground(TEXT);
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setFocusPainted(false);
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    public static void secondaryButton(AbstractButton button) {
        button.setBackground(explicit(surface()));
        button.setForeground(explicit(text()));
        button.setFocusPainted(false);
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    public static void navigationButton(AbstractButton button) {
        button.setBackground(explicit(surface()));
        button.setForeground(explicit(text()));
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setHorizontalAlignment(AbstractButton.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        button.setFocusPainted(false);
    }

    public static Border selectedNavigationBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent()),
            BorderFactory.createEmptyBorder(8, 2, 8, 6));
    }

    private static Color explicit(Color color) {
        return new Color(color.getRGB(), true);
    }

    public static void transparent(JComponent component) {
        component.setOpaque(false);
    }

    public static Icon logo(final int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float stroke = Math.max(5f, size * .2f);
                g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g.setColor(accent());
                g.draw(new Arc2D.Float(x + stroke / 2, y + stroke / 2, size - stroke, size - stroke,
                    42, 276, Arc2D.OPEN));
                float barHeight = Math.max(6f, size * .16f);
                float barX = x + size * .53f;
                float barY = y + (size - barHeight) / 2f;
                g.setColor(primary());
                g.fill(new RoundRectangle2D.Float(barX, barY, size * .42f, barHeight,
                    barHeight, barHeight));
                g.dispose();
            }
        };
    }

    /** Theme-aware profile symbol that stays sharp at different display scales. */
    public static Icon profileIcon(final int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }

            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(accent());
                float head = size * .36f;
                g.fill(new Ellipse2D.Float(x + (size - head) / 2f, y + size * .08f, head, head));
                g.setColor(primary());
                g.fill(new RoundRectangle2D.Float(x + size * .12f, y + size * .52f,
                        size * .76f, size * .40f, size * .24f, size * .24f));
                g.dispose();
            }
        };
    }

    /** Theme-aware eye symbol for password visibility controls. */
    public static Icon eyeIcon(final int width, final int height) {
        return new Icon() {
            @Override public int getIconWidth() { return width; }
            @Override public int getIconHeight() { return height; }

            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(muted());
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Float(x + 1, y + 3, width - 2, height - 6, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Float(x + 1, y + 3, width - 2, height - 6, 180, 180, Arc2D.OPEN));
                float pupil = Math.min(width, height) * .28f;
                g.fill(new Ellipse2D.Float(x + (width - pupil) / 2f, y + (height - pupil) / 2f, pupil, pupil));
                g.dispose();
            }
        };
    }

    /** Eye with a diagonal line, used while a password is visible. */
    public static Icon eyeOffIcon(final int width, final int height) {
        return new Icon() {
            private final Icon eye = eyeIcon(width, height);
            @Override public int getIconWidth() { return width; }
            @Override public int getIconHeight() { return height; }
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                eye.paintIcon(component, graphics, x, y);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(accent());
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x + 1, y + 1, x + width - 1, y + height - 1);
                g.dispose();
            }
        };
    }
}
