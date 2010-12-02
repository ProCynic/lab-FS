import java.io.Serializable;


public abstract class Node implements Serializable{
	

	public Node() {
	}
	abstract public void write(byte[] block);

}
