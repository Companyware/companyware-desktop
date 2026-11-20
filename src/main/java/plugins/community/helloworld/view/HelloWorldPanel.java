package plugins.community.helloworld.view;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import platform.api.ui.StandardView;
import plugins.community.helloworld.application.GreetingService;
import platform.api.theme.ThemeTokens;
import platform.api.theme.ThemeColor;

/** Right-hand application panel opened by the HelloWorld navigation contribution. */
public final class HelloWorldPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public HelloWorldPanel(GreetingService greetings, ThemeTokens theme) {
        super(new BorderLayout());
        JLabel greeting = new JLabel(greetings.greeting().value().orElse("Hello World!"), SwingConstants.CENTER);
        greeting.setForeground(theme.color(ThemeColor.SUCCESS));
        greeting.setFont(greeting.getFont().deriveFont(Font.BOLD, 30f));
        add(StandardView.create("HelloWorld", greeting), BorderLayout.CENTER);
    }

}
