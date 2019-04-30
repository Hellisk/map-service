package algorithm.mapinference.lineclustering.pcurves.Internet;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

final public class DownLoadFile {
	DataInputStream din;
	boolean opened;
	
	// Sometimes downloaded files start with an additional header, applications should be
	// aware of it.
	public DownLoadFile(String urlString) throws IOException, MalformedURLException {
		try {
			try {
				URL url = new URL(urlString);
				InputStream in = url.openConnection().getInputStream();
				din = new DataInputStream(in);
				opened = true;
			} catch (IOException e) {
				opened = false;
				throw e;
			}
		} catch (MalformedURLException e) {
			opened = false;
			throw e;
		}
	}
	
	public static void main(String[] args) {
		try {
			DownLoadFile downLoadFile = new DownLoadFile("http://index.hu/politika/belhirek");
			if (downLoadFile.Opened()) {
				try {
					while (downLoadFile.GetDataInputStream().available() > 0) {
						@SuppressWarnings("deprecation")
						String line = downLoadFile.GetDataInputStream().readLine();
						System.out.println(line);
						// if (line.equals("      <P><FONT FACE=\"Arial, Helvetica, sans-serif\"><FONT SIZE=-1>")) {
						// write = true;
						// line = downLoadFile.GetDataInputStream().readLine();
						// System.out.println("<P>");
						// if (first) {
						// first = false;
						// System.out.println("Cikk " + page);
						// System.out.println("<P>");
						// }
						// }
						// if (write && line.equals("       </FONT></FONT>&nbsp;"))
						// write = false;
						// if (write)
						// System.out.println(line);
					}
				} catch (IOException e) {
					System.err.println("Something is wrong");
				}
			}
		} catch (IOException e) {
			System.err.println("Can't open http://index.hu/politika/belhirek (no connection)");
		}
	}
	
	final public DataInputStream GetDataInputStream() {
		return din;
	}
	
	final public void Close() throws IOException {
		if (Opened()) {
			din.close();
			opened = false;
		}
	}
	
	final public boolean Opened() {
		return opened;
	}
	
	@SuppressWarnings("deprecation")
	final public void IgnoreLines(int n) throws IOException {
		for (int i = 0; i < n; i++)
			din.readLine();
	}
	
	final public void Save(String filename) throws IOException {
		FileOutputStream fOut = new FileOutputStream(filename);
		PrintStream pOut = new PrintStream(fOut);
		byte b;
		boolean cont = true;
		while (cont) {
			try {
				b = din.readByte();
				pOut.print((char) b);
			} catch (EOFException e) {
				cont = false;
			}
		}
		pOut.close();
		fOut.close();
	}
}
