import java.io.Serializable;


@SuppressWarnings("serial")
public abstract class Node implements Serializable{
	

	public Node() {
	}
	abstract public void write(byte[] block);

}
