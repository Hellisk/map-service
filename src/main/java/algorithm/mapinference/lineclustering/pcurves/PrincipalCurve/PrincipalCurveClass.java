package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve;

import algorithm.mapinference.lineclustering.pcurves.LinearAlgebra.*;
import algorithm.mapinference.lineclustering.pcurves.Optimize.Optimizable;
import algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.*;
import algorithm.mapinference.lineclustering.pcurves.PrincipalCurve.Vertex.Elements.*;
import algorithm.mapinference.lineclustering.pcurves.Utilities.MyMath;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PrincipalCurveClass extends GraphAbstract implements Optimizable {
	
	private static final Logger LOG = Logger.getLogger(PrincipalCurveClass.class);
	private List<Edge> edges;
	private PrincipalCurveSample sample;
	private Sample oldVertices = new Sample(); // vertices from the previous iteration
	private double MSE;
	private double penalty;
	private int vertexDeletionCount = 0;
	@SuppressWarnings("unused")
	private double objective;
	
	private PrincipalCurveParameters principalCurveParameters;
	// Initialization for the optimization routine
	private Vektor[] steepestDescentDirections;
	
	public PrincipalCurveClass(Sample in_sample, PrincipalCurveParameters principalCurveParameters) {
		super();
		this.principalCurveParameters = principalCurveParameters;
		points = new ArrayList<>(in_sample.getSize());
		edges = new ArrayList<>(in_sample.getSize());
		sample = new PrincipalCurveSample(in_sample);
		MSE = penalty = objective = 0;
		ElementVertex.principalCurve = this;
		Vertex.prototypeVektor = in_sample.GetPointAt(0).DefaultClone();
	}
	
	final public LineSegmentObject GetLineSegmentObjectAt(int index) {
		return GetEdgeAt(index).GetEdgeAsLineSegmentObject();
	}
	
	@Override
	final public int GetVektorIndex1OfEdgeAt(int index) {
		return GetEdgeAt(index).GetVertexIndex1();
	}
	
	@Override
	final public int GetVektorIndex2OfEdgeAt(int index) {
		return GetEdgeAt(index).GetVertexIndex2();
	}
	
	@Override
	final public int GetNumOfLineSegments() {
		return edges.size();
	}
	
	final public Edge GetEdgeAt(int index) {
		return (edges.get(index));
	}
	
	final Vertex GetVertexAt(int index) {
		return (Vertex) (GetPointAt(index));
	}
	
	final int GetNumOfVertexes() {
		return getSize();
	}
	
	final PrincipalCurveSampleVektor GetSamplePointAt(int index) {
		return sample.GetSamplePointAt(index);
	}
	
	final public double GetSampleWeight() {
		return sample.GetWeight();
	}
	
	@Override
	final public void Reset() {
		points.clear();
		edges.clear();
	}
	
	final public void InitializeToPrincipalComponent(int randomSeed) {
		Reset();
		
		LineObject pc = sample.FirstPrincipalComponentSmallSample(100, randomSeed);
		Vektor projectionVektor = GetSamplePointAt(0).Project(pc);
		
		double min = projectionVektor.GetCoords(0);
		double max = projectionVektor.GetCoords(0);
		int mindex = 0;
		int maxdex = 0;
		
		// Find the minimum and maximum X coordinate of the projection points
		// for the endpoints of the initial principal segment
		for (int j = 1; j < sample.GetSize(); j++) {
			projectionVektor = GetSamplePointAt(j).Project(pc);
			if (max < projectionVektor.GetCoords(0)) {
				maxdex = j;
				max = projectionVektor.GetCoords(0);
			}
			if (min > projectionVektor.GetCoords(0)) {
				mindex = j;
				min = projectionVektor.GetCoords(0);
			}
		}
		InsertFirstTwoPoints(GetSamplePointAt(mindex).Project(pc), GetSamplePointAt(maxdex).Project(pc));
		
	}
	
	final public void InitializeToCurves(SetOfCurves setOfCurves, double joinThreshold) {
		Reset();
		
		int i, j, k;
		int ei, size;
		Curve curve;
		setOfCurves = setOfCurves.Clone();
		
		// Updating the vertices of the curves to EndVertices and LineVertices
		for (i = 0; i < setOfCurves.GetNumOfCurves(); i++) {
			ei = GetNumOfLineSegments();
			curve = setOfCurves.GetCurveAt(i);
			size = curve.getSize();
			
			for (j = 0; j < size; j++)
				curve.SetPointAt(new DummyVertex(curve.GetPointAt(j)), j);
			for (j = 0; j < size - 1; j++)
				AddEdge(new Edge(curve, j, j + 1));
			
			if (size == 2) {
				curve.SetPointAt(new EndVertex((Vertex) curve.GetPointAt(0), ei), 0);
				curve.SetPointAt(new EndVertex((Vertex) curve.GetPointAt(1), GetNumOfLineSegments() - 1), 1);
			} else {
				curve.SetPointAt(new EndVertex((Vertex) curve.GetPointAt(0), ei, ei + 1), 0);
				curve.SetPointAt(new EndVertex((Vertex) curve.GetPointAt(size - 1), GetNumOfLineSegments() - 1,
						GetNumOfLineSegments() - 2), size - 1);
				if (size == 3)
					curve.SetPointAt(new LineVertex((Vertex) curve.GetPointAt(1), ei, ei + 1), 1);
				else {
					curve.SetPointAt(new LineVertex((Vertex) curve.GetPointAt(1), ei, ei + 1, ei + 2), 1);
					curve.SetPointAt(new LineVertex((Vertex) curve.GetPointAt(size - 2), GetNumOfLineSegments() - 1,
							GetNumOfLineSegments() - 2, GetNumOfLineSegments() - 3), size - 2);
					for (j = 2; j < size - 2; j++) {
						curve.SetPointAt(new LineVertex((Vertex) curve.GetPointAt(j), ei + j - 1, ei + j, ei + j + 1,
								ei + j - 2), j);
					}
				}
			}
		}
		
		// Joining vertices that are closer to each other than joinThreshold
		// JoinVertices() can be overridden to specify the created join vertex types
		// MaintainVertices() should be overridden for new vertex types
		ei = 0;
		for (i = 0; i < setOfCurves.GetNumOfCurves(); i++) {
			curve = setOfCurves.GetCurveAt(i);
			size = curve.getSize();
			for (j = 0; j < size; j++) {
				for (k = 0; k < getSize(); k++)
					if (curve.GetPointAt(j).Dist2(GetPointAt(k)) <= joinThreshold)
						break;
				if (k == getSize()) {
					AddPoint(curve.GetPointAt(j));
				} else {
					JoinVertices(curve, k, j);
				}
				GetVertexAt(k).SetVertexIndexOfEdges(k);
			}
			for (j = 0; j < size - 1; j++, ei++)
				SetEdgeAt(new Edge(this, GetEdgeAt(ei).GetVertexIndex1(), GetEdgeAt(ei).GetVertexIndex2()), ei);
		}
		
		MaintainNeighbors();
		
		oldVertices.Reset();
		for (i = 0; i < getSize(); i++)
			oldVertices.AddPoint(GetPointAt(i));
		for (j = 0; j < sample.GetSize(); j++)
			GetSamplePointAt(j).Initialize();
		
		// Deleting loops and double edges
		for (i = 0; i < GetNumOfLineSegments(); i++) {
			Edge edge1 = GetEdgeAt(i);
			boolean found = false;
			for (j = i + 1; j < GetNumOfLineSegments(); j++) {
				Edge edge2 = GetEdgeAt(j);
				if ((edge1.GetVertexIndex1() == edge2.GetVertexIndex1() && edge1.GetVertexIndex2() == edge2
						.GetVertexIndex2())
						|| (edge1.GetVertexIndex1() == edge2.GetVertexIndex2() && edge1.GetVertexIndex2() == edge2
						.GetVertexIndex1())) {
					found = true;
					break;
				}
			}
			if (found || edge1.GetVertexIndex1() == edge1.GetVertexIndex2()) {
				SetPointAt(edge1.GetVertex1().Degrade(edge1.GetVertex2()), edge1.GetVertexIndex1());
				SetPointAt(edge1.GetVertex2().Degrade(edge1.GetVertex1()), edge1.GetVertexIndex2());
				DeleteEdgeAt(i);
				i--;
			}
		}
		
		// Adding a line vertex to an edge if the two endpoints are both non-LineVertices and non-EndVertices
		for (i = 0; i < GetNumOfLineSegments(); i++) {
			Edge edge = GetEdgeAt(i);
			if (!(edge.GetVertex1() instanceof RegularVertex) && !(edge.GetVertex2() instanceof RegularVertex)) {
				InsertMidPoint(i);
				i--;
			}
		}
	}
	
	void JoinVertices(Curve curve, int k, int j) {
		// /////// Not official /////////////
		// if (GetVertexAt(k) instanceof LineVertex && curve.GetPointAt(j) instanceof LineVertex)
		// SetPointAt(new XVertex((LineVertex)GetVertexAt(k),(LineVertex)curve.GetPointAt(j)),k);
		// else
		// //////////////////////////////////
		// EndVertex + EndVertex = LineVertex
		if (GetVertexAt(k).GetDegree() + ((Vertex) curve.GetPointAt(j)).GetDegree() > 2)
			SetPointAt(new StarOfManyVertex(GetVertexAt(k), (Vertex) curve.GetPointAt(j)), k);
			// EndVertex + EndVertex = LineVertex
		else if (GetVertexAt(k) instanceof EndVertex && curve.GetPointAt(j) instanceof EndVertex) {
			SetPointAt(new LineVertex((EndVertex) GetVertexAt(k), (EndVertex) curve.GetPointAt(j)), k);
		} else
			throw new RuntimeException("NOT KNOWN INITIAL JOIN TYPE!!\n" + curve.GetPointAt(j) + "\n" + GetVertexAt(k)
					+ "\n");
	}
	
	// Maintaining neighbors, so if angle is not penalized at a vertex, it is not penalized
	// either when a neighbor vertex is relocated.
	void MaintainNeighbors() {
		for (int i = 0; i < getSize(); i++) {
			if (GetVertexAt(i) instanceof YVertex)
				GetVertexAt(i).MaintainNeighbors();
		}
	}
	
	final public SetOfCurves ConvertToCurves() {
		if (getSize() == 0)
			return null;
		SetOfCurves setOfCurves = new SetOfCurves();
		for (int i = 0; i < getSize(); i++)
			GetVertexAt(i).label = GetVertexAt(i).GetDegree();
		boolean found = true;
		while (found) {
			found = false;
			int i;
			for (i = 0; i < getSize(); i++) {
				if (!(GetVertexAt(i) instanceof LineVertex) && GetVertexAt(i).label > 0) {
					found = true;
					break;
				}
			}
			if (i < getSize()) { // found a non-LineVertex
				setOfCurves.StartNewCurve();
				Vertex firstVertex = GetVertexAt(i);
				Vertex vertex = firstVertex;
				Vertex[] neighbors = vertex.GetNeighbors();
				Vertex nextVertex = null;
				for (Vertex neighbor : neighbors) {
					if (neighbor.label > 0) {
						nextVertex = neighbor;
						break;
					}
				}
				if (nextVertex == null)
					throw (new ArithmeticException("can't find unscanned path"));
				Vertex tempVertex;
				setOfCurves.AddPoint(vertex);
				vertex.label--;
				while (nextVertex instanceof LineVertex) {
					nextVertex.label = 0;
					setOfCurves.AddPoint(nextVertex);
					tempVertex = vertex;
					vertex = nextVertex;
					nextVertex = ((HasTwoSymmetricEdges) nextVertex).GetOppositeVertex(tempVertex);
				}
				setOfCurves.AddPoint(nextVertex);
				nextVertex.label--;
			} else { // perhaps circle: only LineVertices
				for (i = 0; i < getSize(); i++) {
					if (GetVertexAt(i).label > 0) {
						found = true;
						break;
					}
				}
				if (i < getSize()) {
					setOfCurves.StartNewCurve();
					Vertex firstVertex = GetVertexAt(i);
					Vertex vertex = firstVertex;
					Vertex nextVertex = ((LineVertex) firstVertex).GetEdge1().GetVertex2();
					Vertex tempVertex;
					setOfCurves.AddPoint(vertex);
					vertex.label = 0;
					while (nextVertex != firstVertex) {
						nextVertex.label = 0;
						setOfCurves.AddPoint(nextVertex);
						tempVertex = vertex;
						vertex = nextVertex;
						nextVertex = ((HasTwoSymmetricEdges) nextVertex).GetOppositeVertex(tempVertex);
					}
					setOfCurves.AddPoint(nextVertex);
				}
			}
		}
		return setOfCurves;
	}
	
	final void RepartitionVoronoiRegions() throws IllegalStateException {
//        System.out.println(++s + ":\tBEFORE:\tobjective = " + objective + "\tMSE = " + MSE + "\tpenalty = " + penalty);
		int i, j;
		boolean cont = true;
		double smin, d;
		PrincipalCurveSampleVektor samplePoint;
		double localMSE;
		double maxChange, maxDistance;
		
		while (cont) {
			cont = false;
			
			// For efficiency
			LineSegmentAbstract[] lineSegments = new LineSegmentAbstract[GetNumOfLineSegments()];
			for (i = 0; i < GetNumOfLineSegments(); i++)
				lineSegments[i] = (LineSegmentAbstract) GetLineSegmentAt(i);
			
			// maxChange is the maximum change of position over all vertices of the curve;
			maxChange = 0;
			for (j = 0; j < getSize(); j++) {
				if ((d = GetPointAt(j).Dist2(oldVertices.GetPointAt(j))) > maxChange)
					maxChange = d;
			}
			
			maxDistance = 2 * sample.GetRadius();
			MSE = 0.0;
			
			for (j = 0; j < sample.GetSize(); j++) {
				samplePoint = GetSamplePointAt(j);
				smin = samplePoint.GetDist2FromNearestSegment(maxChange, maxDistance, lineSegments);
				// The edge takes care to place the sample point in the right set
				localMSE = samplePoint.GetWeight() * GetEdgeAt(samplePoint.GetIndexOfNearestSegment()).AddPointToSet(samplePoint, smin);
				MSE += localMSE;
			}
			
			for (i = 0; i < GetNumOfLineSegments(); i++)
				GetEdgeAt(i).DeleteMovedPointsFromSet();
			for (i = 0; i < getSize(); i++)
				GetVertexAt(i).DeleteMovedPointsFromSet();
			
			// Calculate total MSE
			MSE /= sample.GetWeight();
			
			// Filter empty vertexes
			for (i = 0; i < getSize(); i++) {
				// if the vertex and the adjacent segments have no data points in their nearest neighbor cells, delete
				// the vertex
				if (GetVertexAt(i).EmptySet()) {
//                    System.out.println("deleted " + GetVertexAt(i));
					vertexDeletionCount += 1;
					if (vertexDeletionCount >= 10) {
						LOG.debug("Vertex deletes more than 10 times.");
					}
					cont = true;
					try {
						DeleteLineVertexAt(i);
					} catch (ClassCastException e) {
						DeletePointAt(i);
					}
					break;
				}
			}
			oldVertices = Clone();
		}
		
		PenaltyCoefficients.ANGLE_PENALTY_COEFFICIENT =
				anglePenaltyCoefficient(GetNumOfLineSegments(), sample.GetWeight(), MSE) * sample.GetRadius()
						* sample.GetRadius();
		PenaltyCoefficients.LENGTH_PENALTY_COEFFICIENT =
				principalCurveParameters.relativeLengthPenaltyCoefficient
						* anglePenaltyCoefficient(GetNumOfLineSegments(), sample.GetWeight(), MSE);
		PenaltyCoefficients.WEIGHT_DIFFERENCE_PENALTY_COEFFICIENT = 0.01;
		
		objective = MSE + penalty;
		// System.out.println("\tAFTER:\tobjective = " + objective + "\tMSE = " + MSE + "\tpenalty = " + penalty);
		/*
		 * principalCurveParameters.diagnosisTextArea.append(algorithm.mapinference.lineclustering.pcurves.Utilities.Misc.FormatString(
		 * objective,20,1)
		 * + algorithm.mapinference.lineclustering.pcurves.Utilities.Misc.FormatString(MSE,20,1)
		 * + algorithm.mapinference.lineclustering.pcurves.Utilities.Misc.FormatString(penalty,20,1)
		 * + "\n");
		 */
	}
	
	private double anglePenaltyCoefficient(int k, double weight, double MSE) {
		return (principalCurveParameters.penaltyCoefficient * 1 / Math.pow(weight, 1.0 / 3.0) * Math.sqrt(MSE) / sample
				.GetRadius());
	}
	
	// Find edge with the most projection points. In case of a tie, the longer segment is chosen.
	final public boolean AddOneVertexAsMidpoint(boolean addAnyway) {
		int i;
		double s;
		
		double max = GetEdgeAt(0).GetSetWeight() + (GetEdgeAt(0).GetVertex1().GetSetWeight() + GetEdgeAt(0).GetVertex2().GetSetWeight()) / 2;
		int maxIndex = 0;
		double maxLength = GetLineSegmentAt(0).GetLength();
		for (i = 1; i < GetNumOfLineSegments(); i++) {
			s = GetEdgeAt(i).GetSetWeight() + (GetEdgeAt(i).GetVertex1().GetSetWeight() + GetEdgeAt(i).GetVertex2().GetSetWeight()) / 2;
			if (s == max) {
				if (GetLineSegmentAt(i).GetLength() > maxLength) {
					maxIndex = i;
					maxLength = GetLineSegmentAt(i).GetLength();
				}
			} else if (s > max) {
				max = s;
				maxIndex = i;
				maxLength = GetLineSegmentAt(i).GetLength();
			}
		}
		boolean cont = !terminatingCondition(GetEdgeAt(maxIndex).GetSetWeight());
		if (addAnyway || cont) {
			InsertMidPoint(maxIndex);
		}
		return cont;
	}
	
	// Find the longest edge
	final public boolean AddOneVertexAsMidpointOfLongestSegment(boolean addAnyway) {
		int i;
		double l;
		int maxIndex = 0;
		double maxLength = GetLineSegmentAt(0).GetLength();
		for (i = 1; i < GetNumOfLineSegments(); i++) {
			l = GetLineSegmentAt(i).GetLength();
			if (l > maxLength) {
				maxLength = l;
				maxIndex = i;
			}
		}
		boolean cont = !terminatingConditionOnLength(maxLength);
		if (addAnyway || cont) {
			InsertMidPoint(maxIndex);
		}
		return cont;
	}
	
	// Add midpoints to all edges
	final public boolean AddVerticesAsMidpoints(boolean addAnyway) {
		boolean cont = false;
		int k = GetNumOfLineSegments();
		for (int i = 0; i < k; i++) {
			if (!terminatingCondition(GetEdgeAt(i).GetSetWeight())) {
				cont = true;
				InsertMidPoint(i);
			}
		}
		return cont;
	}
	
	final void InsertMidPoint(int oei) { // oei = oldEdgeIndex
		DummyVertex midPoint = new DummyVertex(GetLineSegmentAt(oei).GetMidVektor());
		
		int nvi = getSize();// newVertexIndex
		int ovi1 = GetEdgeAt(oei).GetVertexIndex1(); // oldVertexIndex1
		int ovi2 = GetEdgeAt(oei).GetVertexIndex2(); // oldVertexIndex2
		int nei1 = GetNumOfLineSegments(); // newEdgeIndex1
		int nei2 = oei; // newEdgeIndex2
		
		AddPoint(midPoint);
		// From the aspect of repartitioning, the new vertex does not play any part
		oldVertices.AddPoint(midPoint);
		
		Edge oldEdge = GetEdgeAt(oei);
		AddEdge(new Edge(this, ovi1, nvi));
		SetEdgeAt(new Edge(this, nvi, ovi2), nei2);
		
		// For efficiency, we place the sample points from the nearest neighbor regions
		// of the old edge to the nearest neighbor set of one of the new edges.
		LineSegmentAbstract[] lineSegments = new LineSegmentAbstract[GetNumOfLineSegments()];
		for (int i = 0; i < GetNumOfLineSegments(); i++)
			lineSegments[i] = (LineSegmentAbstract) GetLineSegmentAt(i);
		double smin;
		PrincipalCurveSampleVektor samplePoint;
		for (int i = 0; i < oldEdge.GetSetSize(); i++) {
			samplePoint = oldEdge.GetSetPointAt(i);
			smin = samplePoint.GetDist2FromNearestSegment(nei1, nei2, lineSegments);
			GetEdgeAt(samplePoint.GetIndexOfNearestSegment()).AddPointToSet(samplePoint, smin);
		}
		
		// Since we don't use oldEdge any more, we don't need to DeleteMovedPointsFromSet()
		
		Vertex oldVertex = GetVertexAt(ovi1);
		for (int i = 0; i < oldVertex.GetSetSize(); i++)
			oldVertex.GetSetPointAt(i).SetIndexOfNearestSegment(nei1);
		oldVertex = GetVertexAt(ovi2);
		for (int i = 0; i < oldVertex.GetSetSize(); i++)
			oldVertex.GetSetPointAt(i).SetIndexOfNearestSegment(nei2);
		
		for (int i = 0; i < getSize(); i++)
			GetVertexAt(i).ReplaceEdgeIndexesForInsertion(oei, nei1, nei2);
		
		Vertex vertex2 = GetEdgeAt(nei1).GetVertex1();
		Vertex vertex1 = GetEdgeAt(nei2).GetVertex2();
		
		SetPointAt(new LineVertex(GetVertexAt(nvi), vertex1, vertex2), nvi);
	}
	
	final void InsertFirstTwoPoints(Vektor p1, Vektor p2) {
		Reset();
		AddPoint(new DummyVertex(p1));
		AddPoint(new DummyVertex(p2));
		AddEdge(new Edge(this, 0, 1));
		SetPointAt(new EndVertex(GetVertexAt(0), 0), 0);
		SetPointAt(new EndVertex(GetVertexAt(1), 0), 1);
		
		double localMSE;
		MSE = 0;
		PrincipalCurveSampleVektor samplePoint;
		for (int j = 0; j < sample.GetSize(); j++) {
			samplePoint = GetSamplePointAt(j);
			samplePoint.Initialize();
			localMSE = GetEdgeAt(0).AddPointToSet(samplePoint, samplePoint.Dist2(GetEdgeAt(0)));
			MSE += localMSE;
		}
		// Since this is the first partitioning, we don't need to DeleteMovedPointsFromSet()
		
		// Calculate total MSE
		MSE /= sample.GetWeight();
		oldVertices = Clone();
	}
	
	// Delete a LineVertex, delete the two adjacent edges, and add an edge connecting the neighbor vertices.
	// The neighbor vertices are Maintained.
	void DeleteLineVertexAt(int ovi) {
		LineVertex oldVertex = (LineVertex) GetPointAt(ovi);
		int oei1 = oldVertex.GetEdgeIndex1();
		int oei2 = oldVertex.GetEdgeIndex2();
		int nvi1 = oldVertex.GetEdge1().GetVertexIndex2();
		int nvi2 = oldVertex.GetEdge2().GetVertexIndex2();
		int nei = GetNumOfLineSegments();
		AddEdge(new Edge(this, nvi1, nvi2));
		
		for (int i = 0; i < oldVertex.GetSetSize(); i++)
			oldVertex.GetSetPointAt(i).SetIndexOfNearestSegment(nei);
		for (int i = 0; i < oldVertex.GetEdge1().GetSetSize(); i++)
			oldVertex.GetEdge1().GetSetPointAt(i).SetIndexOfNearestSegment(nei);
		for (int i = 0; i < oldVertex.GetEdge2().GetSetSize(); i++)
			oldVertex.GetEdge2().GetSetPointAt(i).SetIndexOfNearestSegment(nei);
		for (int i = 0; i < GetVertexAt(nvi1).GetSetSize(); i++)
			GetVertexAt(nvi1).GetSetPointAt(i).SetIndexOfNearestSegment(nei);
		for (int i = 0; i < GetVertexAt(nvi2).GetSetSize(); i++)
			GetVertexAt(nvi2).GetSetPointAt(i).SetIndexOfNearestSegment(nei);
		
		for (int i = 0; i < getSize(); i++) {
			if (i != ovi) {
				if (i != nvi2)
					GetVertexAt(i).ReplaceEdgeIndexesForDeletion(oei1, GetNumOfLineSegments() - 1);
				if (i != nvi1)
					GetVertexAt(i).ReplaceEdgeIndexesForDeletion(oei2, GetNumOfLineSegments() - 1);
			}
		}
		
		GetVertexAt(nvi1).Maintain(GetVertexAt(nvi2));
		GetVertexAt(nvi2).Maintain(GetVertexAt(nvi1));
		
		super.DeletePointAt(ovi);
		for (int i = 0; i < GetNumOfLineSegments(); i++) {
			GetEdgeAt(i).DecrementPointIndexes(ovi);
		}
		
		DeleteEdgeAt(Math.max(oei1, oei2));
		DeleteEdgeAt(Math.min(oei1, oei2));
		
		// From the aspect of repartitioning, deleting a LineVertex is like replacing
		// the vertex with its projection point to the new edge.
		oldVertices.UpdatePointAt(oldVertex.Project(GetEdgeAt(GetNumOfLineSegments() - 1)), ovi);
	}
	
	// Delete adjacent edges, delete the vertex, and Degrade the neighbor vertices.
	@Override
	final public void DeletePointAt(int oldVertexIndex) throws IllegalStateException {
		Vertex oldVertex = GetVertexAt(oldVertexIndex);
		int[] newVertexIndices = oldVertex.GetNeighborIndexes();
		int[] oldEdgeIndices = oldVertex.GetEdgeIndexes();
		boolean[] vertexIsSingular = new boolean[oldVertex.GetDegree()];
		// Degrading neighbors
		for (int l = 0; l < newVertexIndices.length; l++) {
			try {
				SetPointAt(GetVertexAt(newVertexIndices[l]).Degrade(oldVertex), newVertexIndices[l]);
				vertexIsSingular[l] = false;
			} catch (ArithmeticException e) { // "CAN'T DEGRADE AN EndVertex"
				SetPointAt(new DummyVertex(GetVertexAt(newVertexIndices[l])), newVertexIndices[l]);
				vertexIsSingular[l] = true;
			}
		}
		
		// Resetting indices of nearest segments to -1, so distances are recalculated in RepartitionVoronoiRegions()
		for (int i = 0; i < oldVertex.GetSetSize(); i++)
			oldVertex.GetSetPointAt(i).SetIndexOfNearestSegment(-1);
		for (int oldEdgeIndice : oldEdgeIndices)
			for (int i = 0; i < GetEdgeAt(oldEdgeIndice).GetSetSize(); i++)
				GetEdgeAt(oldEdgeIndice).GetSetPointAt(i).SetIndexOfNearestSegment(-1);
		for (int newVertexIndice : newVertexIndices)
			for (int i = 0; i < GetVertexAt(newVertexIndice).GetSetSize(); i++)
				GetVertexAt(newVertexIndice).GetSetPointAt(i).SetIndexOfNearestSegment(-1);
		
		// Deleting the vertex and adjusting vertex indices of edges
		super.DeletePointAt(oldVertexIndex);
		for (int i = 0; i < GetNumOfLineSegments(); i++) {
			GetEdgeAt(i).DecrementPointIndexes(oldVertexIndex);
		}
		
		// "Calling DecrementPointIndexes(oldVertexIndex) for newVertexIndices"
		for (int l = 0; l < newVertexIndices.length; l++)
			if (oldVertexIndex < newVertexIndices[l])
				newVertexIndices[l]--;
		
		// Deleting the incident edges in decreasing order of their indexes
		MyMath.sortInsertion(oldEdgeIndices);
		for (int l = 0; l < newVertexIndices.length; l++)
			DeleteEdgeAt(oldEdgeIndices[l]);
		
		// Deleting singular neighbors
		for (int l = 0; l < newVertexIndices.length; l++) {
			if (vertexIsSingular[l]) {
				// Deleting the vertex and adjusting vertex indices of edges
				super.DeletePointAt(newVertexIndices[l]);
				for (int i = 0; i < GetNumOfLineSegments(); i++) {
					GetEdgeAt(i).DecrementPointIndexes(newVertexIndices[l]);
				}
				// "Calling DecrementPointIndexes(newVertexIndices[l]) for newVertexIndices"
				for (int l1 = l + 1; l1 < newVertexIndices.length; l1++)
					if (newVertexIndices[l] < newVertexIndices[l1])
						newVertexIndices[l1]--;
			}
		}
		if (getSize() == 0)
			throw new IllegalStateException("WARNING! The current principal curve is deleted. " + sample.GetSize() + " points are ignored.");
	}
	
	final void AddEdge(Edge edge) {
		edges.add(edge);
	}
	
	private void SetEdgeAt(Edge edge, int index) {
		edges.set(index, edge);
	}
	
	final void DeleteEdgeAt(int oei) {
		edges.remove(oei);
		for (int i = 0; i < getSize(); i++)
			GetVertexAt(i).DecrementEdgeIndexes(oei);
		for (int i = 0; i < sample.GetSize(); i++)
			GetSamplePointAt(i).DecrementEdgeIndexes(oei);
	}
	
	private boolean terminatingCondition(double setWeight) {
		return (setWeight < 2 || setWeight < (1.0 / principalCurveParameters.terminatingConditionCoefficient)
				* Math.pow(sample.GetWeight(), 2.0 / 3.0) * Math.sqrt(MSE) / sample.GetRadius()
				&& GetNumOfLineSegments() > 3);
	}
	
	private boolean terminatingConditionOnLength(double maxLength) {
		return (maxLength < principalCurveParameters.terminatingConditionMaxLength);
	}
	
	final public void SetSteepestDescentDirections() {
		steepestDescentDirections = new Vektor[GetNumOfVertexes()];
		for (int i = 0; i < GetNumOfVertexes(); i++)
			steepestDescentDirections[i] = GetVertexAt(i).NewVertex().Sub(GetVertexAt(i));
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Optimizable BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public synchronized void OptimizingStep(double step) {
		for (int i = 0; i < GetNumOfVertexes(); i++) {
			GetVertexAt(i).AddEqual(steepestDescentDirections[i].Mul(step));
		}
	}
	
	@Override
	final public synchronized double GetCriterion() {
		MSE = 0;
		penalty = 0;
		int n = GetNumOfVertexes();
		int k = GetNumOfLineSegments();
		for (int i = 0; i < n; i++) {
			MSE += GetVertexAt(i).GetMSETimesWeight();
			penalty += GetVertexAt(i).GetPenalty();
		}
		for (int i = 0; i < k; i++)
			MSE += GetEdgeAt(i).GetMSETimesWeight();
		MSE /= sample.GetWeight();
		return (objective = MSE + penalty);
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// interface Optimizable END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	final public String toString() {
		String s = "";
		for (int i = 0; i < getSize(); i++) {
			s += "point[" + i + "]:\t";
			s += GetPointAt(i).toString();
			s += "\n";
		}
		
		for (int i = 0; i < GetNumOfLineSegments(); i++) {
			s += "edge[" + i + "]:\t";
			s += GetEdgeAt(i).toString();
			s += "\n";
		}
		return s;
	}
	
	final class PrincipalCurveSample extends SampleWithOnlineStatistics {
		OnlineSampleStatisticsDegreeOne statisticsDegreeOne;
		double radius;
		
		public PrincipalCurveSample(Sample in_sample) {
			super(1);
			this.sample = new Sample();
			PrincipalCurveSampleVektor.prototypeVektor = in_sample.GetPointAt(0).DefaultClone();
			statisticsDegreeOne = new OnlineSampleStatisticsDegreeOne(PrincipalCurveSampleVektor.prototypeVektor);
			statistics[0] = statisticsDegreeOne;
			for (int i = 0; i < in_sample.getSize(); i++)
				AddPoint(new PrincipalCurveSampleVektor(in_sample.GetPointAt(i)));
			radius = 0;
			double d;
			for (int i = 0; i < GetSize(); i++) {
				if ((d = statisticsDegreeOne.GetCenter().Dist2(this.sample.GetPointAt(i))) > radius)
					radius = d;
			}
		}
		
		final public PrincipalCurveSampleVektor GetSamplePointAt(int index) {
			return (PrincipalCurveSampleVektor) (this.sample.GetPointAt(index));
		}
		
		final public LineObject FirstPrincipalComponentSmallSample(int maxpoints, int randomSeed) {
			SampleWithOnlineStatisticsDegreeTwo smallSample = new SampleWithOnlineStatisticsDegreeTwo(this.sample.RandomSample(maxpoints, randomSeed));
			return smallSample.FirstPrincipalComponent();
		}
		
		final public double GetRadius() {
			return radius;
		}
		
		final public double GetWeight() {
			return statisticsDegreeOne.GetWeight();
		}
	}
}
