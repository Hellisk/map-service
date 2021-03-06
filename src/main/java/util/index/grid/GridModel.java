package util.index.grid;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.index.SpatialIndexModel;
import util.object.spatialobject.Rect;
import util.object.spatialobject.SpatialObject;

import java.util.HashSet;

/**
 * A static grid index model made of n x m cells.
 * <p>
 * Grid are statically constructed spatial models.
 * <p>
 * The grid is constructed from left to right, bottom up. The first position in the grid is the position index zero 0=[i,j]=[0,0].
 *
 * @author uqdalves, Hellisk
 */
public class GridModel implements SpatialIndexModel {
	
	private static final Logger LOG = Logger.getLogger(GridModel.class);
	
	/**
	 * The grid cells matrix
	 */
	private final Rect[][] grid;
	
	/**
	 * The boundaries of this grid diagram
	 */
	private final Rect boundary;
	
	/**
	 * Grid dimensions (number of cells in each axis)
	 */
	private final int sizeX;
	private final int sizeY;
	
	/**
	 * Create a new static grid model of (n x m) cells with the given dimensions.
	 *
	 * @param n    The number of horizontal cells (x).
	 * @param m    The number of vertical cells (y).
	 * @param minX Lower-left X coordinate.
	 * @param minY Lower-left Y coordinate.
	 * @param maxX Upper-right X coordinate.
	 * @param maxY Upper-right Y coordinate.
	 */
	GridModel(int n, int m, double minX, double minY, double maxX, double maxY, DistanceFunction df) {
		if (n <= 0 || m <= 0) {
			throw new IllegalArgumentException("Grid dimensions " + "must be positive.");
		}
		this.grid = new Rect[n][m];
		this.boundary = new Rect(minX, minY, maxX, maxY, df);
		this.sizeX = n;
		this.sizeY = m;
		// build the grid
		build(df);
	}
	
	/**
	 * Build the model.
	 * Generates the grid cells.
	 */
	private void build(DistanceFunction df) {
		// axis increments
		double incrX = (boundary.maxX() - boundary.minX()) / sizeX;
		double incrY = (boundary.maxY() - boundary.minY()) / sizeY;
		double currentX, currentY;
		String cellId;
		currentY = boundary.minY();
		for (int y = 0; y < sizeY; y++) {
			currentX = boundary.minX();
			for (int x = 0; x < sizeX; x++) {
				cellId = getIndexString(x, y);
				Rect cell = new Rect(currentX, currentY, currentX + incrX, currentY + incrY, df);
				cell.setID(cellId);
				grid[x][y] = cell;
				currentX += incrX;
			}
			currentY += incrY;
		}
	}
	
	/**
	 * @return Number of horizontal cells (parallel to the X axis).
	 */
	public int sizeX() {
		return this.sizeX;
	}
	
	/**
	 * @return Number of vertical cells (parallel to the Y axis).
	 */
	public int sizeY() {
		return this.sizeY;
	}
	
	@Override
	public int size() {
		return (sizeX * sizeY);
	}
	
	@Override
	public boolean isEmpty() {
		if (grid == null) return true;
		return (sizeX == 0 || sizeY == 0);
	}
	
	/**
	 * @return The height (vertical axis) of the cells
	 * in this grid.
	 */
	public double cellsHeight() {
		return (boundary.maxY() - boundary.minY()) / sizeY;
	}
	
	/**
	 * @return The width (horizontal axis) of the cells
	 * in this grid.
	 */
	public double cellsWidth() {
		return (boundary.maxX() - boundary.minX()) / sizeX;
	}
	
	/**
	 * @return Return the list of grid cells in this model.
	 */
	public Rect[][] getCells() {
		return grid;
	}
	
	/**
	 * Return the cell in the position [i,j] of the grid. Grid i and j position starts from [0,0].
	 *
	 * @param i cell position in the horizontal axis (x).
	 * @param j cell position in the vertical axis (y).
	 * @return The cell in the position [i,j] of the grid.
	 */
	public Rect get(int i, int j) {
		if (i < 0 || i >= sizeX || j < 0 && j >= sizeY) {
			throw new IndexOutOfBoundsException("Grid index out of bounds.");
		}
		return grid[i][j];
	}
	
	@Override
	public Rect get(final String index) {
		if (index == null || index.isEmpty()) {
			throw new IllegalArgumentException("Grid index must not be empty.");
		}
		int[] pos = getCellPosition(index);
		return get(pos[0], pos[1]);
	}
	
	/**
	 * Return the position [i,j] in the grid of the cell with the given index.
	 * <br> [0] = i cell position in the horizontal axis (x).
	 * <br> [1] = j cell position in the vertical axis (y).
	 *
	 * @param index The index of the cell to search.
	 * @return The position [i,j] in the grid of the cell with the given index.
	 */
	public int[] getCellPosition(final String index) {
		try {
			// get cell position
			String[] pos = index.replaceAll("i|j", "").split("-");
			int i = Integer.parseInt(pos[0]);
			int j = Integer.parseInt(pos[1]);
			return new int[]{i, j};
		} catch (Exception e) {
			throw new IllegalArgumentException("Grid index is invalid.");
		}
	}
	
