package shared.card_based;

import shared.card_based.Card.Rank;

public class Poker_Hands{
	public winners result;
	public Card highcard;
	public Card secondhigh;
	public int playernumber;
	public enum winners{
		HIGH, ONEPAIR, TWOPAIR, THREEKIND, STRAIGHT, FLUSH, FULLHOUSE,FOURKIND, STRAIGHTFLUSH
	}
	public Poker_Hands(winners hand, Card high, int number) {
		this.result = hand;
		this.highcard = high;
		this.secondhigh = null;
		this.playernumber = number;
	}
	public Poker_Hands(winners hand, Card high, Card second, int number) {
		this.result = hand;
		this.highcard = high;
		this.secondhigh = second;
		this.playernumber = number;
	}
	/**
     * here because I don't completely understand how comparators work and this lets me compare poker hands by winners
     * @return the result parameter
     */
	public winners getResult() {
		return this.result;
	}
	//same as above basically
	public Rank gethighrank() {
		return this.highcard.get_rank();
	}
	public Rank getsecondrank() {
		return this.secondhigh.get_rank();
	}
}