

import java.io.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import static java.lang.System.*;

public class Server {
	
	private ORB orb;
    private DBHandler dbHandler;
    private org.omg.CORBA.Object obj;
    private DBHandlerImpl dbh_impl;
    private POA rootPOA;
	
	/**
	 * Instantiates a new server.
	 *
	 * @param args the args
	 * @param host the host name of the DB server
	 * @param databaseName the database name
	 * @param username the username
	 * @param password the password
	 * @param port the port
	 */
	public Server(String[] args, String host, String databaseName, String username, 
			String password, int port) {
		
		initialization(args);
		
		dbh_impl = new DBHandlerImpl(host, databaseName, username, password, port);
		dbHandler = dbh_impl._this(orb);
		
		getRef();
		
		try {
			rootPOA.the_POAManager().activate();
		} catch (AdapterInactive e) {
			out.println("Exception: " + e.getMessage());
			exit(1);
		}
      	orb.run();
	}
	
	private void initialization(String[] args) {
		Properties props = getProperties();
		orb = ORB.init(args, props);
		obj = null;
		rootPOA = null;
		
		try {
      		obj = orb.resolve_initial_references("RootPOA");
      		rootPOA = POAHelper.narrow(obj);
      	} catch (org.omg.CORBA.ORBPackage.InvalidName e) { }

	}
	
	private void getRef() {
		try {
      		FileOutputStream file =
      				new FileOutputStream("DBHandler.ref");
      		PrintWriter writer = new PrintWriter(file);
	        String ref = orb.object_to_string(dbHandler);
	        writer.println(ref);
	        writer.flush();
	        file.close();
	        out.println("Server started."
	          + " Stop: Ctrl-C");
	      
      	} catch (IOException ex) {
      		out.println("File error: "
      				+ ex.getMessage());
      		exit(2);
      	}
	}
	
	
  
	public static void main(String[] args) {
	  
		if(args.length == 9) {
			String[] serverArgs = {args[0], args[1], args[2], args[3]};
			String localhost = args[4];
			String databaseName = args[5];
			String username = args[6];
			String password = args[7];
			int port = 5432;
			
			try {
				port = Integer.parseInt(args[8]);
			} catch(NumberFormatException e) {
				System.out.println("Invalid port number");
				System.exit(1);
			}
			
			Server server = new Server(
					serverArgs, 
					localhost, 
					databaseName,
					username, 
					password, 
					port);
		} else {
			System.out.println("Usage: "
					+ "java -ORBInitialPort 1050 "
					+ "-ORBInitialHost hostname "
					+ "database hostname "
					+ "database name "
					+ "username "
					+ "password "
					+ "port");
		}
	}
}