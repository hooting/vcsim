package it.polimi.vcdu.sim.record;

import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Iteration;
import it.polimi.vcdu.model.IterationNode;
import it.polimi.vcdu.model.OutPort;
import it.polimi.vcdu.model.Transaction;

import java.util.ArrayList;

public class TransactionRecord {
		// for recording and replaying:
	private String id;
	private float[] delays;
	private String[] outPortIDs;
	private float  theLocalProcessingTime;
	// we do not need ancestors;
	public TransactionRecord(Transaction tx){
		this.id = tx.getId();
		this.theLocalProcessingTime=tx.getIteration().getLocalProcessingTime();
		ArrayList<IterationNode> list = tx.getIteration().getList();
		delays = new float[list.size()];
		outPortIDs = new String[list.size()];
		for(int i=0;i<list.size();i++){
			delays[i] = list.get(i).getDelay();
			OutPort op = list.get(i).getOutPort();
			if(op==null){
				outPortIDs[i] = null;
			}
			else{
				outPortIDs[i] = op.getId();
			}
		}
	}
	
	public Transaction getTransaction(Component host, ArrayList<String> ancestors){
		ArrayList<IterationNode> iterNodeList = new ArrayList<IterationNode>(delays.length);
		for(int i=0;i<delays.length;i++){
			OutPort theOp = null;
			if(outPortIDs[i]==null) {
				theOp = null;
			}else{
				for (OutPort op : host.getOutPorts()){
					if (op.getId().equals(outPortIDs[i])) {
						theOp = op;
						break;
					}
				}
				assert theOp!=null;
			}
			iterNodeList.add(new IterationNode(delays[i], theOp));
		}
		Iteration iter = new Iteration( this.theLocalProcessingTime, iterNodeList);
		Transaction tx = new Transaction(this.id, host, ancestors, iter);
		return tx;
	}

}
