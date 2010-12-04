import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Node implements Serializable{
	
	public static final int NULL_PTR = Integer.MIN_VALUE;
	
	int[] pointers;
	
//	abstract public byte[] getBytes();
//	abstract public void fromBytes(byte[] buffer);

}
