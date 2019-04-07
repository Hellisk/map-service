package mapupdate.mapinference.trajectoryclustering.pcurves.AWT;

import java.awt.*;

final public class ChoiceLabelPanel extends Panel {
    /**
     *
     */
    private static final long serialVersionUID = -2004461919306901613L;
    private Label label;
    private Choice choice;

    public ChoiceLabelPanel(String text, int alignment, boolean horizontal) {
        label = new Label(text, alignment);
        choice = new Choice();
        if (horizontal)
            setLayout(new GridLayout(1, 0));
        else
            setLayout(new GridLayout(0, 1));
        add(label);
        add(choice);
        validate();
    }

    public ChoiceLabelPanel(Choice in_choice, String text, int alignment, boolean horizontal) {
        choice = in_choice;
        label = new Label(text, alignment);
        if (horizontal)
            setLayout(new GridLayout(1, 0));
        else
            setLayout(new GridLayout(0, 1));
        add(label);
        add(choice);
        validate();
    }

    final public void addItem(String item) {
        choice.addItem(item);
    }

    final public int countItems() {
        return choice.getItemCount();
    }

    final public String getItem(int index) {
        return choice.getItem(index);
    }

    final public int getSelectedIndex() {
        return choice.getSelectedIndex();
    }

    final public String getSelectedItem() {
        return choice.getSelectedItem();
    }

    final public void select(int pos) {
        choice.select(pos);
    }

    final public void select(String str) {
        choice.select(str);
    }

    @SuppressWarnings("deprecation")
    @Override
    final public boolean handleEvent(Event event) {
        if (event.target == choice) {
            postEvent(new Event(this, event.id, event.arg));
        }
        return super.handleEvent(event);
    }
}
