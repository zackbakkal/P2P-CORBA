import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.System.exit;
import static java.lang.System.getProperties;
import static java.lang.System.out;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;

/**
* title: Tme2client.java
* to compile: javac Tme2client.java
* 
* description: This class represents a client of the p2p application. It is
*               used to setup the client parameters, register the in the 
*               database, share and remove files, download and upload files.
*               It also starts a server that server other clients requests.
* 
 * @author Zakaria Bakkal
 * @date August 02, 2018
 * @verion 5
 */
public class Tme2client {
    
    // used for maximum file downloads and uplaods
    private final int MAX = 3_000_000;
    // used for client requests handled
    private final int MAX_REQ = 10;
    // the listening port of the client server
    private final int SERVER_PORT = 9000;
    // the MAC address of the machine the client resides on
    // it is used as the user id in the database
    private String mac;
    // the clients ip address used for uploading files
    private String myIP;
    // the ip address of the client that owns the required file to download
    private String remoteIP;
    // the port of the listening server of the client who will send the required file
    private int remotePort;
    // holds the path of the shared files folder
    private String sharedFolder ;
    // holds the path of the downloded files folder
    private String downloads;
    
    // CORBA objects
    private ORB orb;
    private DBHandler dbHandler;
    private org.omg.CORBA.Object obj;
    
    /**
     * This constructor sets up the CORBA objects needed for the distributed 
     * system, and sets up this client parameters.
     * 
     * calls: initialize(String[])
     *        getRef(String)
     *        narrow()
     *        setupClient()
     * 
     * @param args String[]
     * @param refFile String
     */
    public Tme2client(String[] args, String refFile) {
        initializeORB(args);
        obj = getRef(refFile);
        narrow();
        setupClient();
    }

    private void initializeORB(String[] args) {
        Properties props = getProperties();
        orb = ORB.init(args, props);
    }
    
    private org.omg.CORBA.Object getRef(String refFile) {
        
        String ref = null;
	try {
            Scanner reader = new Scanner(new File(refFile));
            ref = reader.nextLine();
        } catch (IOException ex) {
            out.println("File error: " + ex.getMessage());
            exit(2);
	}
        
        org.omg.CORBA.Object obj = orb.string_to_object(ref);
	if (obj == null) {
            out.println("Invalid IOR");
            exit(4);
	}
        
        return obj;
    }
    
    private void narrow() {
        
        dbHandler = null;
        try {
            dbHandler = DBHandlerHelper.narrow(obj);
	} catch (BAD_PARAM ex) {
            out.println("Narrowing failed");
            exit(3);
	}
    }
    
    /**
     * sets the local IP address, the MAC address of this client. Adds this\
     * client to the database and loads the settings. The settings are the
     * download files folder if it was modified the last time the user
     * was using the application
     * 
     * calls: setLocalIP()
     *        setMAC()
     *        DBHandler.addClient(String. String, int)
     *        loadSettings()
     */
    private void setupClient() {
        setLocalIP();
        setMAC();
        dbHandler.addClient(mac, myIP, SERVER_PORT);
        loadSettings();
    }
    
    /**
     * sets up the shared folder to "p2psharedfolder" in the current directory,
     * and loads the folder path where the downloads are saved.
     */
    private void loadSettings() {
        try {
            // set the sharedFolder path
            sharedFolder = new File(".").getCanonicalPath() + "\\p2psharedfolder";
        } catch (IOException ex) {
            System.out.println("Tme2client: loadSettings(): "
                        + "IO Exception " + ex.getMessage());
        }
        try {
            // read the downloads folder path from the downloadsFolderSettings
            // file.
            FileInputStream fis = new FileInputStream(new File(".").getCanonicalPath()
                    + "\\settings\\dowlodsFolderSettings.txt");
            int c = 0;
            StringBuilder sb = new StringBuilder();
            while((c = fis.read()) != -1) {
                sb.append((char) c);
            }
            downloads = sb.toString();
        } catch (FileNotFoundException ex) {
            try {
                downloads = new File(".").getCanonicalPath() + "\\p2pdownloads";
            } catch (IOException ex1) {
                System.out.println("Tme2client: loadSettings(): "
                        + "IO Exception " + ex.getMessage());
            }
        } catch (IOException ex) {
            System.out.println("Tme2client: loadSettings(): "
                        + "IO Exception " + ex.getMessage());
        }
    }
    
