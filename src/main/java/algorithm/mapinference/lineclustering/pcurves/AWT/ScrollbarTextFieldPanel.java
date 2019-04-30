package algorithm.mapinference.lineclustering.pcurves.AWT;

import java.awt.*;

final public class ScrollbarTextFieldPanel extends Panel {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private TextField textField;
	private Scrollbar scrollbar;
	private String text;
	private int factor;
	
	public ScrollbarTextFieldPanel(String in_text, int value, int minimum, int maximum, boolean horizontal,
								   int in_factor) {
		factor = in_factor;
		text = in_text;
		textField = new TextField(text + (double) value / factor);
		textField.setEditable(false);
		scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, value, (maximum - minimum) / 100 + 1, minimum, maximum);
		if (horizontal)
			setLayout(new GridLayout(1, 0));
		else
			setLayout(new GridLayout(0, 1));
		add(textField);
		add(scrollbar);
		validate();
	}
	
	static public boolean ScrollbarEvent(Event event) {
		switch (event.id) {
			case Event.SCROLL_LINE_UP:
			case Event.SCROLL_LINE_DOWN:
			case Event.SCROLL_PAGE_UP:
			case Event.SCROLL_PAGE_DOWN:
			case Event.SCROLL_ABSOLUTE:
				return true;
		}
		return false;
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
		textField.setText(text + (double) value / factor);
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
		if (event.target == scrollbar && ScrollbarEvent(event)) {
			textField.setText(text + ((Integer) event.arg).doubleValue() / factor);
			postEvent(new Event(this, event.id, event.arg));
		}
		return super.handleEvent(event);
	}
}