	@Override
	public Rect getBoundary() {
		return boundary;
	}
	
	@Override
	public String search(double x, double y) {
		if (!boundary.contains(x, y)) {
			LOG.error("ERROR! The specific location is not included in the map: " + x + "," + y);
			return null; // didn't find anything
		}
		double width = cellsWidth();
		double height = cellsHeight();
		int i = (int) ((x - boundary.minX()) / width);
		int j = (int) ((y - boundary.minY()) / height);
		if ((x != boundary.minX() && (x - boundary.minX()) % width == 0) || x == boundary.maxX()) i--;
		if ((y != boundary.minY() && (y - boundary.minY()) % height == 0) || y == boundary.maxY()) j--;
		
		return getIndexString(i, j);
	}
	
	@Override
	public HashSet<String> rangeSearch(SpatialObject obj) {
		HashSet<String> posList = new HashSet<>();
		// object is not in this grid
		if (!boundary.intersects(obj)) return posList;
		// search the grid cells
		for (int i = 0; i < sizeX; i++) {
			for (int j = 0; j < sizeY; j++) {
				if (grid[i][j].intersects(obj)) {
					posList.add(grid[i][j].getID());
				}
			}
		}
		return posList;
	}
	
	/**
	 * Return the indexes of all cells adjacent from the cell containing the object in the (x,y) position (if any).
	 *
	 * @param x Object's X coordinate.
	 * @param y Object's Y coordinate.
	 * @return A set of adjacent cells IDs, or an empty set if the object is out of the boundaries of this index model, or if there is no
	 * adjacent cells.
	 */
	public HashSet<String> adjacentSearch(double x, double y) {
		String index = search(x, y);
		return adjacentSearch(index);
	}
	
	/**
	 * Return the indexes of all cells adjacent from the cell with the given index.
	 *
	 * @param index
	 * @return A set of adjacent cells IDs, or an empty set
	 * if the cell has no adjacent cells.
	 */
	public HashSet<String> adjacentSearch(final String index) {
		if (index == null || index.isEmpty()) {
//            return new HashSet<>();
			throw new IllegalArgumentException("Grid index must not be empty.");
		}
		int[] pos = getCellPosition(index);
		return adjacentSearch(pos[0], pos[1]);
	}
	
	/**
	 * Return the index of the adjacent cells from the given grid position [i,j] (if any).
	 *
	 * @param i cell position in the horizontal axis (x).
	 * @param j cell position in the vertical axis (y).
	 * @return An adjacent cells index set, or an empty set if the cell has no adjacent cells.
	 */
	public HashSet<String> adjacentSearch(final int i, final int j) {
		if (i < 0 || i >= sizeX || j < 0 && j >= sizeY) {
			throw new IndexOutOfBoundsException("Grid index out of bounds.");
		}
		HashSet<String> posList = new HashSet<>();
		int adjX, adjY;
		
		adjX = i - 1;
		adjY = j - 1;
		if (adjX >= 0 && adjY >= 0) {
			posList.add(getIndexString(adjX, adjY));
		}
		adjX = i;
		adjY = j - 1;
		if (adjY >= 0) {
			posList.add(getIndexString(adjX, adjY));
		}
		adjX = i + 1;
		adjY = j - 1;
		if (adjX < sizeX && adjY >= 0) {
			posList.add(getIndexString(adjX, adjY));
		}
		adjX = i - 1;
		adjY = j;
		if (adjX >= 0) {
			posList.add(getIndexString(adjX, adjY));
		}
		adjX = i + 1;
		adjY = j;
		if (adjX < sizeX) {
			posList.add(getIndexString(adjX, adjY));
		}
		adjX = i - 1;
		adjY = j + 1;
		if (adjX >= 0 && adjY < sizeY) {
			posList.add(getIndexString(adjX, adjY));
		}
		adjX = i;
		adjY = j + 1;
		if (adjY < sizeY) {
			posList.add(getIndexString(adjX, adjY));
		}
		adjX = i + 1;
		adjY = j + 1;
		if (adjX < sizeX && adjY < sizeY) {
			posList.add(getIndexString(adjX, adjY));
		}
		
		return posList;
	}
	
	@Override
	public void print() {
		LOG.info("[GRID] [" + sizeX + " x " + sizeY + "]");
		Rect cell;
		for (int j = sizeY - 1; j >= 0; j--) {
			for (int i = 0; i < sizeX; i++) {
				cell = grid[i][j];
				System.out.format("[(%.2f,%.2f)(%.2f,%.2f)] ",
						cell.minX(), cell.maxY(),
						cell.maxX(), cell.maxY());
			}
			for (int i = 0; i < sizeX; i++) {
				cell = grid[i][j];
				System.out.format("[(%.2f,%.2f)(%.2f,%.2f)] ",
						cell.minX(), cell.minY(),
						cell.maxX(), cell.minY());
			}
		}
	}
	
	/**
	 * Generates the cell index.
	 *
	 * @param i cell position in the horizontal axis (x).
	 * @param j cell position in the vertical axis (y).
	 * @return The index of the cell in the position [i,j] as a String "i-j".
	 */
	private String getIndexString(final int i, final int j) {
		return ("i" + i + "-" + "j" + j);
	}
}
