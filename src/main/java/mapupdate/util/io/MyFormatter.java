package mapupdate.util.io;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MyFormatter extends Formatter {

    /* (non-Javadoc)
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(LogRecord record) {
        record.setSourceClassName(MyFormatter.class.getName());
        return String.format(
                "%1$s:%2$s\n",
                record.getLevel().getName(), formatMessage(record));

    }
}