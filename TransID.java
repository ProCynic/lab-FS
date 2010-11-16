import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * TransId.java
 *
 * Interface to ADisk
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */
public class TransID implements Iterable<Action>{

  //
  // Implement this class
  //
	
	public long tid;
	
	private ArrayList<Action> actions;
	
	public TransID() {
	}

	@Override
	public Iterator<Action> iterator() {
		return actions.iterator();
	}
  
}

abstract class Action {
	protected int ActionTag;
	protected Disk disk;
	protected int sectorNum;
	protected byte[] buffer;
	Action(int sectorNum, byte buffer[], Disk disk) {
		this.disk = disk;
		this.sectorNum = sectorNum;
		this.buffer = buffer;
	}
	
	abstract void apply();
}

class Write extends Action{
	Write(int sectorNum, byte buffer[], Disk disk){
		super(sectorNum, buffer, disk);
	}

	@Override
	void apply() {
		try {
			this.disk.startRequest(Disk.WRITE, this.ActionTag, this.sectorNum, this.buffer);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		
	}
	
}

class Read extends Action{
	Read(int sectorNum, Disk disk, byte[] buffer) {
		super(sectorNum, buffer, disk);
	}

	@Override
	void apply() {
		// TODO Auto-generated method stub
		
	}

}
