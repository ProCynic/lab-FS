import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Node implements Serializable{
	
	int[] pointers;
	
	abstract public byte[] getBytes();
	abstract public void fromBytes(byte[] buffer);

}
