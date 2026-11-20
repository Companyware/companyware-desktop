package plugins.core.plugins.model;

import static org.junit.Assert.assertFalse;

import java.util.Collections;
import org.junit.Test;

public class ObjectTableModelTest {
    @Test
    public void transientInvalidEditorRowIsNotEditable() {
        ObjectTableModel<String> model = new ObjectTableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override public boolean isColumnEditable(int columnIndex) { return true; }
            @Override public Object getValueAt(String value, int columnIndex) { return value; }
            @Override public boolean setObjectFieldValue(String value, int column, Object fieldValue) { return true; }
            @Override public String getColumnName(int column) { return "value"; }
            @Override public String getFieldName(int column) { return "value"; }
            @Override public int getColumnCount() { return 1; }
        };
        model.setObjectRows(Collections.singletonList("plugin"));

        assertFalse(model.isCellEditable(-1, 0));
        assertFalse(model.isCellEditable(1, 0));
    }
}
