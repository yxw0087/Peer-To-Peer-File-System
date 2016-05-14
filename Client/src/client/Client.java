/**
   *Author: Yee H. Wong
  *CLID: yxw0087
  *Class: CSCE 513
  *Term Project
   *Due Date: 04/28/16
  *Description: This is a general module for a peer in the P2P network..
 */

package client;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

public class Client {

    public final static int portNumber = 1234;
    public final static String hostName = "localhost";
    static Vector<String> myFilePaths = new Vector<String>();
    static Vector<String> myFileNames = new Vector<String>();
    static Hashtable<String, hostInfo> localFileTable = new Hashtable<String, hostInfo>();

    public static void main(String[] args) throws Exception {

        Scanner reader = new Scanner(System.in);
        System.out.println("Enter a directory to read files: ");
        String path = reader.nextLine();
        final File folder = new File(path);
        readFolder(folder, myFilePaths);

        for (int i = 0; i < myFilePaths.size(); i++) {
            System.out.println(myFilePaths.elementAt(i));
            File file = new File(myFilePaths.elementAt(i));
            myFileNames.add(file.getName());
            System.out.println(myFileNames.elementAt(i));
            /*byte[] buffer = new byte[(int) file.length()];
             FileInputStream fis = new FileInputStream(file);
             fis.read(buffer);
             fis.close();
             MessageDigest md = MessageDigest.getInstance("MD5");
             md.update(buffer);
             byte[] digest = md.digest();
             System.out.println("MD5 fingerprint: " + ByteBuffer.wrap(digest).getInt());*/
        }

        Socket sock = null;
        ObjectOutputStream outToServer = null;
        ObjectInputStream inFromServer = null;

        try {
            sock = new Socket(hostName, portNumber);

            System.out.println("Connecting...");

            // Start a new thread for listening connection(s) from other client
            ClientServer clientServe = new ClientServer(sock.getLocalPort(), myFilePaths, myFileNames);
            clientServe.start();

            // And contnue to connect to server
            // Get standard user input
            BufferedReader stdIn
                    = new BufferedReader(new InputStreamReader(System.in));
            String fromServer;
            String fromUser;

            // Object input and output streams
            outToServer = new ObjectOutputStream(sock.getOutputStream());
            inFromServer = new ObjectInputStream(sock.getInputStream());

            // Begin interaction with the server
            try {
                fromServer = (String) inFromServer.readObject();
                System.out.println("Server: " + fromServer);
                while (true) {
                    fromUser = stdIn.readLine();
                    if (fromUser.equals("Close")) {
                        outToServer.writeObject(fromUser);
                        break;
                    }
                    if (fromUser != null) {
                        System.out.println("Client: " + fromUser);

                        if (fromUser.equals("Join")) {
                            outToServer.writeObject(fromUser);
                            if (inFromServer.readObject().equals("Go ahead.")) {
                                outToServer.writeObject(myFileNames);
                            }
                            fromServer = (String) inFromServer.readObject();
                            System.out.println("Server: " + fromServer);
                        } else if (fromUser.equals("Update")) {
                            outToServer.writeObject(fromUser);                        
                            fromServer = (String) inFromServer.readObject();
                            if (fromServer.equals("Add or delete file? (Add/Delete): ")) {
                                System.out.println("Server: " + fromServer);
                                fromUser = stdIn.readLine();
                                outToServer.writeObject(fromUser);
                                fromServer = (String) inFromServer.readObject();
                                System.out.println("Server: " + fromServer);
                                
                                // Warning: User has to input file that really exists or it will create a fake entry                                
                                String filepath = stdIn.readLine();
                                if (fromUser.equals("Add")) {
                                    File newfile = new File(filepath);
                                    myFilePaths.add(filepath);
                                    myFileNames.add(newfile.getName());
                                    outToServer.writeObject(newfile.getName());
                                } else if (fromUser.equals("Delete")) {
                                    if (!myFileNames.contains(filepath)) {
                                        outToServer.writeObject(filepath);
                                        fromServer = (String) inFromServer.readObject();
                                        // Displays error message from server - file is not found
                                        System.out.println("Server: " + fromServer);
                                    } else {
                                        myFilePaths.removeElementAt(myFileNames.indexOf(filepath));
                                        myFileNames.remove(filepath);
                                        outToServer.writeObject(filepath);
                                    }
                                }                                                         
                            }
                            fromServer = (String) inFromServer.readObject();
                            // Displays confirmation or error message from server
                            System.out.println("Server: " + fromServer);
                        } else if (fromUser.equals("Request")) {
                            System.out.println("Enter the desired file name: ");
                            String receivePath = stdIn.readLine();
                            
                            // If client already knows file holder
                            if (localFileTable.containsKey(receivePath)) {
                                try {
                                    System.out.println("File holder known to be " + localFileTable.get(receivePath).ip);
                                    System.out.println("Connecting to host " + localFileTable.get(receivePath).ip + "...");
                                    Socket sock2 = new Socket(localFileTable.get(receivePath).ip, localFileTable.get(receivePath).portNumber - 2345);
                                    System.out.println("Connection with " + localFileTable.get(receivePath).ip + " is established on port " + localFileTable.get(receivePath).portNumber);
                                    requestFile(sock2, receivePath);
                                } catch (IOException e) {
                                    System.err.println("Target is offline.");
                                    localFileTable.remove(receivePath);
                                }
                            } else {
                                // Send request command
                                outToServer.writeObject(fromUser);
                                // Send file name
                                outToServer.writeObject(receivePath);
                                if (inFromServer.readObject().equals("File found.")) {

                                    // Receives a list of host(s) that hold this file from server
                                    Vector<Integer> targetPorts = (Vector<Integer>) inFromServer.readObject();
                                    Vector<InetAddress> targetIPs = (Vector<InetAddress>) inFromServer.readObject();
                                    fromServer = (String) inFromServer.readObject();
                                    System.out.println("Server: " + fromServer);

                                    // Connects to the first host in the provided list
                                    int targetPort = targetPorts.firstElement();
                                    InetAddress targetIP = targetIPs.firstElement();
                                    try {
                                        System.out.println("Connecting to host " + targetPort + "...");
                                        Socket sock2 = new Socket(targetIP, targetPort - 2345);
                                        System.out.println("Connection with " + targetIP + " on port " + targetPort + " is established.");
                                        requestFile(sock2, receivePath);
                                    } catch (IOException e) {
                                        System.err.println("Target is offline.");
                                    }
                                    hostInfo newHostInfo = new hostInfo(targetIP, targetPort);
                                    localFileTable.put(receivePath, newHostInfo);
                                    // Update list of files to the server
                                    outToServer.writeObject("Update");
                                    fromServer = (String) inFromServer.readObject();
                                    if (fromServer.equals("Add or delete file? (Add/Delete): ")) {
                                        outToServer.writeObject("Add");
                                        fromServer = (String) inFromServer.readObject();
                                        if (fromServer.equals("Enter file path: ")) {
                                            outToServer.writeObject(receivePath);
                                        }
                                        fromServer = (String) inFromServer.readObject();
                                        System.out.println("Server: " + fromServer);
                                    }
                                } else {
                                    // File not found error
                                    fromServer = (String) inFromServer.readObject();
                                    System.out.println("Server: " + fromServer);
                                }
                            }
                        } else {
                            outToServer.writeObject(fromUser);
                            fromServer = (String) inFromServer.readObject();
                            System.out.println("Server: " + fromServer);
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to "
                    + hostName);
            System.exit(1);
        } finally {
            if (sock != null) {
                sock.close();
                System.out.println("Connection to server is closed.");
                System.exit(0);
            }
            if (outToServer != null) {
                outToServer.close();
            }
            if (inFromServer != null) {
                inFromServer.close();
            }
        }
    }

    /*
     Reads all the files contain in an user input directory and add them to myFiles
     */
    public static void readFolder(final File folder, Vector<String> myFiles) {
        
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                readFolder(fileEntry, myFiles);
            } else {
                myFiles.add(fileEntry.getPath());
            }
        }
    }

    public static void requestFile(Socket sock, String filePath) throws Exception {
        
        FileOutputStream outFile = null;
        BufferedOutputStream buff = null;
        ObjectOutputStream outToClient = null;
        ObjectInputStream inFromClient = null;
        BufferedReader stdIn
                = new BufferedReader(new InputStreamReader(System.in));

        outToClient = new ObjectOutputStream(sock.getOutputStream());
        inFromClient = new ObjectInputStream(sock.getInputStream());

        outToClient.writeObject("Request to download file.");        
        outToClient.writeObject(filePath);
        long fileSize = (long) inFromClient.readObject();
        byte[] mybytearray = new byte[(int) fileSize];
        InputStream is = sock.getInputStream();
        System.out.println("Enter the directory to save this file: ");
        String savePath = stdIn.readLine();
        File newFile = new File(savePath);
        /*newFile.mkdirs();
         System.out.println("Give a name to the downloaded file: ");
         savePath = stdIn.readLine();*/
        outFile = new FileOutputStream(savePath);
        buff = new BufferedOutputStream(outFile);
        int bytesRead = is.read(mybytearray, 0, mybytearray.length);
        int current = bytesRead;

        do {
            bytesRead
                    = is.read(mybytearray, current, (mybytearray.length - current));
            if (bytesRead >= 0) {
                current += bytesRead;
            }
        } while (current < fileSize);

        buff.write(mybytearray, 0, current);
        buff.flush();
        System.out.println("File downloaded and saved at " + savePath + " (" + current + " bytes read)");
        myFilePaths.add(savePath);
        myFileNames.add(filePath);        

        if (outFile != null) {
            outFile.close();
        }
        if (buff != null) {
            buff.close();
        }
        if (sock != null) {
            sock.close();
            System.out.println("Connection to " + sock + " is closed.");
        }

    }
}

class ClientServer extends Thread {

