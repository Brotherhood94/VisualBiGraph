/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.unipi.visualbigraph;

import com.google.common.primitives.Ints;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.ImmutableSubgraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
/**
 *
 * @author alessandro
 */
public class WebGraphUtility {
    //Eventualmente far restituire counter alle varie funzioni e scriverle nel panel in un label o simili
    private static WebGraphUtility wgu = null;
    
    private static final Logger LOGGER = Logger.getLogger(WebGraphUtility.class.getName());
    
    private BVGraph wbgraph;
    private ImmutableGraph twbgraph;
    private ImmutableSubgraph inducedGraph;
    private ProgressLogger pl;
    private ProjectController pc;
    private Workspace workspace;
    
    private GephiGraph gephiGraph;
    private HashMap<Integer,GephiGraph> graphList = null;
    private boolean degree;
    private boolean imported;
    
    
    private int graphCounter = 0;
    
    private String saveTo;
    
    private WebGraphUtility(){ //queste operazioni dovrebbero essere fatte quando si importa in modo da creare una nuova workspace quando si importa. Ma vorrei davvero crearne una nuova?
        LOGGER.info("Setting up");
        this.pl = new ProgressLogger();
        this.pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        this.workspace = pc.getCurrentWorkspace();//si può fare add alla current workspace
        this.twbgraph = null;
        imported = false;
    }
    
    public static WebGraphUtility FactoryWebGraphUtility(){
        if(wgu == null) //garantisco che ce ne sia solo uno
            wgu = new WebGraphUtility();
        return wgu;
    }
    
    public boolean importer(String loadPath){ //Così, ogni volta che importo un nuovo grafo, creo una nuova workspace. Ma il grafo è sempre lo stesso quindi potrebbe andare bene.
        try{
            LOGGER.info("Loading .graph file");
            this.wbgraph = BVGraph.loadMapped(loadPath,pl);
            if(graphList!=null)
                for(Integer id : graphList.keySet()) //rimuovere tutti dalla selezione
                    graphList.get(id).clearGraph();
            graphList = new HashMap<Integer,GephiGraph>(); //ogni volta che importo un nuovo WebGraph, rimuovo tutti i gephiGraph precedenti
        }catch(IOException e){
            LOGGER.severe(e.getMessage());
            return false;
        }
        try {
            this.saveTo =  loadPath.substring(0, loadPath.lastIndexOf("/"))+"/"+loadPath.substring(loadPath.lastIndexOf("/"), loadPath.length())+"-transpose";
            this.twbgraph = BVGraph.loadMapped(saveTo,pl);
        } catch (IOException ex) {
            LOGGER.warning("No graph's transpose found. When needed will be generated."+saveTo);
        }
        LOGGER.info("Successfully Loaded");
        return (this.imported = true);
    }
        
    public void getSuccessors(int x, int step, int filter){
       LOGGER.info("Successors-----------");
       int k, counter = 0;
       Node n, s;
       s = gephiGraph.addNode(x, Color.green);
       if(step == 0)
           return;
       LazyIntIterator li = wbgraph.successors(x);
       while(( k = li.nextInt()) != -1){
           if(degree && (wbgraph.outdegree(k) > filter))
                    continue;
           LOGGER.info("Successor: "+k);
           n = gephiGraph.addNode(k,Color.green);
           counter++;
           gephiGraph.addEdge(s, n, Color.green);
           this.getSuccessors(k, step-1, filter);
       }
       //gephiGraph.applyRandom_YifanHu(counter);
       LOGGER.info("No. Successors: "+counter);
       return;
    }
    
    public void getPredecessors(int x, int step, int filter){
       LOGGER.info("Predecessors-----------");
       int k, counter = 0;
       Node p, t;
       checkTranspose();
       t = gephiGraph.addNode(x, Color.blue);
       if(step == 0)
           return;
       LazyIntIterator li = twbgraph.successors(x);
       while(( k = li.nextInt()) != -1){
           if(degree && (twbgraph.outdegree(k) > filter)) //prima era wbgraph.outdegree(k) > filter (non quello trasposto)
                    continue;
           LOGGER.info("Predecessor: "+k);
           p = gephiGraph.addNode(k,Color.blue);
           counter++;
           gephiGraph.addEdge(p, t, Color.blue);
           this.getPredecessors(k, step-1, filter);
       }
       //gephiGraph.applyRandom_YifanHu(counter);
       LOGGER.info("No. Predecessors: "+counter);
       return;
    }
    
