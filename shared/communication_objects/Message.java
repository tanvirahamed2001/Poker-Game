package shared.communication_objects;

import java.io.Serializable;

/**
 * object to hande basic messages instead of using BufferedReaders and Writers
 */
public class Message implements Serializable {

    private String text;

    /**
     * Constructs the message
     * 
     * @param text string representation of the message
     */
    public Message(String text) {
        this.text = text;
    }

    /**
     * Gets the embedded message
     * 
     * @return the string message
     */
    public String getMsg() {
        return this.text;
    }

}
