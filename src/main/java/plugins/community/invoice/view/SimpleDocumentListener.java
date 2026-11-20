package plugins.community.invoice.view;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class SimpleDocumentListener implements DocumentListener {
    private final Runnable change;
    SimpleDocumentListener(Runnable change) { this.change = change; }
    @Override public void insertUpdate(DocumentEvent event) { change.run(); }
    @Override public void removeUpdate(DocumentEvent event) { change.run(); }
    @Override public void changedUpdate(DocumentEvent event) { change.run(); }
}

