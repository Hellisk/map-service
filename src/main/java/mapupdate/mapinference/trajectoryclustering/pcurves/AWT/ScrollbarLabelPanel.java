package mapupdate.mapinference.trajectoryclustering.pcurves.AWT;

import java.awt.*;

final public class ScrollbarLabelPanel extends Panel {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private Label label;
    private Scrollbar scrollbar;

    public ScrollbarLabelPanel(String text, int alignment, int value, int minimum, int maximum, boolean horizontal) {
        label = new Label(text, alignment);
        scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, value, (maximum - minimum) / 100 + 1, minimum, maximum);
        if (horizontal)
            setLayout(new GridLayout(1, 0));
        else
            setLayout(new GridLayout(0, 1));
        add(label);
        add(scrollbar);
        validate();
    }

    @SuppressWarnings("deprecation")
    final public int getLineIncrement() {
        return scrollbar.getLineIncrement();
    }

    @SuppressWarnings("deprecation")
    final public void setLineIncrement(int l) {
        scrollbar.setLineIncrement(l);
    }

    final public int getMaximum() {
        return scrollbar.getMaximum();
    }

    final public int getMinimum() {
        return scrollbar.getMinimum();
    }

    final public int getOrientation() {
        return scrollbar.getOrientation();
    }

    @SuppressWarnings("deprecation")
    final public int getPageIncrement() {
        return scrollbar.getPageIncrement();
    }

    final public void setPageIncrement(int l) {
        scrollbar.setBlockIncrement(l);
    }

    final public int getValue() {
        return scrollbar.getValue();
    }

    final public void setValue(int value) {
        scrollbar.setValue(value);
    }

    @SuppressWarnings("deprecation")
    final public int getVisible() {
        return scrollbar.getVisible();
    }

    final public void setValues(int value, int visible, int minimum, int maximum) {
        scrollbar.setValues(value, visible, minimum, maximum);
    }

    @Override
    @SuppressWarnings("deprecation")
    final public boolean handleEvent(Event event) {
        if (event.target == scrollbar && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
            postEvent(new Event(this, event.id, event.arg));
        }
        return super.handleEvent(event);
    }
}
