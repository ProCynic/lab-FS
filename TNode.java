
public class TNode extends PointerNode {
	byte[] metadata;
	int treeHeight;
	int TNum;
	public static final int TNODE_SIZE = 0; //TODO:  Set to actual size
	
	public TNode(int tnum) {
		this.metadata = new byte[PTree.METADATA_SIZE];
		this.pointers = new Node[PTree.TNODE_POINTERS];
		this.TNum = tnum;
		this.treeHeight = 0;
	}

	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

}
