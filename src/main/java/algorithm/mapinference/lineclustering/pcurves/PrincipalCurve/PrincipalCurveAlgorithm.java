package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve;

import algorithm.mapinference.lineclustering.pcurves.Optimize.Optimizer;

public class PrincipalCurveAlgorithm {
	// Debug strings
	public final static String DEBUG_INITIALIZE = "Initializing... ";
	public final static String DEBUG_INITIALIZE_TO_CURVES = "Initializing to set of curves... ";
	public final static String DEBUG_ADD_VERTEX_AS_ONE_MIDPOINT = "Adding vertex as one midpoint... ";
	public final static String DEBUG_REPARTITION_VORONOI_REGIONS = "Repartitioning Voronoi-regions... ";
	public final static String DEBUG_OPTIMIZE_VERTICES = "Optimizing vertices... ";
	public final static String DEBUG_INNER_ITERATION = "Inner iteration... ";
	public final static String DEBUG_OUTER_ITERATION = "Outer iteration... ";
	// Principal Curve members
	PrincipalCurveClass principalCurve;
	int iteration;
	boolean stop;
	private PrincipalCurveParameters principalCurveParameters;
	
	public PrincipalCurveAlgorithm(PrincipalCurveClass initialCurve, PrincipalCurveParameters principalCurveParameters) {
		this.principalCurveParameters = principalCurveParameters;
		InitializeMembers(initialCurve);
	}
	
	final protected void InitializeMembers(PrincipalCurveClass initialCurve) {
		principalCurve = initialCurve;
		stop = false;
	}
	
	final public void start(int randomSeed) throws InterruptedException, IllegalStateException {
		Initialize(randomSeed);
		Continue();
	}
	
	public synchronized void Continue() throws InterruptedException, IllegalStateException {
		// Debug fd = new Debug(DEBUG_OUTER_ITERATION);
		try {
			wait(0, 1);
		} catch (InterruptedException e) {
		}
		
		// fd.SetIterator(principalCurve.GetSize()-2);
		boolean cont = true;
		while (cont) {
			// fd.Iterate();
			try {
				wait(0, 1);
			} catch (InterruptedException e) {
			}
			OuterStep();
			cont = !stop && AddOneVertexAsMidpoint(false);
		}
		
		// fd.Terminate();
		try {
			wait(0, 1);
		} catch (InterruptedException e) {
		}
	}
	
	final public boolean InnerStep() throws IllegalStateException {
		RepartitionVoronoiRegions();
		return OptimizeVertices();
	}
	
	public synchronized void OuterStep() throws InterruptedException, IllegalStateException {
		// Debug fd = new Debug(DEBUG_INNER_ITERATION);
		try {
			wait(0, 1);
		} catch (InterruptedException e) {
		}
		iteration = 0;
		boolean cont = true;
		while (!stop && (iteration < 2 || cont) && iteration < 500) {
			if (Thread.interrupted())
				throw new InterruptedException();
			iteration++;
			// fd.Iterate();
			try {
				wait(0, 1);
			} catch (InterruptedException e) {
			}
			cont = InnerStep();
		}
		// fd.Terminate();
		try {
			wait(0, 1);
		} catch (InterruptedException e) {
		}
	}
	
	public synchronized void Initialize(int randomSeed) {
		
		iteration = 0;
		
		principalCurve.InitializeToPrincipalComponent(randomSeed);
		
	}
	
	final public synchronized boolean AddOneVertexAsMidpoint(boolean addAnyway) {
		// Debug fd = new Debug(DEBUG_ADD_VERTEX_AS_ONE_MIDPOINT);
		try {
			wait(0, 1);
		} catch (InterruptedException e) {
		}
		
		boolean cont = false;
		
		if (principalCurveParameters.addVertexMode == PrincipalCurveParameters.ADD_ONE_VERTEX)
			cont = principalCurve.AddOneVertexAsMidpoint(addAnyway);
		else if (principalCurveParameters.addVertexMode == PrincipalCurveParameters.ADD_VERTICES)
			cont = principalCurve.AddVerticesAsMidpoints(addAnyway);
		else if (principalCurveParameters.addVertexMode == PrincipalCurveParameters.ADD_ONE_VERTEX_TO_LONGEST)
			cont = principalCurve.AddOneVertexAsMidpointOfLongestSegment(addAnyway);
		
		// fd.Terminate();
		try {
			wait(1);
		} catch (InterruptedException e) {
		}
		
		return cont;
	}
	
	private synchronized void RepartitionVoronoiRegions() throws IllegalStateException {
		// Debug fd = new Debug(DEBUG_REPARTITION_VORONOI_REGIONS);
		try {
			wait(0, 1);
		} catch (InterruptedException e) {
		}
		
		principalCurve.RepartitionVoronoiRegions();
		
		// fd.Terminate();
	}
	
	private synchronized boolean OptimizeVertices() {
		// Debug fd = new Debug(DEBUG_OPTIMIZE_VERTICES);
		try {
			wait(0, 1);
		} catch (InterruptedException e) {
		}
		
		principalCurve.SetSteepestDescentDirections();
		Optimizer optimizer = new Optimizer(principalCurve);
		double criterionBefore = principalCurve.GetCriterion();
		double criterionAfter = optimizer.Optimize(principalCurveParameters.relativeChangeInCriterionThreshold, 1);
		
		// fd.Terminate();
		try {
			wait(1);
		} catch (InterruptedException e) {
		}
		
		return Math.abs((criterionBefore - criterionAfter) / criterionBefore) >= principalCurveParameters.relativeChangeInCriterionThreshold;
	}
	
}
