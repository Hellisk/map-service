package mapupdate.mapinference.trajectoryclustering.pcurves.Internet;

public final class UpLoadFile extends UpLoadFileOffline implements Runnable {
    public final static int ACTION_OPEN = 0;
    // Actions
    private final static int NO_ACTION = -1;
    private final static int ACTION_APPEND = 1;
    private final static int ACTION_TERMINATE = 2;
    private Thread upLoadFileThread = null;
    private int action = NO_ACTION;

    // filename is relative to $root/CgiOutput (see openFile.cgi and appendFile.cgi). If filename is needed to
    // be accessessed from the net, it should be pre-created manually to set the rights.
    public UpLoadFile(String in_filename, long in_uniquenumber) {
        super(in_filename, in_uniquenumber);
        start();
    }

    // Random uniqueNumber
    public UpLoadFile(String in_filename) {
        super(in_filename);
        start();
    }

    void start() {
        if (upLoadFileThread == null) {
            upLoadFileThread = new Thread(this, "UpLoadFile");
            upLoadFileThread.start();
        }
    }

    public void stop() {
        upLoadFileThread = null;
    }

    @Override
    public void run() {
        boolean cont = true;
        upLoadFileThread.setPriority(Thread.MIN_PRIORITY);
        Thread myThread = Thread.currentThread();
        while (upLoadFileThread == myThread && cont) {
            ActionLoopIsWaiting();
            switch (action) {
                case ACTION_OPEN:
                    Open();
                    action = NO_ACTION;
                    break;
                case ACTION_APPEND:
                    Append();
                    action = NO_ACTION;
                    break;
                case ACTION_TERMINATE:
                    cont = false;
                    break;
            }
        }
    }

    synchronized void ActionLoopIsWaiting() {
        notifyAll();
        try {
            // to avoid deadlock, action loop repeats once a second
            wait(1000);
        } catch (InterruptedException e) {
        }
    }

    public synchronized void SetAction(int in_action) {
        try {
            wait();
        } catch (InterruptedException e) {
        }
        action = in_action;
        notifyAll();
    }

    public synchronized void AppendString(String in_stringToAppend) {
        try {
            wait();
        } catch (InterruptedException e) {
        }
        stringToAppend = in_stringToAppend;
        action = ACTION_APPEND;
        notifyAll();
    }
}
