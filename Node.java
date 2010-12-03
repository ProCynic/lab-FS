public abstract class Node{
	
	int[] pointers;
	public int location;
	
	public Node(int location) {
		this.location = location;
	}
	
	public Node(int location, byte[] buffer) {
		this(location);
		this.fromBytes(buffer);
	}
	
	abstract public byte[] getBytes();
	abstract public void fromBytes(byte[] buffer);

}
