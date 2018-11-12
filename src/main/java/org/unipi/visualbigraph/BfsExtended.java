/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.unipi.visualbigraph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author alessandro
 */
public class BfsExtended{
	/** The graph under examination. */
	public final ImmutableGraph graph;
	/** The queue of visited nodes. */
	public final IntArrayList queue;
	/** At the end of a visit, the cutpoints of {@link #queue}. The <var>d</var>-th cutpoint is the first node in the queue at distance <var>d</var>. The
	 * last cutpoint is the queue size. */
	public final IntArrayList cutPoints;
	/** Whether {@link #marker} contains parent nodes or round numbers. */
	public final boolean parent;
	/** The marker array; contains -1 for nodes that have not still been enqueued, the parent of the visit tree if
	 * {@link #parent} is true, or an index increased at each visit if {@link #parent} is false, which in the symmetric case is the index
	 * of the connected component of the node. */
	public final AtomicIntegerArray marker;
	/** The global progress logger. */
	private final ProgressLogger pl;
	/** The number of threads. */
	private final int numberOfThreads;
	/** The number of nodes visited. */
	private final AtomicInteger progress;
	/** The next node position to be picked from the last segment of {@link #queue}. */
	private final AtomicLong nextPosition;
	/** If true, the current visit is over. */
	private volatile boolean completed;
	/** The barrier used to synchronize visiting threads. */
	private volatile CyclicBarrier barrier;
	/** Keeps track of problems in visiting threads. */
	private volatile Throwable threadThrowable;
	/** A number increased at each nonempty visit (used to mark {@link #marker} if {@link #parent} is false). */
	public int round;
	
	/** Creates a new class for keeping track of the state of parallel breadth-first visits. 
	 * 
	 * @param graph a graph.
	 * @param requestedThreads the requested number of threads (0 for {@link Runtime#availableProcessors()}).
	 * @param parent if true, {@link #marker} will contain parent nodes; otherwise, it will contain {@linkplain #round round numbers}.
	 * @param pl a progress logger, or <code>null</code>.
	 */
	public BfsExtended( final ImmutableGraph graph, final int requestedThreads, final boolean parent, final ProgressLogger pl ) {
		this.graph = graph;
		this.parent = parent;
		this.pl = pl;
		this.marker = new AtomicIntegerArray( graph.numNodes() );
		this.queue = new IntArrayList( graph.numNodes() );
		this.progress = new AtomicInteger();
		this.nextPosition = new AtomicLong();
		this.cutPoints = new IntArrayList();
		numberOfThreads = requestedThreads != 0 ? requestedThreads : Runtime.getRuntime().availableProcessors();
		clear();
	}
	
	/** Clears the internal state of the visit, setting all {@link #marker} entries and {@link #round} to -1. */
	public void clear() {
		round = -1;
		for( int i = marker.length(); i-- != 0; ) marker.set( i, -1 );
	}

	private final class IterationThread extends Thread {
		private static final int GRANULARITY = 1000;

		public void run() {
			try {
				// We cache frequently used fields.
				final AtomicIntegerArray marker = BfsExtended.this.marker;
				final ImmutableGraph graph = BfsExtended.this.graph.copy();
				final boolean parent = BfsExtended.this.parent;
				
				for(;;) {
					barrier.await();
					if ( completed ) return;
					final IntArrayList out = new IntArrayList();
					final int first = cutPoints.getInt( cutPoints.size() - 2 );
					final int last = cutPoints.getInt( cutPoints.size() - 1 );
					int mark = round;
					for(;;) {
						// Try to get another piece of work.
						final long start = first + nextPosition.getAndAdd( GRANULARITY );
						if ( start >= last ) {
							nextPosition.getAndAdd( -GRANULARITY );
							break;
						}

						final int end = (int)(Math.min( last, start + GRANULARITY ) );
						out.clear();
						
						for( int pos = (int)start; pos < end; pos++ ) {
							final int curr = queue.getInt( pos );
							if ( parent == true ) mark = curr;
							final LazyIntIterator successors = graph.successors( curr );
							for( int s; ( s = successors.nextInt() ) != -1; ) 
								if ( marker.compareAndSet( s, -1, mark ) ) out.add( s );
						}

						progress.addAndGet( end - (int)start );

						if ( ! out.isEmpty() ) synchronized( queue ) {
							queue.addAll( out );
						}
					}
				}
			}
			catch( Throwable t ) {
				threadThrowable = t;
			}
		}
	}
	

	/** Performs a breadth-first visit of the given graph starting from the given node.
	 * 
	 * <p>This method will increment {@link #round}.
	 * 
	 * @param start the starting node.
	 * @return the number of visited nodes.
	 * @see #visit(int,int)
	 */
	public int visit( final int start, final int target ) {
		return visit( start, target , -1);
	}


