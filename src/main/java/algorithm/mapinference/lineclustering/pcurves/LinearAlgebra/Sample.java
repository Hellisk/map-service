package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Sample {
	protected List<Vektor> points;
	
	public Sample() {
		InitializeSampleParameters(100);
	}
	
	public Sample(int initSize) {
		InitializeSampleParameters(initSize);
	}
	
	private void InitializeSampleParameters(int initSize) {
		points = new ArrayList<>(initSize);
	}
	
	public Sample Clone() {
		Sample sample = new Sample(getSize());
		for (int i = 0; i < getSize(); i++)
			sample.AddPoint(GetPointAt(i).Clone());
		return sample;
	}
	
	public Sample ShallowClone() {
		Sample sample = new Sample(getSize());
		for (int i = 0; i < getSize(); i++)
			sample.AddPoint(GetPointAt(i));
		return sample;
	}
	
	public Sample DefaultClone() {
		return new Sample();
	}
	
	final public Vektor GetPointAt(int i) {
		try {
			return points.get(i);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: i = " + i + " size = " + getSize() + "\n");
		}
	}
	
	final public int FindPoint(Vektor vektor) {
		return points.indexOf(vektor);
	}
	
	final public int getSize() {
		return points.size();
	}
	
	@Override
	public String toString() {
		String string = "Size = " + getSize() + "\n";
		for (int i = 0; i < getSize(); i++)
			string += i + ": " + GetPointAt(i).toString() + "\n";
		return string;
	}
	
	public void Reset() {
		points.clear();
	}
	
	final public void Add(Sample sample) {
		for (int i = 0; i < sample.getSize(); i++)
			AddPoint(sample.GetPointAt(i));
	}
	
	final public void AddPoint(Vektor vektor) {
		InsertPointAt(vektor, getSize());
	}
	
	public void InsertPointAt(Vektor vektor, int index) {
		try {
			points.add(index, vektor);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: index = " + index + " size = " + getSize() + "\n");
		}
	}
	
	public void DeletePointAt(int index) {
		try {
			points.remove(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: index = " + index + " size = " + getSize() + "\n");
		}
	}
	
	// Batch deleting
	public void DeletePoints(boolean[] toBeDeleted) {
		int j = 0;
		for (int i = 0; i < getSize(); i++) {
			if (!toBeDeleted[i]) {
				points.set(j, points.get(i));
				j++;
			}
		}
		// points.setSize(j);
		points.subList(j, getSize()).clear();
	}
	
	public void UpdatePointAt(Vektor vektor, int i) {
		GetPointAt(i).Update(vektor);
	}
	
	public void SetPointAt(Vektor vektor, int index) {
		try {
			points.set(index, vektor);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Myerror: index = " + index + " size = " + getSize() + "\n");
		}
	}
	
	// Creating smaller random sample with min(maxpoints,size) points
	final public Sample RandomSample(int maxpoints, int randomSeed) {
		Random random = new Random(randomSeed);
		Sample random_sample = DefaultClone();
		for (int i = 0; i < getSize(); i++)
			if (random.nextDouble() < (double) (maxpoints - random_sample.getSize()) / (getSize() - i))
				random_sample.AddPoint(GetPointAt(i));
		return random_sample;
	}
	
	final public Sample GetProjectionResiduals(Line line) {
		Sample residuals = DefaultClone();
		for (int i = 0; i < getSize(); i++) {
			Vektor vektor = GetPointAt(i);
			Vektor projection = vektor.Project(line);
			residuals.AddPoint(vektor.Sub(projection));
		}
		return residuals;
	}
	
	public void Save(String fileName) {
		try {
			FileOutputStream fOut = new FileOutputStream(fileName);
			PrintStream pOut = new PrintStream(fOut);
			for (int i = 0; i < getSize(); i++) {
				GetPointAt(i).Save(pOut);
				pOut.println();
			}
			pOut.close();
			fOut.close();
		} catch (IOException e) {
			System.out.println("Can't open file " + fileName);
		}
	}
	
	public String SaveToString() {
		String str = new String();
		for (int i = 0; i < getSize(); i++) {
			str += GetPointAt(i).SaveToString() + "\n";
		}
		return str;
	}
	
	public void AddEqual(Vektor vektor) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).AddEqual(vektor);
	}
	
	public void SubEqual(Vektor vektor) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).SubEqual(vektor);
	}
	
	public void MulEqual(double d) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).MulEqual(d);
	}
	
	public void DivEqual(double d) {
		for (int i = 0; i < getSize(); i++)
			GetPointAt(i).DivEqual(d);
	}
}