    Socket clientSocket = null;
    ServerSocket serverSocket = null;
    int port;
    Vector<String> myFilePaths;
    Vector<String> myFileNames;

    public ClientServer(int port, Vector<String> myFilePaths, Vector<String> myFileNames) {
        this.port = port;
        this.myFileNames = myFileNames;
        this.myFilePaths = myFilePaths;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(port - 2345);
            while (true) {
                try {
                    clientSocket = serverSocket.accept();
                    System.out.println("Accepted connection : " + clientSocket);
                    ClientThread client = new ClientThread(clientSocket, myFilePaths, myFileNames);
                    client.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Connection Error");

                }
            }
        } catch (IOException e) {
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

class ClientThread extends Thread {

    Vector<String> myFilePaths;
    Vector<String> myFileNames;
    ObjectOutputStream outToClient = null;
    ObjectInputStream inFromClient = null;
    FileInputStream inFile = null;
    BufferedInputStream buff = null;
    OutputStream out = null;
    Socket sock;
    BufferedReader stdIn
            = new BufferedReader(new InputStreamReader(System.in));

    public ClientThread(Socket sock, Vector<String> myFilePaths, Vector<String> myFileNames) {
        this.sock = sock;
        this.myFileNames = myFileNames;
        this.myFilePaths = myFilePaths;
    }

    public void run() {
        try {
            outToClient = new ObjectOutputStream(sock.getOutputStream());
            inFromClient = new ObjectInputStream(sock.getInputStream());
            String fromClient = (String) inFromClient.readObject();
            System.out.print("Client " + sock.getPort() + ": ");
            System.out.println(fromClient);
                //System.out.println("Your response (Yes/No): ");
            //String response = stdIn.readLine();
            //outToClient.writeObject(response);
            //if (response.equals("Yes")){ 
            String sendName = (String) inFromClient.readObject();
            String sendPath = myFilePaths.elementAt(myFileNames.indexOf(sendName));
            File myFile = new File(sendPath);
            outToClient.writeObject(myFile.length());
            byte[] mybytearray = new byte[(int) myFile.length()];
            inFile = new FileInputStream(myFile);
            buff = new BufferedInputStream(inFile);
            buff.read(mybytearray, 0, mybytearray.length);
            out = sock.getOutputStream();
            System.out.println("Sending " + sendPath + "(" + mybytearray.length + " bytes)");
            out.write(mybytearray, 0, mybytearray.length);
            out.flush();
            System.out.println("Done.");
            //}
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            try {
                if (sock != null) {
                    sock.close();
                    System.out.println("Connection to " + sock + " is closed.");
                }
            } catch (IOException e) {

            }
        }
    }
}

class hostInfo {

    InetAddress ip;
    int portNumber;

    public hostInfo(InetAddress ip, int portNumber) {
        this.ip = ip;
        this.portNumber = portNumber;
    }
}