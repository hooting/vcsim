package it.polimi.vcdu.model;

public class PastEdgeOD extends PastEdge {
	public PastEdgeOD(PastEdge pe) {
		super(pe.getFrom(), pe.getTo(), pe.getRid());
	}
	
	public String toString(){
		if (this.representation==null){
			this.representation = super.toString()+"OD";
		}
		return representation;
	}

}
