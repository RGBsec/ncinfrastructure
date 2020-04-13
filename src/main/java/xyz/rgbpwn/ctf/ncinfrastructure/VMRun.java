package xyz.rgbpwn.ctf.ncinfrastructure;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

public class VMRun {
	private static int port;

	public static void main(String[] argv) throws IOException {

		Stream.of(new File(".").listFiles()).forEach(n -> {
			System.out.println(n.getName());
		});
		;
		if (!new File(argv[0]).exists()) {
			System.err.printf("File not found: %s", argv[0]);
			System.err.println("USAGE: java -jar pwn.jar <binary> <port>");
			System.exit(1);
		}
		try {
			port = Integer.parseInt(argv[1]);
		} catch (NumberFormatException e) {
			System.err.printf("Invalid port: %s", argv[1]);
			System.err.println("USAGE: java -jar pwn.jar <binary> <port>");
			System.exit(1);
		}

		new NetConsole(port, "/usr/app/" + argv[0]);

	}

}
