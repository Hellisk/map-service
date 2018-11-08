package mapupdate.mapinference.trajectoryclustering.pcurves.Debug;

import java.awt.*;

final public class DebugEvent extends Event {
    public final static int STARTED_DEBUG_THREAD = 10000;
    public final static int FINISHED_DEBUG_THREAD = 10001;
    public final static int ITERATE_DEBUG_THREAD = 10002;
    public final static int RESET_DEBUG_THREAD = 10003;
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public DebugEvent(Object target, int id, Object arg) {
        super(target, id, arg);
    }
}
