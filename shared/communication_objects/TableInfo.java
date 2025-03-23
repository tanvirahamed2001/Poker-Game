package shared.communication_objects;

import java.io.Serializable;

public class TableInfo implements Serializable {

    private int tableID;

    public TableInfo(int id) {
        this.tableID = id;
    }

    public int getTableID() {
        return tableID;
    }
}
