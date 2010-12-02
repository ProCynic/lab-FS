
public class InternalNode extends PointerNode{
	public InternalNode() {
		this.pointers = new Node[PTree.POINTERS_PER_INTERNAL_NODE];

	}
	
	public byte[] getBytes() {
		return new byte[0];  //TODO:  Serialize this object.
	}
}
