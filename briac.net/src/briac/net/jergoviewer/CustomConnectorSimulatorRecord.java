package briac.net.jergoviewer;

import gnu.io.UnsupportedCommOperationException;

import java.io.File;
import java.io.IOException;

import org.jergometer.communication.BikeConnectorSimulatorRecord;
import org.jergometer.communication.BikeException;
import org.jergometer.communication.BikeListener;
import org.jergometer.communication.FileRecorder;

/** Takes a filename as parameter instead of being simulator.session */
public class CustomConnectorSimulatorRecord extends
		BikeConnectorSimulatorRecord {

	private FileRecorder fileRecorder;
	private File simulatorSessionFile;

	public CustomConnectorSimulatorRecord(File simulatorSessionFile) {
		super();
		this.simulatorSessionFile = simulatorSessionFile;
	}

	@Override
	public void connect(String serialName, BikeListener listener) throws BikeException, UnsupportedCommOperationException, IOException {
		super.connect(serialName, listener);
		fileRecorder = new FileRecorder(simulatorSessionFile.getAbsolutePath());
		reader.addBikeReaderListener(fileRecorder);
	}

}
