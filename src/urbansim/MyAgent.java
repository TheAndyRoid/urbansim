package urbansim;

import java.util.List;

import de.matthiasmann.continuations.Coroutine;
import de.matthiasmann.continuations.CoroutineProto;
import de.matthiasmann.continuations.SuspendExecution;

public class MyAgent implements DeviceAgent, CoroutineProto{
	private Device device;
	public Coroutine co;
	
	// This is where we should execute main
	public final void coExecute() throws SuspendExecution {
		main();
	}

	public MyAgent(Device device) {
		this.device = device;
		co = new Coroutine(this);
	}

	// This method must be friendly. It must yeild.
	public void main() throws SuspendExecution {
		do {
			System.out.println("running");
			List<Device> inRange = device.scan();
			// System.out.println("Main");
			for (Device d : inRange) {
				Message msg = new Message(device, this, d, 0);
				System.out.println("Sent message");
				device.sendTo(device, msg);
			}
			while (device.recv() != null) {
				System.out.println("Got Message");
			}
			System.out.println("yeild");
			Coroutine.yield();
		} while (true);
	}

}
