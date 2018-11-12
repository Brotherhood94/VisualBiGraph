/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.unipi.visualbigraph;

import java.awt.Color;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

/**
 *
 * @author alessandro
 */
public class GephiGraph {
    
    //Sembra che la workspace definisca i nodi. Perciò diveris GephiGraph si riferiscono allo stesso insieme di nodi ma con leaves diverse.
    //Mi conviene creare una workspace indipendente? --> in teoria no perché volevo questo ma mi dà Concurrent modification  con egoNetwork
    private static final Logger LOGGER = Logger.getLogger(GephiGraph.class.getName());
    private GraphModel graphModel;
    private DirectedGraph directedGraph;
    private ConcurrentLinkedQueue<Node> leaves;
    private String ID;
    public GephiGraph(int ID, Workspace workspace){
        this.ID = String.valueOf(ID);
        this.graphModel =  Lookup.getDefault().lookup(GraphController.class).getGraphModel(workspace);
        this.directedGraph = graphModel.getDirectedGraph();
        this.leaves = new ConcurrentLinkedQueue<Node>();
    }
    
    public String getID(){
        return this.ID;
    }
        
    public Edge addEdge(Node source, Node target, Color c){
        Edge e = null;
        if( (e = directedGraph.getEdge(source, target)) == null){
            directedGraph.addEdge(e = graphModel.factory().newEdge(source, target, true));
        }
        e.setColor(c);
        return e;
    }
    
    public Node addNode(Integer id, Color c){
        Node node = null;
        if( (node = directedGraph.getNode(String.valueOf(id))) == null){        //Controllare che sia anche nel webgraph. Sennò potrei inserire un nodo che non c'è
            directedGraph.addNode(node = graphModel.factory().newNode(String.valueOf(id)));
            LOGGER.info("Insert new Node: "+id.intValue());
            node.setSize(3.0F);
            node.setLabel(this.ID);
            leaves.add(node);
        }
        node.setColor(c);
        return node;
    }
    
    public Edge addEdge(Node source, Node target, double weight, Color c){
        Edge e = null;
        if( (e = directedGraph.getEdge(source, target)) == null){
            directedGraph.addEdge(e = graphModel.factory().newEdge(source, target, true));
            e.setWeight(weight);
        }
        e.setColor(c);
        return e;
    }
    
    public Node addNode(Integer id, float amount, Color c){
        Node node = null;
        if( (node = directedGraph.getNode(String.valueOf(id))) == null){        //Controllare che sia anche nel webgraph. Sennò potrei inserire un nodo che non c'è
            directedGraph.addNode(node = graphModel.factory().newNode(String.valueOf(id)));
            LOGGER.info("Insert new Node: "+id.intValue());
            node.setSize(amount);
            node.setLabel(this.ID);
            leaves.add(node);
        }
        node.setColor(c);
        return node;
    }
    
    public boolean applyRandom_YifanHu(int counter){
        if(counter>0){ //altrimenti si impalla la prima volta che uso il layout. Es se un nodo non ha successori si impalla. Ma non si impalla se prima avevo scovato altri nodi
            Layouts.Random_YifanHu(graphModel);
            return true;
        }
        return false;
    }
    
    public void clearGraph(){
        if(this.directedGraph!=null){
            this.leaves.clear();
            for(Node n : this.directedGraph.getNodes().toCollection())
                if(n.getLabel().equals(this.ID))
                    this.directedGraph.removeNode(n);
        }
    }
    
    public int getEdgeCount(){
        return this.directedGraph.getEdgeCount();
    }
    
    public int getNodeCount(){
        return this.directedGraph.getNodeCount();
    }
    
    public Iterator<Node> getLeavesIterator(){
        return this.leaves.iterator();
    }
    
    public void removeNode(Node n){ //Da controllare che sia presente
        this.directedGraph.removeNode(n);
    }
    
    public ConcurrentLinkedQueue<Node> getLeaves(){
        return this.leaves;
    }
    

    
}
