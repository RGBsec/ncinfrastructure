package xyz.rgbpwn.ctf.ncinfrastructure;

import java.io.BufferedReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

/**
 * A class which is similar to {@link java.io.Console} that operates over a
 * socket, interfacing with a process
 * 
 * @author Alec Petridis
 * @see java.io.Console
 */

// TODO: Write tests
public class NetConsole implements Flushable {

	private ServerSocket serverSocket;
	private ArrayList<Socket> sockets;
	private String command;

	/**
	 * Constructor for {@link NetConsole}
	 * 
	 * 
	 * @throws IOException
	 * @param port    - port to listen on
	 * @param process - a running process which the console will interface with
	 */

	public NetConsole(int port, String command) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.command = command;
		sockets = new ArrayList<Socket>();
		new Thread(new ListenerThread()).start();
	}

	private static void copyData(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[8 * 1024];
		int len;
		while ((len = in.read(buffer)) > 0) {
			out.write(buffer, 0, len);
		}
	}

	/**
	 * <p>
	 * This is a separate process used to monitor for incoming connections. It will
	 * spawn a new {@link SocketThread} for each separate connection
	 * </p>
	 * 
	 * @author Alec Petridis </br>
	 * 
	 * @see SocketThread
	 * @see Runnable
	 */
	protected class ListenerThread implements Runnable {

		public void run() {
			try {
				Socket socket = serverSocket.accept();
				sockets.add(socket);
				new SocketThread(socket, Runtime.getRuntime().exec(command));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * <p>
	 * Contains one {@link Socket} and one {@link Process} with connected I/O
	 * streams.
	 * </p>
	 * 
	 * @author Alec Petridis
	 * @see Runnable
	 * @see IOUtils#copy
	 */
	protected class SocketThread implements Runnable {
		Socket socket;
		Process process;

		public SocketThread(Socket socket, Process process) {
			this.socket = socket;
			this.process = process;

		}

		public void run() {
			try {
				InputStream socketInputStream = socket.getInputStream();
				OutputStream socketOutputStream = socket.getOutputStream();

				InputStream processInputStream = process.getInputStream();
				InputStream processErrorStream = process.getErrorStream();
				OutputStream processOutputStream = process.getOutputStream();

				for (;;) {
					copyData(processInputStream, socketOutputStream);
					copyData(processErrorStream, socketOutputStream);

					copyData(socketInputStream, processOutputStream);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				process.destroyForcibly();
			}

		}
	};

	/**
	 * {@inheritDoc}
	 * 
	 */
	public void flush() throws IOException {
		sockets.forEach(socket -> {
			try {
				socket.getOutputStream().flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

	}

	public static void main(String[] argv) throws IOException {
		NetConsole console = new NetConsole(12345, "cmd.exe");
		Socket socket = new Socket("127.0.0.1", 12345);
		PrintWriter write = new PrintWriter(socket.getOutputStream());
		write.println("dir");
		console.flush();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		System.out.println(reader.readLine());
	}

}
