package it.polimi.vcdu.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.renderers.Renderer;


public class TopologyGenerator<V,E> {
	
	int numNodes;
	int maxEdges;
	Factory<Graph<V,E>> facGraph;
	Factory<V> facVertex;
	Factory<E> facEdge;
	
	RandomEdgeBAGenerator<V,E> bag;
	Graph<V,E> mg;
	
	
	public TopologyGenerator(int n_nodes, int max_edges,int seed,
			Factory<Graph<V,E>> fg, Factory<V> fv, Factory<E> fe){
		this.numNodes = n_nodes;
		this.maxEdges = max_edges;
		this.facGraph = fg;
		this.facVertex = fv;
		this.facEdge = fe;
		
		int init_nodes = (int) (Math.log(n_nodes)/Math.log(2));
		this.bag = new  RandomEdgeBAGenerator<V,E>(fg, fv, fe, init_nodes, max_edges, 
				seed, new HashSet<V>() );
	}
	
	public Graph<V,E> generate(){
		mg = bag.create();
		while (mg.getVertexCount() < this.numNodes){ bag.evolveGraph(1);};
		return mg;
	};
	
	
	public int getNumNodes() {
		return numNodes;
	}

	public int getMaxEdges() {
		return maxEdges;
	}

	public RandomEdgeBAGenerator<V, E> getBag() {
		return bag;
	}





	public JFrame visualize(Transformer<V,String> tv) {
		//Layout<Number, Number> layout = new CircleLayout(mg);
		Layout<V, E> layout = new FRLayout2<V, E>(mg);
		//Layout<Number, Number> layout = new SpringLayout2(mg);
//		layout.setSize(new Dimension(600,600)); // sets the initial size of the space
		// The BasicVisualizationServer<V,E> is parameterized by the edge types
//		BasicVisualizationServer<Number,Number> vv = 
//			new BasicVisualizationServer<Number,Number>(layout);
//		vv.setPreferredSize(new Dimension(650,650)); //Sets the viewing area size

		VisualizationViewer<V,E> vv = 
			new VisualizationViewer<V,E>(layout, new Dimension(650,650));
		vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
		vv.getRenderContext().setVertexLabelTransformer(tv);
		vv.setForeground(Color.white);
		
		JFrame frame = new JFrame("Simple Graph View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(vv);
		frame.pack();
		frame.setVisible(true);
		
//		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		return frame;
	}
	
	public void save(String fName) throws IOException{
		GraphMLWriter<V,E> gmlwtr = new GraphMLWriter<V,E>();
		gmlwtr.setVertexIDs(new Transformer<V,String>(){
						@Override
						public String transform(V arg0) {
/*							if (arg0 instanceof agg.xt_basis.Node){
								return ((agg.xt_basis.Node)arg0).getObjectName();
							}*/
							return arg0.toString();
						}
					});
		FileWriter fr = new FileWriter(fName +".gml");
		gmlwtr.save(mg, fr);
	}
	
	public void saveJFrame(String fName, JFrame frame) throws IOException{
			Dimension size = frame.getSize();
	      //BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
	      BufferedImage image = (BufferedImage)frame.createImage(size.width, size.height);
	      Graphics g = image.getGraphics();
	      frame.paint(g);
	      ImageIO.write(image, "gif", new File(fName+".gif"));

	      g.dispose();
	  
	}
}
