package shared.communication_objects;

import java.io.Serializable;

/**
 * this class is to mimic the client making a choice of creating a
 * NEW table or to JOIN a table in progress
 */
public class GameChoice implements Serializable {

    public enum Choice {
        NEW, JOIN
    }

    private Choice choice;
    private int tableId;

    /**
     * GameChoice constructor
     * 
     * @param choice GameChoice.Choice
     */
    public GameChoice(Choice choice) {
        this.choice = choice;
    }

    /**
     * Sets the Table ID
     * 
     * @param id Int Table ID
     */
    public void setId(int id) {
        this.tableId = id;
    }

    /**
     * Get the Table ID
     * 
     * @return Integer Table ID
     */
    public int getId() {
        return this.tableId;
    }

    /**
     * Get the players choice
     * 
     * @return GameChoice.Choice
     */
    public Choice getChoice() {
        return this.choice;
    }

}
