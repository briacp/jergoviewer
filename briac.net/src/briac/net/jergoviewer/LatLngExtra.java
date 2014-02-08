package briac.net.jergoviewer;

import com.javadocmd.simplelatlng.LatLng;

public class LatLngExtra extends LatLng {
	private static final long serialVersionUID = -2440999674039177599L;

	double altitude;
	double distance = 0;
	int bearing = 0;

	public int getBearing() {
		return bearing;
	}

	public void setBearing(int bearing) {
		this.bearing = bearing;
	}

	public LatLngExtra(double latitude, double longitude, double altitude, double distance) {
		super(latitude, longitude);
		this.altitude = altitude;
		this.distance = distance;
	}

	public LatLngExtra(double latitude, double longitude, double altitude) {
		this(latitude, longitude, altitude, 0);
	}

	public double getAltitude() {
		return altitude;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public void addDistance(double distance) {
		this.distance += distance;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getLatitude());
		builder.append(", ");
		builder.append(getLongitude());
		builder.append(", ");
		builder.append(getAltitude());
		builder.append(", ");
		builder.append(getDistance());
		builder.append(", ");
		builder.append(getBearing());
		builder.append("]");
		return builder.toString();
	}
	
	

}