	/** Performs a breadth-first visit of the given graph starting from the given node.
	 * 
	 * <p>This method will increment {@link #round} if at least one node is visited.
	 * 
	 * @param start the starting node.
	 * @param expectedSize the expected size (number of nodes) of the visit (for logging), or -1 to use the number of nodes of the graph.
	 * @return the number of visited nodes.
	 */
	public int visit( final int start, final int target, final int expectedSize ) { //Runnable checkEnd implementa il controllo del target
		if ( marker.get( start ) != -1 ) return 0;
		round++;
		completed = false;
		queue.clear();
		cutPoints.clear();
		queue.add( start );
		cutPoints.add( 0 );
		marker.set( start, parent ? start : round );
		final IterationThread[] thread = new IterationThread[ numberOfThreads ];		
		for( int i = thread.length; i-- != 0; ) thread[ i ] = new IterationThread();
		progress.set( 0 );

		if ( pl != null ) {
			pl.start( "Starting visit..." );
			pl.expectedUpdates = expectedSize != -1 ? expectedSize : graph.numNodes();
			pl.itemsName = "nodes";
		}

		barrier = new CyclicBarrier( numberOfThreads, new Runnable() {
			@Override
			public void run() {
				if ( pl != null ) pl.set( progress.get() );
				if  (queue.contains(target) && target!=-1){ /////QUA
					completed = true;
					return;
				}
					
				if ( queue.size() == cutPoints.getInt( cutPoints.size() - 1 ) ) {
					completed = true;
					return;
				}
				
				cutPoints.add( queue.size() );
				nextPosition.set( 0 );
			}
		}
		);

		for( int i = thread.length; i-- != 0; ) thread[ i ].start();		
		for( int i = thread.length; i-- != 0; )
			try {
				thread[ i ].join();
			}
			catch ( InterruptedException e ) {
				throw new RuntimeException( e );
			}
	
		if ( threadThrowable != null ) throw new RuntimeException( threadThrowable );
		if ( pl != null ) pl.done();
		return queue.size();
	}

	/** Visits all nodes. Calls {@link #clear()} initially.
	 * 
	 * <p>This method is more efficient than invoking {@link #visit(int, int)} on all nodes as threads are created just once. 
	 */
	public void visitAll() {
		final IterationThread[] thread = new IterationThread[ numberOfThreads ];		
		for( int i = thread.length; i-- != 0; ) thread[ i ] = new IterationThread();
		final int n = graph.numNodes();
		completed = false;
		clear();
		queue.clear();
		cutPoints.clear();
		progress.set( 0 );
		
		if ( pl != null ) {
			pl.start( "Starting visits..." );
			pl.expectedUpdates = graph.numNodes();
			pl.displayLocalSpeed = true;
			pl.itemsName = "nodes";
		}

		barrier = new CyclicBarrier( numberOfThreads, new Runnable() {
			int curr = -1;
			@Override
			public void run() {
				if ( pl != null ) pl.set( progress.get() );
				// Either first call, or queue did not grow from the last call.
				if ( curr == -1 || queue.size() == cutPoints.getInt( cutPoints.size() - 1 ) ) {
					if ( pl != null ) pl.set( progress.get() );
					// Look for the first nonterminal node not yet visited.
					for(;;) {
						while( ++curr < n && marker.get( curr ) != -1 );
					
						if ( curr == n ) {
							completed = true;
							return;
						}
						else {
							round++;
							marker.set( curr, parent ? curr : round );

							final int d = graph.outdegree( curr );
							if ( d != 0 && ! ( d == 1 && graph.successors( curr ).nextInt() == curr ) ) {
								queue.clear();
								queue.add( curr );

								cutPoints.clear();
								cutPoints.add( 0 );
								break;
							}
						}
					}
				}
				
				cutPoints.add( queue.size() );
				nextPosition.set( 0 );
			}	
		}
		);

		for( int i = thread.length; i-- != 0; ) thread[ i ].start();
		for( int i = thread.length; i-- != 0; )
			try {
				thread[ i ].join();
			}
			catch ( InterruptedException e ) {
				throw new RuntimeException( e );
			}
	
		if ( threadThrowable != null ) throw new RuntimeException( threadThrowable );
		if ( pl != null ) pl.done();
	}
	

	/** Returns a node at maximum distance during the last visit (e.g., a node realising the positive eccentricity of the starting node).
	 * 
	 * @return the maximum distance computed during the last visit.
	 */
	public int nodeAtMaxDistance() {
		return queue.getInt( queue.size() - 1 );
	}

	/** Returns the maximum distance computed during the last visit (e.g., the eccentricity of the source).
	 * 
	 * @return the maximum distance computed during the last visit.
	 */
	
	public int maxDistance() {
		return cutPoints.size() - 2;
	}
}
