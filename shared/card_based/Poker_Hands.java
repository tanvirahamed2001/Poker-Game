package shared.card_based;

import java.io.Serializable;
import shared.card_based.Card.Rank;

/**
 * Utility class for helping decide on a winner based on the poker hands
 */
public class Poker_Hands implements Serializable {

	public winners result;
	public Card highcard;
	public Card secondhigh;
	public int playernumber;

	public enum winners {
		HIGH, ONEPAIR, TWOPAIR, THREEKIND, STRAIGHT, FLUSH, FULLHOUSE, FOURKIND, STRAIGHTFLUSH
	}

	/**
	 * Sets the hands for the players, has no second high card
	 * 
	 * @param hand   the winning hands
	 * @param high   the high card
	 * @param number the player number
	 */
	public Poker_Hands(winners hand, Card high, int number) {
		this.result = hand;
		this.highcard = high;
		this.secondhigh = null;
		this.playernumber = number;
	}

	/**
	 * Sets the hands for the players, has second high card
	 * 
	 * @param hand   the winning hands
	 * @param high   the high card
	 * @param second the second high card
	 * @param number the player number
	 */
	public Poker_Hands(winners hand, Card high, Card second, int number) {
		this.result = hand;
		this.highcard = high;
		this.secondhigh = second;
		this.playernumber = number;
	}

	/**
	 * Gets the result of the hand
	 * 
	 * @return the result parameter
	 */
	public winners getResult() {
		return this.result;
	}

	/**
	 * Gets the high rank
	 * 
	 * @return the high rank
	 */
	public Rank gethighrank() {
		return this.highcard.get_rank();
	}

	/**
	 * Gets the second rank
	 * 
	 * @return the second rank
	 */
	public Rank getsecondrank() {
		return this.secondhigh.get_rank();
	}
}