package mapupdate.mapinference.trajectoryclustering.pcurves.Debug;

import java.awt.*;

final public class Debug {
    public static TextComponent diagnosisTextComponent;

    int iterator;
    String previousString;
    String presentString;
    String newString;

    @SuppressWarnings("deprecation")
    public Debug(String string) {
        iterator = 0;
        newString = string;
        previousString = diagnosisTextComponent.getText();
        presentString = previousString + newString;
        diagnosisTextComponent.setText(presentString);
        diagnosisTextComponent.postEvent(new DebugEvent(diagnosisTextComponent, DebugEvent.STARTED_DEBUG_THREAD,
                newString));
    }

    final public void SetIterator(int in_iterator) {
        iterator = in_iterator;
    }

    final public int GetIterator() {
        return iterator;
    }

    @SuppressWarnings("deprecation")
    final public void Reset(String string) {
        iterator = 0;
        newString = string;
        presentString = previousString + newString;
        diagnosisTextComponent.setText(presentString);
        diagnosisTextComponent.postEvent(new DebugEvent(diagnosisTextComponent, DebugEvent.RESET_DEBUG_THREAD,
                newString));
    }

    @SuppressWarnings("deprecation")
    final public void Terminate() {
        diagnosisTextComponent.setText(previousString);
        diagnosisTextComponent.postEvent(new DebugEvent(diagnosisTextComponent, DebugEvent.FINISHED_DEBUG_THREAD,
                newString));
    }

    @SuppressWarnings("deprecation")
    final public void Iterate() {
        diagnosisTextComponent.setText(presentString + " " + ++iterator + " ");
        diagnosisTextComponent.postEvent(new DebugEvent(diagnosisTextComponent, DebugEvent.ITERATE_DEBUG_THREAD,
                newString));
    }
}
