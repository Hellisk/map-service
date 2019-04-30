package algorithm.mapinference.lineclustering.pcurves.Utilities;

import java.applet.Applet;
import java.awt.*;

public class AppletButton extends Applet implements Runnable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private int frameNumber = 1;
	private String windowClass;
	private String buttonText;
	private String windowTitle;
	private int requestedWidth = 0;
	private int requestedHeight = 0;
	private Button button;
	private Thread windowThread;
	private Label label;
	private boolean pleaseCreate = false;
	
	@SuppressWarnings("deprecation")
	public static void main(String args[]) {
		AppletButton window = new AppletButton();
		window.show();
	}
	
	@Override
	final public void init() {
		Environment.inApplet = true;
		Environment.cRoutines = false;
		windowClass = getParameter("WINDOWCLASS");
		if (windowClass == null) {
			windowClass = "TestWindow";
		}
		
		buttonText = getParameter("BUTTONTEXT");
		if (buttonText == null) {
			buttonText = "Click here to bring up a " + windowClass;
		}
		
		windowTitle = getParameter("WINDOWTITLE");
		if (windowTitle == null) {
			windowTitle = windowClass;
		}
		
		setBackground(new Color(153, 204, 187));
		setFont(new Font("Helvetica", Font.PLAIN, 20));
		setForeground(new Color(136, 0, 0));
		
		String windowWidthString = getParameter("WINDOWWIDTH");
		if (windowWidthString != null) {
			try {
				requestedWidth = Integer.parseInt(windowWidthString);
			} catch (NumberFormatException e) {
				// Use default width.
			}
		}
		
		String windowHeightString = getParameter("WINDOWHEIGHT");
		if (windowHeightString != null) {
			try {
				requestedHeight = Integer.parseInt(windowHeightString);
			} catch (NumberFormatException e) {
				// Use default height.
			}
		}
		
		setLayout(new GridLayout(2, 0));
		add(button = new Button(buttonText));
		button.setFont(new Font("Helvetica", Font.PLAIN, 20));
		button.setBackground(new Color(250, 184, 120));
		button.setForeground(new Color(136, 0, 0));
		add(label = new Label("", Label.CENTER));
	}
	
	@Override
	public void start() {
		if (windowThread == null) {
			windowThread = new Thread(this, "Bringing Up " + windowClass);
			windowThread.start();
		}
	}
	
	@SuppressWarnings({"deprecation", "null"})
	@Override
	public synchronized void run() {
		Class<?> windowClassObject = null;
		Class<?> tmp = null;
		String name = null;
		
		// Make sure the window class exists and is really a Frame.
		// This has the added benefit of pre-loading the class,
		// which makes it much quicker for the first window to come up.
		try {
			windowClassObject = Class.forName(windowClass);
		} catch (Exception e) {
			// The specified class isn't anywhere that we can find.
			// label.setText(e.getMessage());
			ShowError(e);
			button.disable();
			return;
		}
		
		// Find out whether the class is a Frame.
		for (tmp = windowClassObject, name = tmp.getName(); !(name.equals("java.lang.Object") || name
				.equals("java.awt.Frame")); ) {
			tmp = tmp.getSuperclass();
			name = tmp.getName();
		}
		if ((name == null) || name.equals("java.lang.Object")) {
			// We can't run; ERROR; print status, never bring up window
			label.setText("Can't create window: " + windowClass + " isn't a Frame subclass.");
			button.disable();
			return;
		} else if (name.equals("java.awt.Frame")) {
			// Everything's OK. Wait until we're asked to create a window.
			while (windowThread != null) {
				while (!pleaseCreate) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
				
				// We've been asked to bring up a window.
				pleaseCreate = false;
				Frame window = null;
				try {
					window = (Frame) windowClassObject.newInstance();
				} catch (Exception e) {
					// label.setText(e.getMessage());
					ShowError(e);
					button.disable();
					return;
				}
				if (frameNumber == 1) {
					window.setTitle(windowTitle);
				} else {
					window.setTitle(windowTitle + ": " + frameNumber);
				}
				frameNumber++;
				
				// Set the window's size.
				window.pack();
				if ((requestedWidth > 0) | (requestedHeight > 0)) {
					window.resize(Math.max(requestedWidth, window.size().width), Math.max(requestedHeight, window
							.size().height));
				}
				
				window.show();
				label.setText("");
			}
		}
	}
	
	@Override
	public synchronized boolean action(Event event, Object what) {
		if (event.target instanceof Button) {
			// signal the window thread to build a window
			label.setText("Please wait while the window comes up...");
			pleaseCreate = true;
			notify();
		}
		return true;
	}
	
	@Override
	public void stop() {
		windowThread = null;
	}
	
	public void ShowError(Object s) {
	}
	
}

class TestWindow extends Frame {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("deprecation")
	public TestWindow() {
		resize(300, 300);
	}
}

class ErrorWindow extends Frame {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	Button okButton;
	
	@SuppressWarnings("deprecation")
	public ErrorWindow(Object message) {
		super("Error");
		setLayout(new BorderLayout());
		Label label = new Label(message.toString());
		add("Center", label);
		okButton = new Button("OK");
		add("South", okButton);
		validate();
		show();
		pack();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean action(Event event, Object arg) {
		// Done
		if (event.target == okButton) {
			postEvent(new Event(this, Event.WINDOW_DESTROY, ""));
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean handleEvent(Event event) {
		// Quit
		if (event.id == Event.WINDOW_DESTROY) {
			dispose();
		}
		return super.handleEvent(event);
	}
	
}
