package briac.net.jergoviewer;

import gnu.io.UnsupportedCommOperationException;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jergometer.communication.BikeConnector;
import org.jergometer.communication.BikeException;
import org.jergometer.communication.BikeListener;
import org.jergometer.communication.KettlerBikeConnector;
import org.jergometer.model.BikeSession;
import org.jergometer.model.DataRecord;
import org.jergometer.model.MiniDataRecord;
import org.json.JSONException;
import org.json.JSONObject;

import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

public class JergoViewer implements BikeListener {

	private static final String STREETVIEW_URL = "http://maps.googleapis.com/maps/api/streetview";
	private static final String STREETVIEW_KEY = "AIzaSyB2rIeVeFVjygF-MqobdN3giz_SnZkiJ7M";
	private static final int STREETVIEW_WIDTH  = 640;
	private static final int STREETVIEW_HEIGHT = 450;
	
	private static final String SERIAL_PORT = "COM4";

	static final File JSON_FILE =  new File("c:/xampp/htdocs/jergoviewer/run.json");

	private static final int BIKE_DELAY = 1000;
	private static final boolean TEST = true;

	
	List<LatLngExtra> path = new ArrayList<LatLngExtra>();
	double currentDistance  = 0;
	double lastDistance     = 0;
	int    currentStepIndex = 0;
	
	public JergoViewer(KMLHelper kml) {
		this.path = kml.getPath();
	}

	public static void main(String[] args) throws BikeException,
			UnsupportedCommOperationException, IOException,
			InterruptedException {
		

		BikeConnector connector;
		String dataFilename = "simulator.data";

		File kmlDir = new File("C:/Users/briac/git/jergoviewer/briac.net/kml");
		
		KMLHelper kml = new KMLHelper(new File(kmlDir, "Tokyo.kml"));
		JergoViewer jv = new JergoViewer(kml);
		
		if (TEST)
		{
			connector = new CustomBikeReplay(dataFilename);
		}
		else
		{
			connector = new KettlerBikeConnector();
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
			BikeConnector bsim = new CustomConnectorSimulatorRecord(new File("session_" + df.format(new Date())));
			bsim.connect(SERIAL_PORT, jv);
		}
		
		connector.connect(SERIAL_PORT, jv);
		
		// TODO - 
		//		* Auto stop after 20 sec of inactivity ?
		//      * Increase/decrease ergometer power when climbing/going down
		while (true) {
			try {
				connector.sendGetData();
				Thread.sleep(BIKE_DELAY);
			}
			catch (IOException e)
			{
				break;
			}

		}

		connector.close();
		System.out.println("End of data");
	}

	@Override
	public void bikeAck() {
		System.out.println("bikeAck");

	}
	
	private JSONObject pathJson(LatLngExtra l)
	{
		JSONObject j = new  JSONObject();
		
		try {
			j.put("lat", l.getLatitude());
			j.put("lon", l.getLongitude());
			j.put("alt", l.getAltitude());
			j.put("bearing", l.getBearing());
			j.put("distance", l.getDistance());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return j;
	}
	
	
	@Override
	public void bikeData(DataRecord data) {
		//System.out.println("bikeData: " + data);

		// distance 1 = 100m
		double bikeDistance = data.getDistance() * 100;

		double distance;
		// Kettler has 100m distance increments, we try to calculate the distance in between.
		if (bikeDistance > lastDistance)
		{
			distance = bikeDistance;
		}
		else
		{
			distance = lastDistance + (new Double(data.getSpeed()) / 100);
			
			//  Add a slight coef to account for the server / browser delay( distance * 1.1 ?)
			distance = distance * 1.1;
			
			// Don't go over the indicated bike distance
            if (distance > bikeDistance + 100) {
                distance -= 100;
            }
		}
		
		while (path.size() >= currentStepIndex && path.get(currentStepIndex).getDistance() < distance)
		{
			currentStepIndex++;
		}

		LatLngExtra point = path.get(currentStepIndex);
		lastDistance  = distance;
		
		JSONObject jso = new JSONObject(data);
		
		try {
			jso.put("calcDistance", Math.round(distance));
			jso.put("point", pathJson(point));
			
			jso.put("img", getStreetViewUrl(point));
			
			if (path.size() > currentStepIndex)
			{
				LatLngExtra nextPoint = path.get(currentStepIndex + 1);
				jso.put("nextPoint", pathJson(nextPoint));
				jso.put("nextImg", getStreetViewUrl(nextPoint));
				
			}
			
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		System.out.println(jso.toString());

		try {
			FileWriter fos = new FileWriter(JSON_FILE);
			fos.write(jso.toString());
			
			fos.flush();
			fos.close();
		} catch (IOException e) {
			System.err.println("Error while writing file \"" + JSON_FILE + "\": " + e.getMessage());
		}
	}

	private String getStreetViewUrl(LatLngExtra point) {
		return String.format(
				"%s?sensor=false&key=%s&size=%dx%d&location=%.4f,%.4f&heading=%d", 
					STREETVIEW_URL, 
					STREETVIEW_KEY, 
					STREETVIEW_WIDTH, 
					STREETVIEW_HEIGHT, 
					point.getLatitude(), 
					point.getLongitude(), 
					point.getBearing()
		);
	}

	@Override
	public void bikeError() {
		System.out.println("// bikeError");
	}

	@Override
	public void bikeDestPowerChanged(int change) {
		System.out.println("// bikeDestPowerChanged: " + change + " => " + (change*5));
	}

}
