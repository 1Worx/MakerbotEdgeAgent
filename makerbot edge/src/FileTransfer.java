import java.util.HashMap;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.filetransfer.FileTransferVirtualThing;

@SuppressWarnings("serial")
public class FileTransfer extends FileTransferVirtualThing {

	public FileTransfer(String name, String description,
            ConnectedThingClient client, HashMap<String, String> virtualDirectories)
	{
		super(name, description, client, virtualDirectories);
	}

}
