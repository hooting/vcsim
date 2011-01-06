package it.polimi.vcdu.model;

public class FutureEdgeOD extends FutureEdge {

	public FutureEdgeOD(FutureEdge fe) {
		super(fe.getFrom(), fe.getTo(), fe.getRid());
	}
	
	public String toString(){
		if (this.representation==null){
			this.representation = super.toString()+"OD";
		}
		return representation;
	}

}
