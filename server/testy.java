//test class used for testing things outside of a thread

import java.util.ArrayList;
import java.util.HashSet;

public class testy implements Runnable {
	public static void main(String[] args){
		testy thing = new testy();
		Thread thread = new Thread(thing);
		thread.start();
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