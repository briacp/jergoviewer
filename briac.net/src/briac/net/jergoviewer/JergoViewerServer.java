package briac.net.jergoviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class JergoViewerServer extends NanoHTTPD {

	private JergoViewer jv;

	private static final Map<String, String> mimeTypes = new HashMap<String, String>();
	static
	{
		mimeTypes.put("css",  "text/css");
		mimeTypes.put("js",   "text/javascript");
		mimeTypes.put("json", "application/json");
		mimeTypes.put("html", "text/html");
		mimeTypes.put("png",  "image/png");
		mimeTypes.put("kml",  "application/vnd.google-earth.kml+xml ");
	}

	public static void main(String[] args) throws IOException {
		JergoViewerServer server = new JergoViewerServer("localhost", 8282, null);
		server.start();
		server.stop();
	}

	public void startServer() throws IOException, InterruptedException {
		start();
		System.in.read();
	}

	public JergoViewerServer(String hostname, int port, JergoViewer jv) {
		super(hostname, port);
		this.jv = jv;
	}

	@Override
	public Response serve(IHTTPSession session) {
		Method method = session.getMethod();
		String uri = session.getUri();
		Map<String, String> parms = session.getParms();

		File rootDirectory = new File("www");

		Response res = new NanoHTTPD.Response("jErgoViewer - Not Found");
		if (uri.endsWith(".do")) {
			if (uri.equals("/list_kml.do"))
			{
				res = listKml(session);
			}
			else if (uri.equals("/stats.do"))
			{
				res = getStats(session);
			}
		} else {
			if (uri.equals("/"))
			{
				uri = "index.html";
			}

			// static file
			File requestedFile = new File(rootDirectory, uri);
			if (requestedFile.exists())
			{
				try {
					InputStream fileInputStream = new FileInputStream(requestedFile);
					
					String extension = "";
					int i = requestedFile.getName().lastIndexOf('.');
					if (i > 0) {
					    extension =  requestedFile.getName().substring(i+1);
					}

					String mimeType = mimeTypes.containsKey(extension) ? mimeTypes.get(extension) : "application/binary";
					
					res = new NanoHTTPD.Response(Status.OK, mimeType, fileInputStream);
				} catch (FileNotFoundException e) {
					res = new NanoHTTPD.Response(Status.NOT_FOUND, "text/plain", "Error: " + e.getMessage());
				}
			}

		}

		return res;

	}

	private Response listKml(IHTTPSession session) {

		File kmlDirectory = new File("kml");
		File[] kmlFiles = kmlDirectory.listFiles();
		
		JSONArray jsa= new JSONArray();
		
		for (File f : kmlFiles)
		{
			String s = f.getName().replaceFirst("\\.kml$", "");
			jsa.put(s);
		}
		
		return new NanoHTTPD.Response(Status.OK, "application/json", jsa.toString());
	}

	private Response getStats(IHTTPSession session) {
		return new NanoHTTPD.Response(Status.OK, "application/json", "{/*empty*/}");
	}



}
