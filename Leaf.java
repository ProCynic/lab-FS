
public class Leaf extends Node {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4069955652884432367L;
	byte[] buffer;

	public Leaf(byte[] block) {
		this.buffer = new byte[PTree.BLOCK_SIZE_BYTES];
		this.write(block);
	}


	@Override
	public void write(byte[] block) {
		// TODO Auto-generated method stub
		
	}
}
