package algorithm.mapinference.trajectoryclustering.pcurves.Internet;

import java.awt.*;

final public class DownLoadDialog extends Dialog {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private TextField urlTextField;
    private Button doneButton;

    public DownLoadDialog(Frame frame, String title, String initialURL) {
        super(frame, title, true);
        setLayout(new BorderLayout());
        urlTextField = new TextField(initialURL, initialURL.length() + 30);
        add("North", urlTextField);
        doneButton = new Button("Done");
        add("South", doneButton);
        validate();
        pack();
    }

    final public String getURL() {
        return urlTextField.getText();
    }

    @Override
    @SuppressWarnings("deprecation")
    final public boolean action(Event event, Object arg) {
        // Done
        if (event.target == doneButton) {
            postEvent(new Event(this, Event.WINDOW_DESTROY, ""));
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    final public boolean handleEvent(Event event) {
        // Done
        if (event.id == Event.WINDOW_DESTROY) {
            dispose();
        }
        return super.handleEvent(event);
    }
}
