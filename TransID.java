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
public class TransID implements Iterable<Write>{

  //
  // Implement this class
  //
	
	public long tid;
	
	private ArrayList<Write> writes;
	
	public TransID() {
	}

	@Override
	public Iterator<Write> iterator() {
		return writes.iterator();
	}
  
}

class Write {
	protected int WriteTag;
	protected Disk disk;
	protected int sectorNum;
	protected byte[] buffer;
	Write(int sectorNum, byte buffer[], Disk disk) {
		this.disk = disk;
		this.sectorNum = sectorNum;
		this.buffer = buffer;
	}

	void apply() {
		try {
			this.disk.startRequest(Disk.WRITE, this.WriteTag, this.sectorNum, this.buffer);  //TODO: sectorNum should be logHead
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