    public void egoNetwork(int filter){ //To Fix: soluzione possibile: restituire le leaves, prendere la size della queue prima e poi aspettare che tale numero sia a zero. Tanto sono in ordine nella coda
        LOGGER.info("EgoNetwork-----------Degree "+ this.degree);
        int k, r, counter = 0;
        Node t;
        int size = gephiGraph.getLeaves().size();
        LOGGER.info("Leaves size: "+size);
        for(int i = 0; i < size; i++){
            Node m = gephiGraph.getLeaves().poll();
            r = new Integer((String) m.getId());
            LOGGER.info("Leaf: "+r+" degree "+wbgraph.outdegree(r));
            if(degree && (wbgraph.outdegree(r) > filter)){
                    gephiGraph.getLeaves().add(m);
                    continue;
            }
            LazyIntIterator li = wbgraph.successors(r);
            while((k = li.nextInt()) != -1){
                if(degree && (wbgraph.outdegree(k) > filter)){
                    gephiGraph.getLeaves().add(m);        //Se non ci fosse, nel momento in cui disattivo il filtro, non posso recuperare i nodi che avevo filtrato per degree
                }
                LOGGER.info(k+" OutDegree "+wbgraph.outdegree(k));
                t = gephiGraph.addNode(k, Color.black);
                counter++;
                gephiGraph.addEdge(m, t, Color.black);
            }
        }
        //gephiGraph.applyRandom_YifanHu(counter);
        LOGGER.info("No. Neighbors: "+counter);
        return;
    }
    
    private void checkTranspose(){
        if(twbgraph!=null)
            return;
        this.twbgraph = Transform.transpose(wbgraph);
        new Thread(){
            public void run(){
                try {
                    BVGraph.store(twbgraph,saveTo);
                } catch (IOException ex) {
                    LOGGER.severe("Can't store the graph. "+saveTo);
                    Exceptions.printStackTrace(ex);
                }
            }
        }.start();         
    }
    
    public boolean shortestPath(Integer source, Integer target){
        int prec, counter = 0;          //Implementare L'HubFilter
        Node s,t;
        BfsExtended bf;
        bf = new BfsExtended(wbgraph, 6, true, pl);
        LOGGER.info("Searching");
        bf.visit(source, target);
        LOGGER.info("Search Ended. Creating Graph.");
        prec = target;
        t = gephiGraph.addNode(target, Color.red);
        while(prec != source){
            prec = bf.marker.get(prec);
            if( prec == -1){ 
                LOGGER.info("SP NOT FOUND");
                //gephiGraph.removeNode(t);
                return false;
            }
            s = gephiGraph.addNode(prec, Color.red);
            counter++;
            gephiGraph.addEdge(s, t, Color.red);
            t = s;
        }
        LOGGER.info("-- Nodes Added:"+gephiGraph.getNodeCount());
        LOGGER.info("-- Edges Added:"+gephiGraph.getEdgeCount());
        return gephiGraph.applyRandom_YifanHu(counter);
    }
    
    public void appendToGraph(LinkedList<Integer> queue){
        int counter = 0;
        Node depot = gephiGraph.addNode(queue.pollLast(), Color.orange);
        Node s = gephiGraph.addNode(queue.pollFirst(), Color.orange);
        gephiGraph.addEdge(s, depot, Color.orange);
        while(!queue.isEmpty()){
            LOGGER.info("appending "+ (String) s.getId());
            Node t = gephiGraph.addNode(queue.pollFirst(), Color.orange);
            counter++;
            gephiGraph.addEdge(s, t, Color.orange);
            gephiGraph.addEdge(t, depot, Color.orange);
            s = t;
        }
        gephiGraph.applyRandom_YifanHu(counter);
        LOGGER.info("No. Bicycle: "+counter);
    }
    
