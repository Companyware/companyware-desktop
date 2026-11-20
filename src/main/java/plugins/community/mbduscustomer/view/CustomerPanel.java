package plugins.community.mbduscustomer.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import platform.api.application.ApplicationResult;
import plugins.community.mbduscustomer.application.CustomerDto;
import plugins.community.mbduscustomer.application.CustomerService;

/** CRUD presentation backed only by the customer application port. */
public final class CustomerPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final CustomerService customers;
    private final PluginMessages messages;
    private final JTable table = new JTable();
    private final JTextField[] fields = new JTextField[9];
    private Long selectedId;

    public CustomerPanel(CustomerService customers, PluginMessages messages) {
        super(new BorderLayout());
        this.customers = customers;
        this.messages = messages;
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(event -> selectRow());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), form());
        split.setResizeWeight(.65);
        add(StandardView.create(messages.text("customeroverview"), split), BorderLayout.CENTER);
        reload();
    }

    private JPanel form() {
        JPanel result = new JPanel(new BorderLayout(8, 12));
        result.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 8));
        JPanel inputs = new JPanel(new GridLayout(0, 2, 8, 8));
        String[] keys = { "salutation", "name", "firstname", "email", "street",
                "streetnumber", "zipcode", "city", "country" };
        for (int index = 0; index < keys.length; index++) {
            inputs.add(new JLabel(messages.text("customer." + keys[index])));
            fields[index] = new JTextField(18);
            inputs.add(fields[index]);
        }
        result.add(inputs, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clear = button("customer.new", event -> clear());
        JButton save = button("customer.save", event -> save());
        JButton delete = button("customer.delete", event -> delete());
        actions.add(clear); actions.add(save); actions.add(delete);
        result.add(actions, BorderLayout.SOUTH);
        return result;
    }

    private JButton button(String key, java.awt.event.ActionListener action) {
        JButton button = new JButton(messages.text(key));
        button.addActionListener(action);
        return button;
    }

    private void reload() {
        String[] columns = { "ID", messages.text("customer.salutation"), messages.text("customer.name"),
                messages.text("customer.firstname"), messages.text("customer.email"),
                messages.text("customer.street"), messages.text("customer.streetnumber"),
                messages.text("customer.zipcode"), messages.text("customer.city"), messages.text("customer.country") };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        ApplicationResult<List<CustomerDto>> result = customers.list();
        if (result.isSuccess()) {
            for (CustomerDto customer : result.valueOrThrow()) model.addRow(customer.toTableRow());
        } else showError();
        table.setModel(model);
        table.removeColumn(table.getColumnModel().getColumn(0));
    }

    private void selectRow() {
        int selected = table.getSelectedRow();
        if (selected < 0) return;
        int modelRow = table.convertRowIndexToModel(selected);
        selectedId = ((Number) table.getModel().getValueAt(modelRow, 0)).longValue();
        for (int index = 0; index < fields.length; index++) {
            Object value = table.getModel().getValueAt(modelRow, index + 1);
            fields[index].setText(value == null ? "" : value.toString());
        }
    }

    private void save() {
        String[] values = new String[fields.length];
        for (int index = 0; index < fields.length; index++) values[index] = fields[index].getText().trim();
        if (customers.save(new CustomerDto(selectedId, values)).isSuccess()) {
            clear(); reload();
        } else showError();
    }

    private void delete() {
        if (selectedId == null) return;
        if (customers.delete(selectedId).isSuccess()) {
            clear(); reload();
        } else showError();
    }

    private void clear() {
        selectedId = null;
        table.clearSelection();
        for (JTextField field : fields) field.setText("");
        fields[0].requestFocusInWindow();
    }

    private void showError() {
        JOptionPane.showMessageDialog(this, messages.text("customer.error"),
                messages.text("customer"), JOptionPane.ERROR_MESSAGE);
    }
}
