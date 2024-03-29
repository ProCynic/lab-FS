import java.util.BitSet;


public class BitMap extends BitSet {

	public BitMap() {
		// TODO Auto-generated constructor stub
	}

	public BitMap(int nbits) {
		super(nbits);
		
	}
	
	public BitMap(byte[] buffer) {
		super(buffer.length*8);
	    fromBytes(buffer);
	}
	
	public BitMap(BitSet bitSet) {
		super();
		this.or(bitSet);
	}

	public byte[] getBytes() {
		byte[] bytes = new byte[(PTree.ROOTS_LOCATION - PTree.FREE_LIST_LOCATION) * Disk.SECTOR_SIZE];
		int x = this.length();
	    for (int i=0; i<this.length(); i++) {
	        if ((boolean)this.get(i)) {
	            bytes[i/8] |= 1<<(8 - i%8);
	        }
	    }
	    return bytes;
	}
	
	public void fromBytes(byte[] buffer) {
		for (int i=0; i<buffer.length*8; i++) {
	        if ((buffer[i/8]&(1<<(8 - i%8))) > 0) {
	            this.set(i);
	        }
	    }
	}

}
