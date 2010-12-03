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
	
	public byte[] getBytes() {
		byte[] bytes = new byte[this.length() / 8 + (this.length() % 8 != 0 ? 1 : 0)];
	    for (int i=0; i<this.length(); i++) {
	        if (this.get(i)) {
	            bytes[bytes.length-i/8-1] |= 1<<(i%8);
	        }
	    }
	    return bytes;
	}
	
	public void fromBytes(byte[] buffer) {
		for (int i=0; i<buffer.length*8; i++) {
	        if ((buffer[buffer.length-i/8-1]&(1<<(i%8))) > 0) {
	            this.set(i);
	        }
	    }
	}

}
