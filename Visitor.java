import java.io.IOException;


public abstract class Visitor {
	protected TransID tid;
	public Visitor(TransID tid) {
		this.tid = tid;
	}
	public abstract void visit(Class type, int location, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, IOException;
}
