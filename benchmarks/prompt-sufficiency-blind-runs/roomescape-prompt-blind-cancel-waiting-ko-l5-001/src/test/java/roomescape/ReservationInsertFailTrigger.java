package roomescape;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

public class ReservationInsertFailTrigger implements Trigger {

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) {
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        throw new SQLException("forced reservation promotion failure");
    }

    @Override
    public void close() {
    }

    @Override
    public void remove() {
    }
}
