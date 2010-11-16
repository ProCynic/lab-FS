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
		this.writes = new ArrayList<Write>();
	}
	
	public void add(int sectorNum, byte[] buffer) {
		writes.add(new Write(sectorNum, buffer));
	}

	@Override
	public Iterator<Write> iterator() {
		return writes.iterator();
	}
  
}

class Write {
	public int sectorNum;
	public byte[] buffer;
	
	Write(int sectorNum, byte buffer[]) {
		
		if (buffer.length != Disk.SECTOR_SIZE)
			  throw new IllegalArgumentException();
		
		if (sectorNum < ADisk.REDO_LOG_SECTORS || sectorNum >= Disk.NUM_OF_SECTORS)
			throw new IndexOutOfBoundsException();
		this.sectorNum = sectorNum;
		this.buffer = buffer;
	}
	
	
}