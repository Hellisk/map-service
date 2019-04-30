package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;


public interface Line {
	public Vektor GetVektor1();
	
	public Vektor GetVektor2();
	
	public Vektor GetDirectionalVektor();
	
	public Vektor Reflect(Vektor vektor);
}
