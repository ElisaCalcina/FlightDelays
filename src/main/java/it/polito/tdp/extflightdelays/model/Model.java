package it.polito.tdp.extflightdelays.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {
	
	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	//vertici sono aeroporti--> oggetti di classe Aeroporto con hashCode e equals --> faccio idMap
	private Map<Integer, Airport> idMap;
	private ExtFlightDelaysDAO dao;
	//mappa che modella le relazioni padre-figlio --> mappa di visita ; e poi la uso per estrarre un percorso possibile che modello come lista di aeroporti
	private Map<Airport, Airport> visita;
	
	public Model() {
		idMap= new HashMap<Integer, Airport>();
		dao = new ExtFlightDelaysDAO();
		this.dao.loadAllAirports(idMap);
	}
	
	public void creaGrafo(int x) {
		this.grafo= new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		//aggiungiamo i vertici --> solo quelli che rispettano il vincolo
		for(Airport a: idMap.values()) {
			if(dao.getAirlinesNumber(a)>x) {
				//inserisco l'aeroporto come vertice
				this.grafo.addVertex(a);
			}
		}
		
		for(Rotta r: dao.getRotte(idMap)) {
			//grafo non orientato--> consideriamo verso A-B e B-A
			//mi faccio dare tutte le rotte dal dao e poi le scorro e faccio il controllo dei doppioni qui
			//esiste già arco tra vertici A e B? se no, aggiungo arco con il peso che recupero dalla rotta
												//se si, i due vertici sono già collegati e quindi non aggiungo arco, prendo il peso dell'arco e lo modifico con il peso della rotta che sto considerando
			
			//controlliamo che i vertici siano presenti
			if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
			
			DefaultWeightedEdge e= this.grafo.getEdge(r.a1, r.a2);
			if(e==null) {
				//arco non c'è ancora e lo inserisco
				Graphs.addEdgeWithVertices(grafo, r.getA1(), r.getA2(), r.getPeso());
			} else {
				double pesoVecchio= this.grafo.getEdgeWeight(e);
				double pesoNuovo=pesoVecchio+r.getPeso();
				//modifico il peso dell'arco
				this.grafo.setEdgeWeight(e, pesoNuovo);
				}
			}
			
		}
	}
	public int vertexNumber() {
		return this.grafo.vertexSet().size();
	}
	
	public int edgeNumber() {
		return this.grafo.edgeSet().size();
	}
	
	public Collection<Airport> getAeroporti(){
		return this.grafo.vertexSet();
	}
	
	//lista di aeroporti che modella il percorso tra due aeroporti: se la lista è nulla non c'è connessione tra i due aeroporti, altrimenti sono connessi
	public List<Airport> trovaPercorso(Airport a1, Airport a2){
		List<Airport> percorso = new ArrayList<>();
		visita= new HashMap<>();
		//visito il grafo e tengo traccia dell'albero di visita e per tenere traccia dell'albero ogni volta che il metodo viene chiamato si usa una mappa di visita
		BreadthFirstIterator<Airport, DefaultWeightedEdge> it= new BreadthFirstIterator<>(this.grafo, a1);
		
		//prima di aggiungere traversalListener aggiungiamo la radice del mio albero di visita
		visita.put(a1, null);
		
		it.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {				
			}
			
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				//quando attraversiamo un arco nella visita ci salviamo la relazione padre-figlio tra nodo sorgente e destinazione nell'albero di visite(mappa per noi)
				Airport sorgente= grafo.getEdgeSource(e.getEdge());
				Airport destinazione= grafo.getEdgeTarget(e.getEdge());
				
				//salvo albero di visita
				if(!visita.containsKey(destinazione) && visita.containsKey(sorgente)) {
					visita.put(destinazione, sorgente);
				}
				else if(!visita.containsKey(sorgente) && visita.containsKey(destinazione)) {
					visita.put(sorgente, destinazione);
				}
				
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
			}
			
		});
		//ora visitiamo il grafo
		while(it.hasNext()) {
			it.next();
		}
		
		//quando la visita termina abbiamo l'albero
		//controlliamo se i due aeroporti sono collegati o no
		if(!visita.containsKey(a1) || !visita.containsKey(a2)) {
			//i due aeroporti non sono collegati
			return null;
		}
		//altrimenti estrapolo dalla mappa il percorso seguendo la relazione padre-figlio nella mappa
		Airport step= a2;
		while(!step.equals(a1)) {
			percorso.add(step);
			step= visita.get(step); //risalgo di uno step finchè non ritrovo la partenza (a1)
		}
		percorso.add(a1);
		return percorso;
	}
}
