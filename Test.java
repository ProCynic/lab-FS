import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ADisk adisk = new ADisk(true);
		TransID tid2 = adisk.beginTransaction();
		TransID tid1 = adisk.beginTransaction();
		byte[] wbuffer = ADisk.blankSector();
		byte[] rbuffer = ADisk.blankSector();
		
		int sectorNum = 1025;
		
		ADisk.fill(wbuffer, "Hello World".getBytes());
		
		adisk.writeSector(tid1,sectorNum, wbuffer);
		try {
			adisk.readSector(tid1, sectorNum, rbuffer);
			adisk.commitTransaction(tid1);
			adisk.readSector(tid2, sectorNum, rbuffer);
			Thread.sleep(2000);
			adisk.readSector(tid2, sectorNum, rbuffer);
		}catch (Exception e) {
			assert(false);
		}
		System.out.println("Done.");
		System.exit(0);
	}
}

class StackPopper implements Runnable{
	Stack s;
	public StackPopper(Stack s) {
		this.s = s;
	}
	
	public void run(){
		while(true) {
			Integer x = s.pop();
			if(x != null)
				System.out.println(x);
		}
	}
}

class StackPusher implements Runnable{
	Stack s;
	public StackPusher(Stack s) {
		this.s = s;
	}
	
	public void run() {
		for(int i = 0; i < 1000; i++)
			this.s.push(i);
	}
}

class Stack {
	private SimpleLock lock;
	private ArrayList<Integer> lst;
	
	public Stack() {
		lock = new SimpleLock();
		lst = new ArrayList<Integer>();
	}
	
	public void push(Integer x) {
		lock.lock();
		lst.add(x);
		lock.unlock();
		return;
	}
	
	public Integer pop() {
		lock.lock();
		Integer ret = null;
		if (!lst.isEmpty()) 
			ret = lst.remove(0);
		lock.unlock();
		return ret;
	}
}