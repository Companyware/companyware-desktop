package plugins.community.invoice.view;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Scrollable-page content rendered from the same PDF bytes used for export. */
final class PdfPagePreview extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final float PREVIEW_DPI = 110f;

    PdfPagePreview() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIManager.getColor("Panel.background"));
    }

    void setPdf(byte[] pdf) throws IOException {
        List<BufferedImage> images = new ArrayList<BufferedImage>();
        try (PDDocument document = PDDocument.load(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                images.add(renderer.renderImageWithDPI(page, PREVIEW_DPI, ImageType.RGB));
            }
        }
        removeAll();
        for (int page = 0; page < images.size(); page++) {
            JLabel label = new JLabel(new ImageIcon(images.get(page)), SwingConstants.CENTER);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setBorder(new EmptyBorder(12, 12, 12, 12));
            add(label);
            if (page + 1 < images.size()) add(Box.createVerticalStrut(12));
        }
        revalidate();
        repaint();
    }
}
