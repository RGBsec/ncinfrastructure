package xyz.rgbpwn.ctf.ncinfrastructure;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Socket;

import org.junit.jupiter.api.Test;

class NetConsoleTest {

	@Test
	void testNetConsole() throws InterruptedException {
		NetConsole console;
		try {
			if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
				console = new NetConsole(123456, "cmd.exe");
				Socket socket = new Socket("127.0.0.1", 123456);
				
				
			}
			else {
				console = new NetConsole(123456, "/bin/bash");
			}
				
		} catch (IOException e) {
			fail(e);
		}
	}

}