    public void findDepot(Integer source){      //Da rivedere
        LOGGER.info("Source: "+source);
        int k, p;
        LazyIntIterator li = wbgraph.successors(source);
        HashSet<Integer> s1 = new HashSet<Integer>();
        LinkedList<Integer> queue = new LinkedList<Integer>();
        checkTranspose();
        LOGGER.info("1");
        while( (k = li.nextInt()) != -1){
            s1.add(k);
            LOGGER.info("AddToS1: "+k);
        }
        for(int n : s1){        //lo sto facendo a 2 vie //debolino
            LOGGER.info(k+" Neighbour: "+n);
            LazyIntIterator li2 = wbgraph.successors(n);
            HashSet<Integer> s2 = new HashSet<Integer>();
            while( (p = li2.nextInt()) != -1)
                s2.add(p);
            s2.retainAll(s1);
            LOGGER.info("Retained");
            if(s2.size()==1){ //Se ci sono più di 1 CN? // era ==1 //devo gestire la coda in caso abbia più di un depot
                LOGGER.info("s2.size >= 1");
                Iterator<Integer> idep = s2.iterator();
                int depot = idep.next();
                LOGGER.info("Depot: "+depot+ "Next: "+ n);
                tow2test(source, depot, queue);
                back2test(source, depot, queue);
                //pippo(source, depot, false, queue);
                //pippo(source, depot, true, queue);
                if(queue.size()<3){
                    LOGGER.info("No. Bicycle: 0");
                    return;
                }
                queue.add(depot); //aggiungo depot in coda in modo da recuperarlo finita la catena avanti e indietro
                LOGGER.info("Appending");
                appendToGraph(queue);
            }
        }
    }
        
    public boolean isThereDepot(Integer node, Integer depot){
        HashSet s = new HashSet<Integer>(Ints.asList(wbgraph.successorArray(node)));
        return s.contains(depot);
    }
    
    public void tow2test(Integer source,Integer depot, LinkedList<Integer> queue){
        int node = source;
        int[] succ;
        while(isThereDepot(node, depot)){
            queue.add(node);
            succ = wbgraph.successorArray(node);
            if(succ.length != 2)
                return;
            for(int i = 0; i < succ.length; i++)   //Se arrivo qua sono sicuro che ci sia il depot
                if(succ[i]==depot)
                    node = succ[(i+1)%2];
        }
    }
    
    public void back2test(Integer source,Integer depot, LinkedList<Integer> queue){
        int node = source;
        int[] prec;
        while(isThereDepot(node,depot)){
            queue.addFirst(node);
            prec = twbgraph.successorArray(node);
            if(prec.length!=1)
                return;
            node = prec[0];
        }
    }
         
    //To Fix: posso usare la tab inducted Graph se non voglio utilizzare il webgraph
    public void getInducedSubGraphL(String edgePath, String vertexPath, char edgeRegex, char nodeRegex, boolean isWeighed){  //Dovrebbe poter essere static
        LOGGER.info("InductedGraphL-----------");
        //gephiGraph.clearGraph();
        int counter= 0;
        //Aggiungo i nodi 
        HashSet<Integer> ai = new HashSet<Integer>();
        if(!this.addVertexToGephiGraph(ai, vertexPath, nodeRegex))
            return;
        //ora è diverso per gli archi perchè prende una linea direttamente.
        //In questo caso dovrebbe andare bene perchè il regex settato è ; e per l'edgesFile è la ",". Quindi non trovando "," legge fino ad '\n'
        ConcurrentLinkedQueue<String> queue = new  ConcurrentLinkedQueue<String>();
        Reader mr = new EdgeReader(queue, edgePath);
        mr.run();
        String[] line = null;
        System.out.println("Entro");
        while(queue.size()>0){  //ci sono 2 archi per riga
           System.out.println("Entro");
           try{
               line = queue.poll().split(String.valueOf(edgeRegex)); //Splitta rispetto a qualsiasi spazio, tab, line breaks, enter..
               System.out.println(line[0].trim()+" "+line[1].trim());
               int s = Integer.valueOf(line[0].trim()), t = Integer.valueOf(line[1].trim());
               if(ai.contains(s) && ai.contains(t)){ //Implementare this.addEdge(source, target, color, PESO);
                    Logger.global.info("source: "+s+" target: "+t);
                    if(isWeighed && line.length>2){
                        double w = 0;
                        try{
                            w = Double.valueOf(line[2].trim());
                        }catch(NumberFormatException e){
                            LOGGER.severe("Can't parse weight "+line[2]);
                            gephiGraph.addEdge(gephiGraph.addNode(s, Color.MAGENTA), gephiGraph.addNode(t, Color.MAGENTA), Color.MAGENTA);
                            LOGGER.warning("Added NOT weighted Node.");
                        }
                        gephiGraph.addEdge(gephiGraph.addNode(s, Color.MAGENTA), gephiGraph.addNode(t, Color.MAGENTA), w ,Color.MAGENTA);
                    }else
                        gephiGraph.addEdge(gephiGraph.addNode(s, Color.MAGENTA), gephiGraph.addNode(t, Color.MAGENTA), Color.MAGENTA);
                    counter++;
               }
           }catch(NumberFormatException e){
               LOGGER.severe("Edges Separator may be incorrect. Current separator:  "+edgeRegex);
               mr.interrupt();
           }
        }
        gephiGraph.applyRandom_YifanHu(counter);
        LOGGER.info("--------------");
    }
    
