package briac.net.jergoviewer;

import gnu.io.UnsupportedCommOperationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jergometer.communication.BikeConnector;
import org.jergometer.communication.BikeException;
import org.jergometer.communication.BikeListener;
import org.jergometer.communication.KettlerBikeConnector;
import org.jergometer.model.DataRecord;
import org.json.JSONException;
import org.json.JSONObject;

public class JergoViewer extends Thread implements BikeListener, Runnable{

	private static final String STREETVIEW_URL = "http://maps.googleapis.com/maps/api/streetview";
	private static final String STREETVIEW_KEY = "AIzaSyB2rIeVeFVjygF-MqobdN3giz_SnZkiJ7M";
	private static final int STREETVIEW_WIDTH  = 640;
	private static final int STREETVIEW_HEIGHT = 450;
	
	private static final String SERIAL_PORT = "COM4";

	static final File JSON_FILE =  new File("www/run.json");

	private static final int BIKE_DELAY = 1000;
	private static final boolean TEST = true;
	private FileWriter dataFile;
	
	List<LatLngExtra> path = new ArrayList<LatLngExtra>();
	double currentDistance  = 0;
	double lastDistance     = 0;
	double lastAltitude     = 0;
	int    currentStepIndex = 0;
	private BikeConnector connector;
	
	public JergoViewer(KMLHelper kml) {
		this.path = kml.getPath();
	}

	public static void main(String[] args) throws BikeException,
			UnsupportedCommOperationException, IOException,
			InterruptedException {
		
		String dataFilename = "simulator.data";

		File kmlDir = new File("kml");
		
		KMLHelper kml = new KMLHelper(new File(kmlDir, "SanFrancisco.kml"));
		JergoViewer jv = new JergoViewer(kml);
		
		BikeConnector connector;
		if (TEST)
		{
			connector = new CustomBikeReplay(dataFilename);
		}
		else
		{
			connector = new KettlerBikeConnector();
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
			jv.dataFile = new FileWriter( new File("sessions/session_" + df.format(new Date())) );
		}
		
		jv.setConnector(SERIAL_PORT, connector);
		
		// Start Bike data thread
		jv.start();
		
		// Start Web server thread
		JergoViewerServer server = new JergoViewerServer("localhost", 8282, jv);
		server.start();
	}


	@Override
	public void run() {
		try {
			processBikeData();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setConnector(String serialPort, BikeConnector connector) throws BikeException, UnsupportedCommOperationException, IOException {
		this.connector = connector;
		connector.connect(SERIAL_PORT, this);
	}

	public void processBikeData() throws InterruptedException, IOException {
		// TODO - 
		//		* Auto stop after 20 sec of inactivity ?
		//      * Increase/decrease ergometer power when climbing/going down
		if (dataFile != null)
		{
			dataFile.write("[\n");	
		}
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
		
		if (dataFile != null)
		{
			dataFile.write("]\n");
			dataFile.flush();
			dataFile.close();
		}

		connector.close();
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
			
			//  Add a slight coef to account for the server / browser delay( distance * 1.001 ?)
			//distance = distance * 1.001;
			
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

		double deltaAltitude = point.getAltitude() - lastAltitude;
		lastAltitude = point.getAltitude();

		// Modify power according to altitude change
		try {
			if (deltaAltitude > 5)
			{
				connector.sendSetPower(data.getDestPower() + 5);
			}
			else if (deltaAltitude < 5)
			{
				connector.sendSetPower(data.getDestPower() - 5);
			}
		} catch (IOException e) {
			System.err.println("Couldn't change power: " + e.getMessage());
		}

		// Calculate pace
		double pace = 1 / (new Double(data.getSpeed())) * 600;
		
		try {
			jso.put("deltaAltitude", String.format("%.2f", deltaAltitude));
			jso.put("calcDistance", Math.round(distance));
			jso.put("pace", String.format("%.2f", pace));
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
			
			if  (! TEST)
			{
				dataFile.write(jso.toString());
				dataFile.write(",\n");
				dataFile.flush();			
			}


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
