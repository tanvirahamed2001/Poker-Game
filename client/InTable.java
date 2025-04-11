/**
 * Object used to identify the given table a player is sitting in.
 * Used in the reconnect functionality for the clients.
 */
public class InTable {
    private boolean in;
    private int tableID;

    /**
     * Constructor
     * 
     * @param in      Boolean, true of in table, false if not
     * @param tableID The table id the player is sitting in.
     */
    public InTable(boolean in, int tableID) {
        this.in = in;
        this.tableID = tableID;
    }

    /**
     * Returns the in status of the player.
     * 
     * @return True if in table. False if not in table.
     */
    public boolean getIn() {
        return this.in;
    }

    /**
     * Returns the table id the player is sitting in.
     * 
     * @return The given table id.
     */
    public int getTableID() {
        return this.tableID;
    }
}
