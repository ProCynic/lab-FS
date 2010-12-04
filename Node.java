import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Node implements Serializable{
	
	public static final short NULL_PTR = (short) Short.MIN_VALUE;
	
	short[] pointers;

	public abstract byte[] getBytes();

	public abstract void fromBytes(byte[] buffer) throws ClassNotFoundException;
	
//	abstract public byte[] getBytes();
//	abstract public void fromBytes(byte[] buffer);

}
