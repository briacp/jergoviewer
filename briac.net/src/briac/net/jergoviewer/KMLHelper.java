package briac.net.jergoviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

public class KMLHelper {

	private File kmlFile;

	private static double SMOOTH_COEF = 0.91;

	public KMLHelper(File kmlFile) {
		this.kmlFile = kmlFile;
	}

	public List<LatLngExtra> getPath() {
		ArrayList<LatLngExtra> points = new ArrayList<LatLngExtra>();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {

			InputStream xmlInput = new FileInputStream(kmlFile);
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new KmlCoordinateHandler(points);
			saxParser.parse(xmlInput, handler);

		} catch (Throwable err) {
			err.printStackTrace();
		}

		return points;
	}

	private class KmlCoordinateHandler extends DefaultHandler {
		ArrayList<LatLngExtra> points;
		StringBuffer coordinates = new StringBuffer();
		private boolean inCoordinate;
		private boolean inLineString;

		public KmlCoordinateHandler(ArrayList<LatLngExtra> points) {
			this.points = points;
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (inCoordinate && inLineString) {
				coordinates.append(Arrays
						.copyOfRange(ch, start, start + length));
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
		    if (qName.equals("LineString")) {
				inLineString = false;
			}
			if (inCoordinate && inLineString && qName.equals("coordinates")) {

				String[] data = coordinates.toString().trim().split("\\s+");

				double lastAlt = 0;
				double distance = 0;
				LatLngExtra lastPoint = null;

				for (String p : data) {
					
					//System.err.println("{{" + p + "}}");
					String[] set = p.split(",");

					double alt = Double.parseDouble(set[2]);

					// Smoothing the altitude
					double smoothAlt = lastAlt == 0 ? alt : SMOOTH_COEF
							* lastAlt + (1 - SMOOTH_COEF) * alt;

					lastAlt = smoothAlt;

					LatLngExtra point = new LatLngExtra(
							Double.parseDouble(set[1]),
							Double.parseDouble(set[0]), smoothAlt);

					if (lastPoint != null) {
						distance += LatLngTool.distance(lastPoint, point,
								LengthUnit.METER);
						point.setDistance(Math.round(distance));

						int bearing = (int) Math.round(LatLngTool
								.initialBearing(lastPoint, point));
						points.get(points.size() - 1).setBearing(bearing);
					}
					lastPoint = point;

					points.add(point);
				}

			}
			
			if (qName.equals("coordinates"))
			{
				inCoordinate = false;
			}
		}

		@Override
		public void startDocument() throws SAXException {
			inCoordinate = false;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.equals("coordinates")) {
				inCoordinate = true;
			}
			else if (qName.equals("LineString")) {
				inLineString = true;
			}
		}

	}
}
