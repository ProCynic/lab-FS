
public abstract class PointerNode extends Node {
	Node[] pointers;
	
	public PointerNode() {
	}
	
	@SuppressWarnings("unused")
	private int freePointers() {
		int s = 0;
		for (Node n : this.pointers)
			if(n == null)
				s++;
		return s;
	}
	
	public void write(byte[] block) {
		for (Node n : this.pointers) {
			if(n == null)
				n = new Leaf(block);
		}
	}

}
