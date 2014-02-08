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
	private KMLHelper kml;
	
	public JergoViewer(KMLHelper kml) {
		this.kml = kml;
		this.path = kml.getPath();
	}

	public static void main(String[] args) throws BikeException,
			UnsupportedCommOperationException, IOException,
			InterruptedException {
		

		BikeConnector connector;
		String dataFilename = "simulator.data";
		String fileName;
		
		fileName =  "c:/users/briac/desktop/Scenictokyo.kml";
		fileName = "c:/users/briac/desktop/VerrieresChatillon.kml";
		fileName = "c:/users/briac/desktop/SanFrancisco-NobHill-RussianHill-TelegraphHill-2012.kml";
		
		KMLHelper kml = new KMLHelper(new File(fileName));
		JergoViewer jv = new JergoViewer(kml);

		//jv.testPathKML();
		
		if (TEST)
		{
			//dataFromSession("simulator.session", dataFilename);
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
			
			// TODO Add a slight coef to account for the server / browser delay( distance * 1.1 ?)
			
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

	
	/** test */
	void dataFromSession(String sessionFilename, String dataFile)
			throws IOException {
		BikeSession bs = new BikeSession(new File(sessionFilename));

		DataOutputStream out = new DataOutputStream(new FileOutputStream(
				new File(dataFile)));
		ArrayList<MiniDataRecord> data = bs.getData();

		Calendar startCalendar = Calendar.getInstance();
		long startMs = startCalendar.getTimeInMillis();
		Calendar runCalendar = (Calendar) startCalendar.clone();

		// https://github.com/kahara/pyergometer/blob/master/data/2012-06-03T17:59:06.csv

		// distance = time * speed;

		int distance = 0;

		for (MiniDataRecord d : data) {
			// 60 rpm = 9,5km/h
			// speed = 10 * (power * 0.15833)
			double speed = Math.ceil(d.getPedalRpm() * 1.583333333);

			// distance = speed * time ( time = 1s / speed in km/h*10)
			distance += speed / 30.5; // XXX should be 36? (s/60/60*10)

			runCalendar.add(Calendar.SECOND, 1);

			long runMs = runCalendar.getTimeInMillis();
			long diff = runMs - startMs;
			int secs = (int) (diff / 1000);

			int hours = secs / 3600, remainder = secs % 3600, minutes = remainder / 60, seconds = remainder % 60;

			String disHour = (hours < 10 ? "0" : "") + hours, disMinu = (minutes < 10 ? "0" : "")
					+ minutes, disSec = (seconds < 10 ? "0" : "") + seconds;
			String length = disHour + ":" + disMinu + ":" + disSec/* + ".0" */;

			out.writeInt(d.getPulse()); // pulse
			out.writeInt(d.getPedalRpm()); // pedalRpm
			out.writeInt((int) speed); // speed
			out.writeInt((int)((float)distance / 100)); // distance
			out.writeInt(d.getPower()); // destPower
			out.writeInt(0); // energy
			out.writeUTF(length); // time
			out.writeInt(0); // realPower
		}

		out.flush();
		out.close();
	}
	
	
	

	void testPathKML() {
		double totalDistance = 0;
		
		int i = 0;
		for (LatLngExtra l : path)
		{
			if ( i > 0)
			{
				LatLngExtra start = path.get(i-1);
				LatLngExtra end   = path.get(i);

				double distance = LatLngTool.distance(start, end, LengthUnit.METER);
				totalDistance += Math.floor(distance);
				
				int bearing = (int) Math.round(LatLngTool.initialBearing(start, end));
				System.out.println(i + ":" + /*start + "/" + end +*/ ":\t" + Math.round(distance) + " - " + bearing + " = " + Math.round(totalDistance));				
			}

			i++;
		}
		System.out.println(path); 
		System.exit(43);
	}


}
