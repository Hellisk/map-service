package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

import algorithm.mapinference.lineclustering.pcurves.Debug.Debug;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface Loadable {
	public void Load(File f, Debug d) throws FileNotFoundException, IOException;
	
	public void Load(File f) throws FileNotFoundException, IOException;
	
	public void Load(DataInputStream din, Debug d) throws IOException;
	
	public void Load(DataInputStream din) throws IOException;
}
