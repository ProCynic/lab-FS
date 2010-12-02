
public class TNode extends PointerNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3601724349004752290L;
	public byte[] metadata;
	public int treeHeight;
	int TNum;  //Do we need this?
	public static final int TNODE_SIZE = 0; //TODO:  Set to actual size
	
	public TNode(int tnum) {
		this.metadata = new byte[PTree.METADATA_SIZE];
		this.pointers = new Node[PTree.TNODE_POINTERS];
		this.TNum = tnum;
		this.treeHeight = 0;
	}


}
