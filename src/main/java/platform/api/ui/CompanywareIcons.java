package platform.api.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * Public, theme-aware Companyware icon set for navigation and common actions.
 * Plugins should prefer these semantic icons over private bitmap resources.
 */
public final class CompanywareIcons {
    public static final Icon OVERVIEW = new VectorIcon(Kind.OVERVIEW);
    public static final Icon CREATE = new VectorIcon(Kind.CREATE);
    public static final Icon EDIT = new VectorIcon(Kind.EDIT);
    public static final Icon DOCUMENT = new VectorIcon(Kind.DOCUMENT);
    public static final Icon IMAGE = new VectorIcon(Kind.IMAGE);
    public static final Icon SEARCH = new VectorIcon(Kind.SEARCH);
    public static final Icon MAIL = new VectorIcon(Kind.MAIL);
    public static final Icon USERS = new VectorIcon(Kind.USERS);
    public static final Icon USER_ADD = new VectorIcon(Kind.USER_ADD);
    public static final Icon SETTINGS = new VectorIcon(Kind.SETTINGS);
    public static final Icon UPLOAD = new VectorIcon(Kind.UPLOAD);
    public static final Icon DELETE = new VectorIcon(Kind.DELETE);

    private CompanywareIcons() { }

    private enum Kind { OVERVIEW, CREATE, EDIT, DOCUMENT, IMAGE, SEARCH, MAIL, USERS, USER_ADD, SETTINGS, UPLOAD, DELETE }

    private static final class VectorIcon implements Icon {
        private static final int SIZE = 30;
        private final Kind kind;

        private VectorIcon(Kind kind) { this.kind = kind; }
        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(kind == Kind.DELETE ? dangerColor() : accentColor());
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (kind == Kind.OVERVIEW) overview(g);
                else if (kind == Kind.CREATE) create(g);
                else if (kind == Kind.EDIT) edit(g);
                else if (kind == Kind.DOCUMENT) page(g);
                else if (kind == Kind.IMAGE) image(g);
                else if (kind == Kind.SEARCH) search(g);
                else if (kind == Kind.MAIL) mail(g);
                else if (kind == Kind.USERS) users(g);
                else if (kind == Kind.USER_ADD) userAdd(g);
                else if (kind == Kind.SETTINGS) settings(g);
                else if (kind == Kind.UPLOAD) upload(g);
                else delete(g);
            } finally {
                g.dispose();
            }
        }

        private Color accentColor() {
            Color color = UIManager.getColor("Companyware.accent");
            if (color == null) color = UIManager.getColor("Component.focusColor");
            if (color == null) color = UIManager.getColor("Button.foreground");
            return color == null ? new Color(81, 68, 181) : color;
        }

        private Color dangerColor() {
            Color color = UIManager.getColor("Actions.Red");
            return color == null ? new Color(196, 55, 55) : color;
        }

        private void page(Graphics2D g) {
            g.draw(new RoundRectangle2D.Float(6, 3, 18, 24, 2, 2));
            g.draw(new Line2D.Float(18, 3, 24, 9));
            g.draw(new Line2D.Float(18, 3, 18, 9));
            g.draw(new Line2D.Float(18, 9, 24, 9));
        }

        private void overview(Graphics2D g) {
            page(g);
            for (int row = 0; row < 3; row++) {
                float yy = 13 + row * 5;
                g.fill(new Rectangle2D.Float(9, yy - 1, 2, 2));
                g.draw(new Line2D.Float(14, yy, 21, yy));
            }
        }

        private void create(Graphics2D g) {
            page(g);
            g.draw(new RoundRectangle2D.Float(15, 17, 13, 11, 2, 2));
            g.draw(new Line2D.Float(18, 22.5f, 25, 22.5f));
            g.draw(new Line2D.Float(21.5f, 19, 21.5f, 26));
        }

        private void edit(Graphics2D g) {
            Path2D.Float pencil = new Path2D.Float();
            pencil.moveTo(8, 21);
            pencil.lineTo(12, 25);
            pencil.lineTo(26, 11);
            pencil.lineTo(22, 7);
            pencil.closePath();
            g.draw(pencil);
            g.draw(new Line2D.Float(10, 19, 14, 23));
            g.draw(new Line2D.Float(20, 9, 24, 13));

            Path2D.Float tip = new Path2D.Float();
            tip.moveTo(8, 21);
            tip.lineTo(12, 25);
            tip.lineTo(5, 28);
            tip.closePath();
            g.draw(tip);
            g.fill(new java.awt.geom.Ellipse2D.Float(5, 26, 2.5f, 2.5f));
        }

        private void image(Graphics2D g) {
            g.draw(new RoundRectangle2D.Float(3, 6, 24, 19, 3, 3));
            g.drawOval(18, 9, 4, 4);
            g.drawLine(6, 22, 12, 15);
            g.drawLine(12, 15, 17, 20);
            g.drawLine(17, 20, 20, 17);
            g.drawLine(20, 17, 25, 22);
        }

        private void search(Graphics2D g) {
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(4, 3, 17, 17);
            g.draw(new Line2D.Float(18, 18, 27, 27));
        }

        private void mail(Graphics2D g) {
            g.draw(new RoundRectangle2D.Float(3, 7, 24, 17, 3, 3));
            g.drawLine(4, 9, 15, 17);
            g.drawLine(26, 9, 15, 17);
            g.drawLine(4, 23, 12, 16);
            g.drawLine(26, 23, 18, 16);
        }

        private void person(Graphics2D g, int x, int y) {
            g.drawOval(x + 4, y, 7, 7);
            g.draw(new RoundRectangle2D.Float(x, y + 9, 15, 10, 6, 6));
        }

        private void users(Graphics2D g) {
            person(g, 2, 5);
            person(g, 13, 5);
        }

        private void userAdd(Graphics2D g) {
            person(g, 3, 5);
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(19, 21, 28, 21);
            g.draw(new Line2D.Float(23.5f, 16.5f, 23.5f, 25.5f));
        }

        private void settings(Graphics2D g) {
            g.drawOval(8, 8, 14, 14);
            g.drawOval(12, 12, 6, 6);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int x1 = 15 + (int) Math.round(Math.cos(angle) * 8);
                int y1 = 15 + (int) Math.round(Math.sin(angle) * 8);
                int x2 = 15 + (int) Math.round(Math.cos(angle) * 12);
                int y2 = 15 + (int) Math.round(Math.sin(angle) * 12);
                g.drawLine(x1, y1, x2, y2);
            }
        }

        private void upload(Graphics2D g) {
            g.draw(new RoundRectangle2D.Float(4, 18, 22, 9, 2, 2));
            g.drawLine(15, 4, 15, 21);
            g.drawLine(9, 10, 15, 4);
            g.drawLine(21, 10, 15, 4);
        }

        private void delete(Graphics2D g) {
            g.draw(new RoundRectangle2D.Float(8, 8, 14, 18, 2, 2));
            g.drawLine(6, 7, 24, 7);
            g.drawLine(11, 4, 19, 4);
            g.drawLine(12, 11, 12, 22);
            g.drawLine(18, 11, 18, 22);
        }
    }
}
