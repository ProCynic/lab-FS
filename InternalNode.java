
public class InternalNode extends PointerNode{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7400442076200379954L;

	public InternalNode() {
		this.pointers = new Node[PTree.POINTERS_PER_INTERNAL_NODE];

	}

}
