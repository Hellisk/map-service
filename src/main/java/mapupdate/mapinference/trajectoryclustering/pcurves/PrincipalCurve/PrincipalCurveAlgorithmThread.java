package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve;

final class PrincipalCurveAlgorithmThread extends PrincipalCurveAlgorithm implements Runnable {
    public final static int ACTION_INIT = 0;
    public final static int ACTION_INNER_STEP = 1;
    public final static int ACTION_OUTER_STEP = 2;
    public final static int ACTION_ADD_VERTEX_AS_ONE_MIDPOINT = 3;
    public final static int ACTION_START = 4;
    public final static int ACTION_CONTINUE = 5;
    private final static int ACTION_TERMINATE = 6;
    private Thread algorithmThread = null;
    // Actions
    private int action;

    public PrincipalCurveAlgorithmThread(PrincipalCurveClass initialCurve,
                                         PrincipalCurveParameters principalCurveParameters) {
        super(initialCurve, principalCurveParameters);
        start();
    }

    final void start() {
        if (algorithmThread == null) {
            algorithmThread = new Thread(this, "PrincipalCurveAlgorithm");
            algorithmThread.start();
        }
    }

    final public void stop() {
        algorithmThread = null;
    }

    @Override
    final public void run() {
        boolean cont = true;
        algorithmThread.setPriority(Thread.MIN_PRIORITY);
        Thread myThread = Thread.currentThread();
        try {
            while (algorithmThread == myThread && cont) {
                ActionLoopIsWaiting();
                stop = false;
                switch (action) {
                    case ACTION_INIT:
                        Initialize(0);
                        break;
                    case ACTION_INNER_STEP:
                        InnerStep();
                        break;
                    case ACTION_OUTER_STEP:
                        OuterStep();
                        break;
                    case ACTION_START:
                        start(0);
                        break;
                    case ACTION_CONTINUE:
                        Continue();
                        break;
                    case ACTION_ADD_VERTEX_AS_ONE_MIDPOINT:
                        AddOneVertexAsMidpoint(true);
                        break;
                    case ACTION_TERMINATE:
                        cont = false;
                        break;
                }
            }
        } catch (InterruptedException e) {
        }
    }

    final synchronized void ActionLoopIsWaiting() {
        try {
            wait();
        } catch (InterruptedException e) {
        }
    }

    final public synchronized void SetAction(int in_action) {
        action = in_action;
        notifyAll();
    }

    final public synchronized void ReleaseStep() {
        notifyAll();
    }

    final public void Stop() {
        stop = true;
    }
}
