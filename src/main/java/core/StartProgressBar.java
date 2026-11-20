package core;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

import core.ui.CompanywareTheme;

/** Compact branded startup feedback displayed while the plugin platform boots. */
public class StartProgressBar extends JWindow {
    private static final long serialVersionUID = 1L;
    private static final int MAX_VALUE = 100;

    private JProgressBar progressBar;

    public void initializeUI() throws IOException, URISyntaxException {
        CompanywareTheme.install();
        ResourceBundle bundle = ResourceBundle.getBundle("messages");

        JPanel card = new JPanel(new BorderLayout(0, 22));
        card.setBackground(CompanywareTheme.SURFACE);
        card.setBorder(CompanywareTheme.cardBorder(28));

        JLabel brand = new JLabel("<html>Companyware<br><span style='font-size:10px;font-weight:normal'>Desktop</span></html>",
                CompanywareTheme.logo(48), SwingConstants.LEFT);
        brand.setFont(CompanywareTheme.BODY.deriveFont(java.awt.Font.BOLD, 22f));
        brand.setIconTextGap(12);
        card.add(brand, BorderLayout.NORTH);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(bundle.getString("program.start"));
        title.setFont(CompanywareTheme.BODY.deriveFont(java.awt.Font.BOLD, 18f));
        title.setForeground(CompanywareTheme.TEXT);
        JLabel detail = new JLabel(bundle.getString("program.start.detail"));
        detail.setFont(CompanywareTheme.BODY);
        detail.setForeground(CompanywareTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(6));
        copy.add(detail);
        card.add(copy, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, MAX_VALUE);
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setForeground(CompanywareTheme.ROYAL);
        progressBar.setBackground(CompanywareTheme.SIDEBAR);
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.putClientProperty("JProgressBar.largeHeight", Boolean.FALSE);
        progressBar.setIndeterminate(true);
        card.add(progressBar, BorderLayout.SOUTH);

        setContentPane(card);
        pack();
        setSize(new Dimension(470, 230));
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        setVisible(true);
    }

    public void finish() {
        progressBar.setIndeterminate(false);
        progressBar.setValue(MAX_VALUE);
        setVisible(false);
        dispose();
    }
}
