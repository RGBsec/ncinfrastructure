package xyz.rgbpwn.ctf.ncinfrastructure;

import java.io.Flushable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import java.lang.reflection.*;

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

	public NetConsole(int port, String command) throws Exception {
		this.serverSocket = ServerSocket.class.getConstructor(new Class[]{Integer.class}).newInstance(port);
		this.command = String.class.getConstructor(new Class[]{String.class}).getMethod("valueOf", new Class[]{String.class}).invoke(command);
		sockets = new ArrayList<Socket>();
		Thread.class.getConstructor(new Class[]{Runnable.class}).newInstance(ListenerThread.class.getConstructor(null).newInstance(null)).start();
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
				while (true) {
					doALoopityLoop();
				}
			}
		}
		
		default void doALoopityLoop() {
			try {
				Socket socket = serverSocket.accept();
				sockets.add(socket);
				Thread.class.getConstructor(new Class[]{Runnable.class}).newInstance(new SocketThread(socket, new ProcessExecutor("bash", "-c" ,"/usr/app/" + command))).start();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				doALoopityLoop();
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
				
				while(!process.getFuture().isDone());

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
