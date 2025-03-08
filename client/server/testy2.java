import java.util.ArrayList;

public class testy2 implements Runnable {
	ArrayList<Object> monitor;
	public testy2(ArrayList<Object> obj) {
		monitor = obj;
	}

	@Override
	public void run() {
		System.out.println("Wake up!");
		synchronized(monitor.get(0)) {
			monitor.get(0).notify();
		}
		
	}

}
