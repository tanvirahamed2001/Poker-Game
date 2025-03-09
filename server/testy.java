//test class used for testing things outside of a thread

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import games.card_based.Card;
import games.card_based.Card.Rank;
import games.card_based.Card.Suit;

public class testy implements Runnable {
	public static void main(String[] args){
		ArrayList<Card> hand = new ArrayList<>();
		hand.add(new Card(Suit.HEARTS, Rank.TWO));
		hand.add(new Card(Suit.HEARTS, Rank.THREE));
		hand.sort(Comparator.comparing(Card::get_rank));
		for(int i = 0; i < hand.size(); i++) {
			System.out.println(hand.get(i).toString());
		}
	}

	@Override
	public void run() {
		try {
			ArrayList<Object> monitor = new ArrayList<Object>();
			monitor.add(new Object());
			Thread thr = new Thread(new testy2(monitor));
			thr.start();
			synchronized(monitor.get(0)) {
				monitor.get(0).wait();
				System.out.println("I continued!");
			}
		} catch (InterruptedException e) {
			System.out.println("I was interrupted!");
		}
	}
}