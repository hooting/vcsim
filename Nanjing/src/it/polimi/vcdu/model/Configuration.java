/**
 * Configuration is responsible to generate the list of components by visiting the graph, to create and initialize the ports and
 *  to perform the connections between peer outPorts-inPorts
 */
package it.polimi.vcdu.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import edu.uci.ics.jung.graph.Graph;


/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class Configuration {
	
	private ArrayList<Component> components= new ArrayList<Component>();
	private ArrayList<Edge> edges= new ArrayList<Edge>();

	public Configuration(Graph<Number,Number> graph){
		//creating the lists of components and edges
		initialize(graph);
	}

	private void initialize(Graph<Number, Number> graph) {
		
		HashMap<Number, InPort> edgeToInPort = new HashMap<Number,InPort>();
		HashMap<Number, OutPort> edgeToOutPort= new HashMap<Number,OutPort>();
		
		Collection<Number> vertices= graph.getVertices();
		Iterator<Number> i=vertices.iterator();
		while(i.hasNext()){
			Number v= i.next();
			String component_id="C"+v.toString();
			
			ArrayList<OutPort> ops = new ArrayList<OutPort>();
			Collection<Number> outedges = graph.getOutEdges(v);
			String opNamePrefix="op";
			int nthop=0;
			for(Number e:outedges){
				OutPort op = new OutPort(opNamePrefix+nthop);
				ops.add(op);
				edgeToOutPort.put(e, op);
				nthop++;
			}
			
			ArrayList<InPort> ips = new ArrayList<InPort>();
			Collection<Number> inedges = graph.getInEdges(v);
			String ipNamePrefix ="ip";
			int nthip=0;
			for(Number e:inedges){
				InPort ip = new InPort(ipNamePrefix+nthip);
				ips.add(ip);
				edgeToInPort.put(e, ip);
				nthip++;
			}
			
			Component c;
			c = new Component(this, component_id, ips, ops);
			
			this.components.add(c);
		}
		Collection<Number> numberEdges= graph.getEdges();
		for(Number e:numberEdges){
			StaticEdge edge= new StaticEdge(edgeToOutPort.get(e),edgeToInPort.get(e));
			edge.getFrom().setPeerPort(edge.getTo());
			edge.getTo().setPeerPort(edge.getFrom());
			edge.getTo().setIncidentEdge(edge);
			edge.getFrom().setIncidentEdge(edge);
			edges.add(edge);
		}

	}

	/**
	 * @return the components
	 */

	public ArrayList<Component> getComponents() {
		return components;
	}

	public Component getComponentFromId(String id)
	{
		Iterator<Component> i= this.components.iterator();
		while(i.hasNext()){
			Component c =i.next();
			if (c.getId().equals(id))
				return c; 
		}
		return null;
	}

	public String toString(){
		String out="Configuration: \n";
		Iterator<Component> i= this.components.iterator();
		while(i.hasNext()){
			Component c =i.next();
			out+=c.toString();
		}
		return out;	
		
	}



}
