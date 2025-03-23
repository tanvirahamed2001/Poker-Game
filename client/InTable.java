public class InTable {
    private boolean in;
    private int tableID;

    public InTable(boolean in, int tableID) {
        this.in = in;
        this.tableID = tableID;
    }

    public boolean getIn() {
        return this.in;
    }

    public int getTableID() {
        return this.tableID;
    }
}