    public void getInducedSubGraphW(String vertexPath, char regex){
        LOGGER.info("InductedGraphW-----------");
        int counter= 0;
        HashSet<Integer> ai = new HashSet<Integer>();
        if(!this.addVertexToGephiGraph(ai, vertexPath, regex))
            return;

        ArrayList<Integer> nai = null;
        Collections.sort(nai = new ArrayList<Integer>(ai));
        this.inducedGraph = new ImmutableSubgraph((ImmutableGraph) wbgraph,nai.stream().filter(i -> i != null).mapToInt(i->i).toArray());  //devono essere in oridinecrescente
        NodeIterator ni = this.inducedGraph.nodeIterator();
        LazyIntIterator li;
        int succ;
        Node source;
        while(ni.hasNext()){
            int vertex = this.inducedGraph.toSupergraphNode(ni.nextInt());
            Logger.global.info("VERT: "+vertex);
            source = gephiGraph.addNode(vertex, Color.MAGENTA);
            li = ni.successors();
            while(( succ = li.nextInt()) != -1){
                int dest = this.inducedGraph.toSupergraphNode(succ);
                Logger.global.info("DEST: "+dest);
                gephiGraph.addEdge(source, gephiGraph.addNode(dest, Color.MAGENTA), Color.MAGENTA);
                counter++;
            }
        }
        gephiGraph.applyRandom_YifanHu(counter);
        LOGGER.info("--------------");
    }
    
    private boolean addVertexToGephiGraph(HashSet<Integer> ai, String vertexPath, char regex){
        int n = 0;
        String inQueue = "";
        ConcurrentLinkedQueue<String> queue = new  ConcurrentLinkedQueue<String>();
        Reader mr = new NodeReader(queue, vertexPath, regex);
        mr.run();
        LOGGER.info("Queue size: "+queue.size());
        while(queue.size()>0){
            LOGGER.info("Queue size: "+queue.size());
            try{
                LOGGER.info("inQueuee= "+(inQueue = queue.poll()));
                n = Integer.valueOf(inQueue.trim());
                ai.add(n); //Tolgo in testa
                gephiGraph.addNode(n, Color.MAGENTA);
            }catch(NumberFormatException e){
                LOGGER.severe("Separator may be incorrect. Current separator: "+regex);
                mr.interrupt();
                return false;
            }
        }
        queue.clear();
        return true;
    }
    
    public void setDegree(boolean degree){
        this.degree = degree;
    }

    public boolean getImported(){ //questo è relativo al WebGraph. Devo importarlo solo una volta indipendentemente dai GephiGraph
        return this.imported;
    }
    
    public void addGraph(JComboBox jcombo){
        graphList.putIfAbsent(this.graphCounter,new GephiGraph(this.graphCounter, workspace));
        jcombo.addItem("Graph_"+this.graphCounter);
        this.graphCounter++;
    }
    
    public void removeGraph(int index, JComboBox jcombo){
        if(graphList.get(index)!=null){
            if(jcombo.getItemCount()==1)
                this.addGraph(jcombo);
            jcombo.removeItemAt(jcombo.getSelectedIndex());
            graphList.get(index).clearGraph();  //elimina TUTTI i nodi indipendentemente dal grafo-- devo per forza fare più worksapce?
            LOGGER.info("Graph "+graphList.get(index).getID()+" removed.");
        }else{
            LOGGER.warning("Graph "+graphList.get(index).getID()+" does not exist.");
        }
    }
        
    public void setGraph(int index){
        if(graphList.get(index)!=null){
            this.gephiGraph = graphList.get(index);
            LOGGER.info("Graph "+graphList.get(index).getID()+" selected.");
        }else{
            LOGGER.warning("Graph "+graphList.get(index).getID()+" does not exist.");
        }
    }
        
}
