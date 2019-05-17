
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



// TODO: Auto-generated Javadoc
/**
* title: Tme2client.java
* to compile: javac Tme2client.java
* 
* description: This class implements the DBHandlerPOA. It establishes aconnection
* 			   to the database, and implements all the interface methods. 
* 
 * @author Zakaria Bakkal
 * @date August 01, 2018
 * @verion 4
 */
public class DBHandlerImpl extends DBHandlerPOA {
	
	/** The url. */
	// database url
	private final String URL = "jdbc:postgresql://";
	
	/** The connection. */
	// database connection
	private static Connection connection;
	
	/** The statement. */
	// sql statements
	private static Statement statement;
	
	/** The records. */
	// results records
	private ResultSet records;
	
	/**
	 * Instantiates a new DBHandlerImpl.
	 *
	 * @param host the host name of the database server
	 * @param databaseName the database name
	 * @param username the username
	 * @param password the password
	 * @param port the port
	 */
	public DBHandlerImpl(String host, String databaseName, String username, 
			String password, int port) {
		records = null;
		connectToDB(host, databaseName, username, password, port);
	}
	
	/**
	 * Connect to DB.
	 *
	 * @param host the host name of the database server
	 * @param databaseName the database name
	 * @param username the username
	 * @param password the password
	 * @param port the port
	 */
	private void connectToDB(String host, String databaseName, String username, 
			String password, int port) {
		
        try {
        	// load the driver
        	Class.forName("org.postgresql.Driver");
			// establish a connection with the database
			connection = DriverManager.getConnection(URL + 
			        host + ":" + port + "/" + databaseName, username, password);
			// create an instance for Statement to execute the sql query
			statement = connection.createStatement();
			System.out.println("Database Connection Established");
		} catch (SQLException e) {
			System.out.println("Server: SQLException " + e.getMessage());
		} catch (ClassNotFoundException e) {
			System.out.println("Server: Class Not Found Exception, Missing Driver " + e.getMessage());
		}
        
	}
	
	/**
	 * Close DB connection.
	 */
	private void closeDBConnection() {
		try {
			if(connection.isValid(0)) {
				connection.close();
			}
			System.out.println("Connection to database is closed");
			
		} catch (SQLException e) {
			System.out.println("Server: Error closing connection to database");
		}
	}
	
	/**
	 * returns the row count of records retrieved by an sql statement.
	 *
	 * @return int
	 */
	private int rowCount() {
		
		ResultSet temp = records;
		
		int count = 0;
		try{
	        while (temp.next()) {
	            count++;
	        }
	    } catch (Exception e){
	       System.out.println("DBHandler: rowCount(): Exception " + e.getMessage());
	    }
		
		return count;
	}

	/* (non-Javadoc)
	 * @see DBHandlerOperations#addClient(java.lang.String, java.lang.String, int)
	 */
	/* 
	 * adds a client to the database.
	 */
	@Override
	public void addClient(String mac, String ipAddress, int port) {
		// check if the client already exists
		if(!clientExist(mac)) {
			// construct the query
			String query = "INSERT INTO User_Table VALUES "
					+ "('" + mac + "'"
					+ ",'" + ipAddress + "'"
					+ "," + port + ")";
			
			try {
				// execute the query
				statement.executeUpdate(query);
			} catch (SQLException e) {
				System.out.println("DBHandler: addClient(): SQL Exception " + e.getMessage());
			}
		} else { // the client already exists
			// in case the ip address changes
			updateIPAddress(mac, ipAddress);
			// in case the port changes
			updatePort(mac, port);
		}
		
	}

