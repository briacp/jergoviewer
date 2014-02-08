package briac.net.jergoviewer;

import gnu.io.UnsupportedCommOperationException;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.jergometer.communication.BikeConnector;
import org.jergometer.communication.BikeException;
import org.jergometer.communication.BikeListener;
import org.jergometer.model.DataRecord;

import de.endrullis.utils.StreamUtils;

class CustomBikeReplay implements BikeConnector 
{

	private BikeListener listener = null;
	private DataInputStream sessionInputStream;
	private String sessionName;

	public void connect(String serialName, BikeListener listener) throws BikeException, UnsupportedCommOperationException, IOException {
		this.listener = listener;
		sessionInputStream = new DataInputStream(StreamUtils.getInputStream(sessionName));
	}
	
	public CustomBikeReplay(String sessionName) {
		this.sessionName = sessionName;
	}

	public void sendHello() throws IOException {
		listener.bikeAck();
	}
	
	public void sendReset() throws IOException {
		listener.bikeAck();
	}
	
	public void sendGetId() throws IOException {
	}
	
	public void sendGetData() throws IOException {
		listener.bikeData(new DataRecord(sessionInputStream));
	}

	public void sendSetPower(int power) throws IOException {
	}

	public void close() throws IOException {
	}

	public String getName() {
		return "simulator-" + sessionName;
	}

	public String toString() {
		return getName();
	}
}