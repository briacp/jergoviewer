package briac.net.jergoviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class JergoViewerServer extends NanoHTTPD {
	
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
		JergoViewerServer server = new JergoViewerServer("localhost", 8282);
		try {
			server.start();
		} catch (IOException ioe) {
			System.err.println("Couldn't start server:\n" + ioe);
			System.exit(-1);
		}

		System.out.println("Server started, Hit Enter to stop.\n");

		try {
			System.in.read();
		} catch (Throwable ignored) {
		}

		server.stop();
		System.out.println("Server stopped.\n");
	}

	public JergoViewerServer(String hostname, int port) {
		super(hostname, port);
	}

	@Override
	public Response serve(IHTTPSession session) {
		Method method = session.getMethod();
		String uri = session.getUri();
		System.out.println(method + " '" + uri + "' ");
		Map<String, String> parms = session.getParms();

		File rootDirectory = new File(
				"C:/Users/briac/git/jergoviewer/briac.net/www");

		Response res = new NanoHTTPD.Response("jErgoViewer - Not Found");
		if (uri.endsWith(".do")) {
			// do something
		} else {
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

}
