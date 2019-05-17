
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;

public class Handler implements Callable<Void> {

        private final Socket connection;
        private String sharedFolder;

        public Handler(Socket connection) {
        	this.connection = connection;
                sharedFolder = new File(".").getAbsolutePath() + "\\p2psharedfolder";
        }
	
        @Override
	public Void call() {
            
            String filename = readRequestedFile();
            
            // send the requested file
            uploadFile(filename.toString());

            return null;
        }
        
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
                
        public void uploadFile(String filename) {
            try {
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                OutputStream os = null;
                
                // send file
                File myFile = new File (sharedFolder + "\\" + filename.toString());
                byte [] mybytearray  = new byte [(int) myFile.length()];
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