    /**
     * retrieves the machines mac address and stores it in the mac instance
     * variable.
     */
    private void setMAC() {
        NetworkInterface network = null;
        // holds the machine mac address
        byte[] mac = null;
        try {
            // scan the network interface
            network = NetworkInterface.getByInetAddress(setLocalIP());
            // retrieve the machine's mac address
            mac = network.getHardwareAddress();
        } catch (SocketException ex) {
            System.out.println("Tme2Client: setMAC(): Socket Exception "
                    + ex.getMessage());
        }
        
        // convert the mac array from byte to a string
        StringBuilder sb = new StringBuilder();
        if(mac != null) {
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
            }
            this.mac = sb.toString();
        }
    }
    
    /**
     * returns the machine's mac address
     * @return String
     */
    public String getMAC() {
        return mac;
    }
    
    /**
     * retrieves the host's IP address stores it in the myIP instance variable,
     * and returns the IP address.
     * @return InetAddress
     */
    private InetAddress setLocalIP() {
        
        // holds the host IP address
        InetAddress ip = null;
        try {
            ip = InetAddress.getLocalHost();
            String localIP = ip.toString();
            // store only the IP address which starts after the first "/"
            int index = localIP.indexOf("/") + 1;
            myIP = localIP.substring(index);
        } catch (UnknownHostException ex) {
            System.out.println("Tme2Client: setLocalIP(): Unknowns Host Exception "
                    + ex.getMessage());
        }
        
        return ip;
    }
    
    /**
     * returns the remote host IP address, where the file will be downloaded
     * from.
     * 
     * @return String
     */
    public String getRemoteIP() {
        return remoteIP;
    }
    
    /**
     * returns the remote host port number, where the file will be downloaded 
     * from
     * @return int
     */
    public int getRemotePort() {
        return remotePort;
    }
    
    /**
     * shares the file represented by the parameter filename.
     * calls: DBHandler.shareFile(String, String, String)
     * 
     * @param filename represents the file to be downloaded
     */
    public void shareFile(String filename) {
        // calculate the file size
        String size = getFileSize(filename);
        // share the file
        dbHandler.shareFile(mac, filename, size);
    }
    
    /**
     * calculates the file size and returns it in the format: x B, x KB, x MB and
     * x GB, where x is a number.
     * 
     * @param filename represented by the the parameter filename
     * @return String
     */
    public String getFileSize(String filename) {
        // create a file from the filename argument
        File file = new File(sharedFolder + "\\" + filename);
        // we use these to represent the units
        String[] units = {"B", "KB", "MB", "GB"};
        // used to locate which units is appropriate
        int index = 0;
        // check if the file exists
        if(file.exists()) {
            // retrieve the file size in bytes
            double size =  file.length();
            // file size is compared with 1024 because:
            // 1 KB = 1024 Bytes
            // 1 MB = 1024 KB
            // 1 GB = 1024 MG
            while(size > 1024 && index < 3) {
                size /= 1024;
                index++;
            }
            // round the result to 2 decimal places
            size = Math.round(size * 100) / 100;
            
            return size + " " + units[index];
        }
        
        return "0 B";
    }
    
    /**
     * sets the sharedFolder instance variable to the value of the parameter
     * sharedFolder.
     * 
     * @param sharedFolder
     */
    public void setSharedFolder(String sharedFolder) {
        this.sharedFolder = sharedFolder;
    }
    
    /**
     * returns the value of the sharedFolder instance variable
     * @return String
     */
    public String getSharedFolder() {
        return sharedFolder;
    }
    
    /**
     * sets the downloads instance variable to the value of the parameter.
     * 
     * @param downlaods
     */
    public void setDownloadsFolder(String downlaods) {
        this.downloads = downloads;
    }
    
    /**
     * returns the value of the downloads instance variable
     * @return String
     */
    public String getDownloadsFolder() {
        return downloads;
    }
    
    /**
     * removes the file from being shared.
     * calls: DBHandler.removeFile(String, String)
     *
     * @param filename
     */
    public void removeFile(String filename) {
        dbHandler.removeFile(filename, mac);
    }
    
    /**
     * searches the file represented by the parameter filename. The mac parameter
     * is passed so that files retrieved from the database are not shared by
     * the same host who is searching.
     * 
     * @param filename
     * @param mac
     * @return String[]
     */
    public String[] search(String filename, String mac) {
        // retrieve the search results
        String searchResult = dbHandler.search(filename, mac);
        // holds the records of the search result
        String[] records = null;
        // hceck if the search result has any records in it
        if(searchResult != null && searchResult.length() > 0) {
            // the search result are of the format:
            // filename,size,mac;filename,size,mac
            // so we split the records
            records = searchResult.split(";");
        }
        
        return records;
    }
    
    /**
     * retrieve the remote host info which is the IP and port number.
     * 
     * @param filename
     * @param mac
     */
    public void getRemoteHostInfo(String filename, String mac) {
        // store the remote host into in the format:
        // IP,port
        String downloadInfo = dbHandler.downloadFile(filename, mac);
        // if the host exists
        if(downloadInfo != null) {
            // split the info
            String[] info = downloadInfo.split(",");
            // retrive the IP
            remoteIP = info[0];
            // retrieve the port number
            remotePort = Integer.parseInt(info[1]);
        }
    }
    
    /**
     * downloads the file from the remote host.
     * 
     * Calls: requestFile()
     *        saveFile(Socket, String)
     * @param filename
     * @param remoteIP
     * @param remotePort
     * @return Socket to the remote host
     */
    public Socket DownloadFile(String filename, String remoteIP, int remotePort) {
        // create a socket to the remote host
        Socket socket = requestFile(filename, remoteIP, remotePort);
        // save the file on disk
        saveFile(socket, filename);
        
        return socket;
    }
    
    /**
     * creates a socket to the remote host, sends the filename that needs
     * to be downloaded to the remote host, and returns a socket to the 
     * remote host. 
     * 
     * @param filename
     * @param remoteIP
     * @param remotePort
     * @return Socket
     */
    public Socket requestFile(String filename, String remoteIP, int remotePort) {
        
        Socket socket = null;
        try {
            // create a socket to the remote host
            socket = new Socket(remoteIP, remotePort);
            // send the filename wanted
            OutputStream outStream = socket.getOutputStream();
            Writer output = new OutputStreamWriter(outStream);
            // the "*" represents end of message, assuming no file name has a "*"
            output.write(filename + "*");
            output.flush();
        } catch (UnknownHostException ex) {
            System.out.println("Tme2client: DownloanFile(): "
                    + "Unknown Host Exception: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Tme2client: DownloanFile(): "
                    + "IO Exception Exception: " + ex.getMessage());
        }
        
        return socket;
    }
    
    /**
     * 
     * @param clientSock
     * @param filename
     * @throws IOException 
     */
    private void saveFile(Socket clientSock, String filename) {
        
        InputStream is = null;
        try {
            // represents the bytes read from the socket
            int bytesRead;
            // represents the current total bytes read from the socket
            int current = 0;
            // used to write to the file being saved on disk
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            Socket sock = clientSock;
            
            // receive file, and save it on disk
            // holds the bytes read from the socket. There is a MAX of bytes
            // this can be changed depending on the host's memory capacity.
            byte [] bytearray  = new byte [MAX];
            // setup the streams
            is = sock.getInputStream();
            fos = new FileOutputStream(downloads + "\\" + filename);
            bos = new BufferedOutputStream(fos);
            // read the file bytes to the array
            bytesRead = is.read(bytearray,0,bytearray.length);
            current = bytesRead;
            do {
                bytesRead =
                        is.read(bytearray, current, (bytearray.length-current));
                if(bytesRead >= 0) {
                    current += bytesRead;
                }
            } while(bytesRead > -1);
            // savew the file into disk
            bos.write(bytearray, 0 , current);
            // close the streams
            bos.flush();
            fos.close();
            bos.close();
            sock.close();
        } catch (IOException ex) {
            System.out.println("Tme2client: saveFile(): "
                    + "IO Exception Exception: " + ex.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                System.out.println("Tme2client: saveFile(): "
                    + "IO Exception Exception: " + ex.getMessage());
            }
        }
        
    }
    
    /**
     * check if a file is shared
     *
     * @param filename
     * @return boolean
     */
    public boolean isShared(String filename) {
            return dbHandler.isShared(filename, mac);
        }
    
    /**
     * starts the host's server to server other client's requests
     */
    public void startServer() {
        ExecutorService pool = Executors.newFixedThreadPool(10);
	// holds the InetAddress of the server
	InetAddress local = null;
        String localhost = myIP;
    	try {
            local = InetAddress.getByName(localhost);
        } catch (UnknownHostException ex) {
            System.out.println("Server: Unknown Host \"" + local + "\"");
        }
		
        try (ServerSocket server = new ServerSocket(SERVER_PORT, MAX_REQ, local)) {
            System.out.println("Server: " + server.getInetAddress() 
                    + "\tPort: " + server.getLocalPort());
            System.out.println("Accepting Connections...");
            
            while(true) {
		try {
                    Socket connection = server.accept();
                    System.out.println();
                    System.out.println("Accepted connection...");
                    System.out.println("Client: " + connection.getInetAddress()
                            + "\tPort: " + connection.getPort());
                    pool.submit(new Handler(connection) {});
                } catch (IOException ex) {
                    System.out.println("Exception accepting connection" + ex);
		} catch (RuntimeException ex) {
                    System.out.println("Unexpected error" + ex);
		}
            }
	} catch (IOException ex) {
            System.out.println("Could not start server" + ex);
	}
    }
    
    
    /**
     * handles client's requests by sending wanted files to them.
     */
    private class Handler implements Callable<Void> {

        // the client's socket
        private final Socket connection;

        public Handler(Socket connection) {
        	this.connection = connection;
        }
	
        @Override
	public Void call() {
            
            // read the wanted file name from the client
            String filename = readRequestedFile();
            
            // send the requested file
            uploadFile(filename.toString());

            return null;
        }
        
        /**
         * reads the client requested file name and returns it.
         * 
         * @return String
         */
        public String readRequestedFile() {
            InputStream in = null;
            try {
                in = new BufferedInputStream(connection.getInputStream());
            } catch (IOException ex) {
                System.out.println("Tme2client: Handler: call(): "
                        + "IO exception getting input stream" + ex.getMessage());
            }
            int c;
            StringBuilder filename = new StringBuilder();
            try {
                while((c = in.read()) != 42) {
                    filename.append((char) c);
                }
            } catch (IOException ex) {
                System.out.println("Tme2client: Handler: call(): "
                        + "IO exception reading filename" + ex.getMessage());
            }
            
            return filename.toString();
        }
                
        /**
         * sends the file requested by a client
         * 
         * @param filename 
         */
        public void uploadFile(String filename) {
            try {
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                OutputStream os = null;
                
                // send file
                File myFile = new File (sharedFolder + "\\" + filename.toString());
                byte [] mybytearray  = new byte [(int) myFile.length()];
                System.out.println(mybytearray.length);
                fis = new FileInputStream(myFile);
                bis = new BufferedInputStream(fis);
                bis.read(mybytearray, 0, mybytearray.length);
                os = connection.getOutputStream();
                os.write(mybytearray,0,mybytearray.length);
                os.flush();
                
                bis.close();
                os.close();
            } catch (FileNotFoundException ex) {
                System.out.println("Tme2client: Handler: call(): uploadFile: "
                        + "File not found exception " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("Tme2client: Handler: call(): uploadFile: "
                        + "IO exception " + ex.getMessage());
            }
        }
        
        
	
        
    }
    
    
}
