import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;


public class Transaction implements Iterable<Write>{
	
	public static final Sector COMMIT = new Sector("Commit".getBytes());

	private ArrayList<Write> writes;

	public Transaction() {
		this.writes = new ArrayList<Write>();
	}

	public void add(int sectorNum, byte[] buffer) throws IndexOutOfBoundsException, IllegalArgumentException{
		if (writes.size() == Common.MAX_WRITES_PER_TRANSACTION)
			throw new ResourceException();
		writes.add(new Write(sectorNum, buffer));
	}

	public Iterator<Write> iterator() {
		return writes.iterator();
	}
	
	//Return the size of the transaction, in sectors
	public int size() {
		return this.writes.size() + 2;
	}
	
	public ArrayList<byte[]>getSectors() {
		ArrayList<byte[]> bytes = new ArrayList<byte[]>();
		ByteBuffer b = ByteBuffer.allocate(Disk.SECTOR_SIZE);
		b.put("Transaction Metadata".getBytes());  
		
		for (Write w : this)
			b.putInt(w.sectorNum);
		bytes.add(b.array());

		for (Write w : this)
			bytes.add(w.buffer);
		bytes.add(Transaction.COMMIT.array);
		return bytes;
	}
}

class Write {
	public int sectorNum;
	public byte[] buffer;
	
	Write(int sectorNum, byte buffer[]) throws IllegalArgumentException, IndexOutOfBoundsException{
		if (sectorNum < ADisk.REDO_LOG_SECTORS + 1)
			throw new IndexOutOfBoundsException();
		
		if (buffer.length != Disk.SECTOR_SIZE)
			  throw new IllegalArgumentException();
		
		if (sectorNum < ADisk.REDO_LOG_SECTORS || sectorNum >= Disk.NUM_OF_SECTORS)
			throw new IndexOutOfBoundsException();
		this.sectorNum = sectorNum;
		this.buffer = buffer;
	}
}

@SuppressWarnings("serial")
class SegFault extends RuntimeException {
	SegFault() {
		//TODO: Maybe do something with this
	}
}