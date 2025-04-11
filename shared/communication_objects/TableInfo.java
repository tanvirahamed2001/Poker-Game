package shared.communication_objects;

import java.io.Serializable;

/**
 * Utility object for passing table information across sockets.
 * Utilized with the Command object
 */
public class TableInfo implements Serializable {

    private int tableID;

    /**
     * Constructor for Table Info
     * 
     * @param id the given table id
     */
    public TableInfo(int id) {
        this.tableID = id;
    }

    /**
     * Function to get the table id
     * 
     * @return an int representing the table id
     */
    public int getTableID() {
        return tableID;
    }
}
