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
		TransID tidw = adisk.beginTransaction();
		TransID tidr = adisk.beginTransaction();
//		assert(adisk.isActive(tid));
//		adisk.abortTransaction(tid);
//		assert(!adisk.isActive(tid));
		
		int sectorNum = 1025;
		byte[] buffer = new byte[Disk.SECTOR_SIZE];
		ADisk.fill(buffer, "Hello World".getBytes());
		int type = Disk.WRITE;
		try {
			adisk.writeSector(tidw, sectorNum, buffer);
			adisk.commitTransaction(tidw);
			Thread.currentThread().sleep(2000);
			byte[] buff2 = ADisk.blankSector();
			adisk.readSector(tidr, sectorNum, buff2);
			for (int i = 0; i < Disk.SECTOR_SIZE; i++) 
				assert(buffer[i] == buff2[i]);
		} catch (Exception e) {
			e.printStackTrace();
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