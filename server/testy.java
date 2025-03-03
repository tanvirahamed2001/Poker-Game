//test class used for testing things outside of a thread

import java.util.HashSet;

public class testy  {
	public static void main(String[] args){
		HashSet<Integer> testy = new HashSet<Integer>();
		testy.add(1);
		testy.add(2);
		Integer[] lol = testy.toArray(new Integer[]{});
		System.out.println(lol[0]);
		System.out.println(lol[1]);
	}
}