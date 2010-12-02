
public class Leaf extends Node {
	
	byte[] buffer;

	public Leaf(byte[] block) {
		this.buffer = new byte[PTree.BLOCK_SIZE_BYTES];
		this.write(block);
	}

	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(byte[] block) {
		// TODO Auto-generated method stub
		
	}
}