	/* (non-Javadoc)
	 * @see DBHandlerOperations#clientExist(java.lang.String)
	 */
	/* 
	 * check if the client exists in the DB.
	 * @return boolean
	 */
	@Override
	public boolean clientExist(String mac) {
		// construct the query
		String query = "SELECT mac from User_Table "
				+ "WHERE mac = '" + mac + "'";
		
		try {
			// execute the query
			records = statement.executeQuery(query);
			// if the client exists the condition is true
			if(records.next()) {
				return true;
			}
		} catch (SQLException e) {
			System.out.println("DBHandler: clientExist(): SQL Exception " + e.getMessage());
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see DBHandlerOperations#updateIPAddress(java.lang.String, java.lang.String)
	 */
	/* 
	 * updates the client ip address
	 */
	@Override
	public void updateIPAddress(String mac, String ipAddress) {
		if(clientExist(mac)) {
			String query = "UPDATE User_Table "
					+ "SET uip_address = '" + ipAddress + "' "
					+ "WHERE mac = '" + mac + "'";
			
			try {
				statement.executeUpdate(query);
			} catch (SQLException e) {
				System.out.println("DBHandler: updateIPAddress(): SQL Exception " + e.getMessage());
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see DBHandlerOperations#updatePort(java.lang.String, int)
	 */
	/* 
	 * updates the client port
	 */
	@Override
	public void updatePort(String mac, int port) {
		if(clientExist(mac)) {
			String query = "UPDATE User_Table "
					+ "SET uport = " + port + " "
					+ "WHERE mac = '" + mac +"'";
			
			try {
				statement.executeUpdate(query);
			} catch (SQLException e) {
				System.out.println("DBHandler: updatePort(): SQL Exception " + e.getMessage());
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see DBHandlerOperations#shareFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	/* 
	 * adds a new file to the database or changes its status to shared if
	 * it already exists.
	 */
	@Override
	public void shareFile(String mac, String filename, String size) {
		// check if the file exists
		if(!fileExist(filename, mac)) {
			String query = "INSERT INTO File_Table VALUES "
					+ "('" + mac + "'"
					+ ",'" + filename + "'"
					+ ",'s'"
					+ ",'" + size +"')";
			
			try {
				statement.executeUpdate(query);
			} catch (SQLException e) {
				System.out.println("DBHandler: shareFile()-if: SQL Exception " + e.getMessage());
			}
		} else if(!isShared(filename, mac)){ // if it exits check if it is shared
			String query = "UPDATE File_Table "
					+ "SET fstatus = 's' "
					+ "WHERE mac = '" + mac + "' "
					+ "AND fname = '" + filename + "'";
			
			try {
				statement.executeUpdate(query);
			} catch (SQLException e) {
				System.out.println("DBHandler: shareFile()-else: SQL Exception " + e.getMessage());
			}
		}
		
	}

	/* 
	 * removes a file from the DB by changing its status to not shared and
	 * associated with the mac address
	 */
	@Override
	public void removeFile(String filename, String mac) {
		// check if the file exists and shared
		if(fileExist(filename, mac) && isShared(filename, mac)) {
			String query = "UPDATE File_Table "
					+ "SET fstatus = 'n' "
					+ "WHERE mac = '" + mac + "' "
					+ "AND fname = '" + filename +"'";
			
			try {
				statement.executeUpdate(query);
			} catch (SQLException e) {
				System.out.println("DBHandler: removeFile() : SQL Exception " + e.getMessage());
			}
		}
		
	}

	/* 
	 * checks if the file is shared associated with the mac address.
	 * @return boolean
	 */
	@Override
	public boolean isShared(String filename, String mac) {
		
		String query = "SELECT fname from File_Table "
				+ "WHERE mac = '" + mac + "' "
				+ "AND fname = '" + filename + "' "
				+ "AND fstatus = 's'";
		
		try {
			records = statement.executeQuery(query);
			if(records.next()) {
				return true;
			}
		} catch (SQLException e) {
			System.out.println("DBHandler: isShared(): SQL Exception " + e.getMessage());
		}
		
		return false;
	}

	/* 
	 * checks if the exists.
	 * @return boolean
	 */
	@Override
	public boolean fileExist(String filename, String mac) {
		
		String query = "SELECT fname, mac from File_Table "
				+ "WHERE mac = '" + mac + "' "
				+ "AND fname = '" + filename + "'";
		
		try {
			records = statement.executeQuery(query);
			if(records.next()) {
				return true;
			}
		} catch (SQLException e) {
			System.out.println("DBHandler: fileExist():SQL Exception " + e.getMessage());
		}
		
		return false;
	}
	
	/* 
	 * searches the DB for a file associated with the mac address,
	 * @return String search result records
	 */
	@Override
	public String search(String filename, String mac) {
		String query = "SELECT fname, fsize, mac from File_Table "
				+ "WHERE fname = '" + filename + "' "
				+ "AND mac <> '" + mac + "' "
				+ "AND fstatus = 's'";
		
		try {
			records = statement.executeQuery(query);
			// construct the search result in the format:
			// filename,filesize,mac;filename,filesize,mac;
			StringBuilder searchResult = new StringBuilder();
			while(records.next()) {
				searchResult.append(records.getString("fname"));
				searchResult.append(",");
				searchResult.append(records.getString("fsize"));
				searchResult.append(",");
				searchResult.append(records.getString("mac"));
				searchResult.append(";");
			}
			
			return searchResult.toString();
		} catch (SQLException e) {
			System.out.println("DBHandler: search(): SQL Exception " + e.getMessage());
		}
		
		return null;
	}

	/* 
	 * returns a string representing the file owner.
	 * @return String file owner info
	 */
	@Override
	public String downloadFile(String filename, String mac) {
		
		String query = "SELECT uip_address, uport from User_Table, File_Table "
				+ "WHERE File_Table.fname = '" + filename + "' "
				+ "AND File_Table.mac = '" + mac + "' "
				+ "AND User_Table.mac = '" + mac + "'";
		
		try {
			records = statement.executeQuery(query);
			// store ip address
			if(records.next()) {
				String downloadInfo = records.getString("uip_address") + ","
						+ records.getShort("uport");
				return downloadInfo;
			}
		} catch (SQLException e) {
			System.out.println("DBHandler: SQL Exception " + e.getMessage());
		}
		
		return null;
	}


}


