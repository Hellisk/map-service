package algorithm.mapinference.lineclustering.pcurves.PostScript;

import algorithm.mapinference.lineclustering.pcurves.AWT.ScrollbarTextFieldPanel;

import java.awt.*;

final class PostScriptSetupDialog extends Dialog {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final static int MIN_X = 0;
	private final static int MIN_Y = 0;
	private final static int MAX_X = 612; // letter size
	private final static int MAX_Y = 792; // letter size
	private final static int MAX_NUM_OF_PAGES = 100;
	
	private ScrollbarTextFieldPanel boundingBoxX1Panel;
	private ScrollbarTextFieldPanel boundingBoxX2Panel;
	private ScrollbarTextFieldPanel boundingBoxY1Panel;
	private ScrollbarTextFieldPanel boundingBoxY2Panel;
	private ScrollbarTextFieldPanel numOfPagesPanel;
	private Panel panel;
	private Button doneButton;
	
	public PostScriptSetupDialog(Frame frame) {
		super(frame, "PostScript Setup", true);
		SetupPanel();
		setLayout(new BorderLayout());
		add("Center", panel);
		doneButton = new Button("Done");
		add("South", doneButton);
		validate();
		pack();
	}
	
	public PostScriptSetupDialog(Frame frame, Panel additionalPanel) {
		super(frame, "PostScript Setup", true);
		SetupPanel();
		setLayout(new BorderLayout());
		add("Center", panel);
		add("East", additionalPanel);
		doneButton = new Button("Done");
		add("South", doneButton);
		validate();
		pack();
	}
	
	private void SetupPanel() {
		panel = new Panel();
		boundingBoxX1Panel =
				new ScrollbarTextFieldPanel("Left: ", (int) PostScriptDocument.boundingBoxX1, MIN_X, MAX_X - 1, true, 1);
		boundingBoxX2Panel =
				new ScrollbarTextFieldPanel("Right: ", (int) PostScriptDocument.boundingBoxX2, MIN_X + 1, MAX_X, true,
						1);
		boundingBoxY1Panel =
				new ScrollbarTextFieldPanel("Bottom: ", (int) PostScriptDocument.boundingBoxY1, MIN_Y, MAX_Y - 1, true,
						1);
		boundingBoxY2Panel =
				new ScrollbarTextFieldPanel("Top: ", (int) PostScriptDocument.boundingBoxY2, MIN_Y + 1, MAX_Y, true, 1);
		numOfPagesPanel =
				new ScrollbarTextFieldPanel("Number of pages: ", PostScriptDocument.numOfPages, 1, MAX_NUM_OF_PAGES,
						true, 1);
		panel.setLayout(new GridLayout(0, 1));
		panel.add(boundingBoxX1Panel);
		panel.add(boundingBoxX2Panel);
		panel.add(boundingBoxY1Panel);
		panel.add(boundingBoxY2Panel);
		panel.add(numOfPagesPanel);
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
		} else if (event.target == boundingBoxX1Panel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			PostScriptDocument.boundingBoxX1 = (Integer) event.arg;
			if (PostScriptDocument.boundingBoxX2 < PostScriptDocument.boundingBoxX1 + 1) {
				boundingBoxX2Panel.setValue((int) PostScriptDocument.boundingBoxX1 + 1);
				PostScriptDocument.boundingBoxX2 = PostScriptDocument.boundingBoxX1 + 1;
			}
		} else if (event.target == boundingBoxX2Panel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			PostScriptDocument.boundingBoxX2 = (Integer) event.arg;
			if (PostScriptDocument.boundingBoxX2 - 1 < PostScriptDocument.boundingBoxX1) {
				boundingBoxX1Panel.setValue((int) PostScriptDocument.boundingBoxX2 - 1);
				PostScriptDocument.boundingBoxX1 = PostScriptDocument.boundingBoxX2 - 1;
			}
		} else if (event.target == boundingBoxY1Panel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			PostScriptDocument.boundingBoxY1 = (Integer) event.arg;
			if (PostScriptDocument.boundingBoxY2 < PostScriptDocument.boundingBoxY1 + 1) {
				boundingBoxY2Panel.setValue((int) PostScriptDocument.boundingBoxY1 + 1);
				PostScriptDocument.boundingBoxY2 = PostScriptDocument.boundingBoxY1 + 1;
			}
		} else if (event.target == boundingBoxY2Panel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			PostScriptDocument.boundingBoxY2 = (Integer) event.arg;
			if (PostScriptDocument.boundingBoxY2 - 1 < PostScriptDocument.boundingBoxY1) {
				boundingBoxY1Panel.setValue((int) PostScriptDocument.boundingBoxY2 - 1);
				PostScriptDocument.boundingBoxY1 = PostScriptDocument.boundingBoxY2 - 1;
			}
		} else if (event.target == numOfPagesPanel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			PostScriptDocument.numOfPages = (Integer) event.arg;
		}
		return super.handleEvent(event);
	}
}
