import java.io.IOException;


public abstract class Visitor {
	protected TransID tid;
	public Visitor(TransID tid) {
		this.tid = tid;
	}
	public abstract void visit(Node current, Object next) throws IllegalArgumentException, IndexOutOfBoundsException, IOException;
}
