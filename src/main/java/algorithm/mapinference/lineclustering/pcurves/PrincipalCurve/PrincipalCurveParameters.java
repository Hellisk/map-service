package algorithm.mapinference.lineclustering.pcurves.PrincipalCurve;

import java.awt.*;

final public class PrincipalCurveParameters {
	public static final int ADD_ONE_VERTEX = 0;
	public static final int ADD_VERTICES = 1;
	public static final int ADD_ONE_VERTEX_TO_LONGEST = 2;
	public double penaltyCoefficient = 0.09;
	public double relativeLengthPenaltyCoefficient = 0.01;
	public double terminatingConditionCoefficient = 0.5;
	public double terminatingConditionMaxLength = 47.16;
	// public double penaltyCoefficient = 0.0015; //for rivers
	public double relativeChangeInCriterionThreshold = 0.003;
	public int addVertexMode = ADD_ONE_VERTEX;
	public boolean paintProjectionPoints = false;
	public TextArea diagnosisTextArea;
	
}
