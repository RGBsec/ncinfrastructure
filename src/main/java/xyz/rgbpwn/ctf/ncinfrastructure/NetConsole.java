package xyz.rgbpwn.ctf.ncinfrastructure;

import java.io.Flushable;
import java.io.IOException;
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
		serverSocket = new ServerSocket(port);
		this.command = command;
		new Thread(new ListenerThread()).start();
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
			}
		}
	};

	/**
	 * <p>
	 * Contains one {@link Socket} and one {@link Process} with connected I/O streams. 
	 * </p>
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
				IOUtils.copy(socket.getInputStream(), process.getOutputStream());
				IOUtils.copy(process.getInputStream(), socket.getOutputStream());
				IOUtils.copy(process.getErrorStream(), socket.getOutputStream());
			} catch (IOException e) {

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

}
