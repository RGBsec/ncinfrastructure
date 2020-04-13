package xyz.rgbpwn.ctf.ncinfrastructure;

import java.io.Flushable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

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
			for (;;) {
				try {
					Socket socket = serverSocket.accept();
					sockets.add(socket);
					new Thread(new SocketThread(socket, new ProcessExecutor(command))).start();
					;
				} catch (IOException e) {
					e.printStackTrace();
				}
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
		ProcessExecutor executor;
		StartedProcess process;

		public SocketThread(Socket socket, ProcessExecutor executor) {
			this.socket = socket;
			this.executor = executor;

		}

		public void run() {
			try {
				executor.redirectError(socket.getOutputStream());
				executor.redirectInput(socket.getInputStream());
				executor.redirectOutput(socket.getOutputStream());

				process = executor.start();

				for (;;)
					;

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

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
