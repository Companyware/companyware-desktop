package plugins.community.companywaredemo.view;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.companywaredemo.application.DemoOverviewService;
import plugins.community.companywaredemo.application.DemoRow;

/** Swing presentation depending only on the overview application port and i18n. */
public final class DemoPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public DemoPanel(DemoOverviewService overview, PluginMessages messages) {
        super(new BorderLayout());
        add(StandardView.create(messages.text("demo.overview"),
                new JScrollPane(table(overview, messages))), BorderLayout.CENTER);
    }

    private JTable table(DemoOverviewService overview, PluginMessages messages) {
        String[] columns = { messages.text("demo.salutation"), messages.text("demo.name"),
                messages.text("demo.firstname"), messages.text("demo.street"),
                messages.text("demo.streetnumber"), messages.text("demo.zipcode"),
                messages.text("demo.city"), messages.text("demo.country") };
        try {
            List<DemoRow> rows = overview.listRows().valueOrThrow();
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                private static final long serialVersionUID = 1L;
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (DemoRow row : rows) model.addRow(row.toTableRow());
            return new JTable(model);
        } catch (Exception failure) {
            DefaultTableModel model = new DefaultTableModel(columns, 0);
            model.addRow(new Object[] { messages.text("demo.loaderror") });
            return new JTable(model);
        }
    }
}
