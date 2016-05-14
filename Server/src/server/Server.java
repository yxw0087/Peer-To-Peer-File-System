/**
   * Author: Yee H. Wong   
   *CLID: yxw0087   
   *Class: CSCE 513   
   *Term Project 
   *Due Date: 04/28/16   
   *Description: This is a centralized P2P server that handles the communications
   *             between multiple clients.
 */

package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    public final static int portNumber = 1234;

    public static void main(String[] args) throws IOException {

        Hashtable<String, Vector<hostInfo>> fileTable = new Hashtable<String, Vector<hostInfo>>();
        Vector<hostInfo> hostList = new Vector<hostInfo>();
        Socket clientSocket = null;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(portNumber);
            while (true) {
                System.out.println("Listening...");
                try {
                    clientSocket = serverSocket.accept();
                    System.out.println("Accepted connection : " + clientSocket);
                    ServerThread serve = new ServerThread(clientSocket, fileTable, hostList);
                    serve.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Connection Error");
                }
            }
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}

class ServerThread extends Thread {

    Socket sock = null;
    Hashtable fileTable;
    hostInfo host = null;
    Vector<hostInfo> hostList;

    public ServerThread(Socket s, Hashtable h, Vector<hostInfo> hostList) {
        this.sock = s;
        this.fileTable = h;
        this.host = new hostInfo(sock.getInetAddress(), sock.getPort());
        this.hostList = hostList;
    }

    public void run() {

        try {
            
            Enumeration names = fileTable.keys();
            String inputLine, outputLine = "";
            ObjectInputStream inFromClient = new ObjectInputStream(sock.getInputStream());
            ObjectOutputStream outToClient = new ObjectOutputStream(sock.getOutputStream());
            
            outToClient.writeObject("This is a P2P server, use commands Join or Display to get started.");
            
            try {
                while ((inputLine = (String) inFromClient.readObject()) != null && !inputLine.equals("Close")) {

                    switch (inputLine) {
                        case "Join":                        
                            if (!hostList.contains(host)) {
                                outToClient.writeObject("Go ahead.");
                                try {
                                    Vector<String> fileList = new Vector<String>();
                                    fileList = (Vector<String>) inFromClient.readObject();
                                    outToClient.writeObject("Client " + sock.getPort() + " has joined the server.");
                                    hostList.add(host);
                                    for (int i = 0; i < fileList.size(); i++) {
                                        Vector<hostInfo> hosts = new Vector<hostInfo>();
                                        if (fileTable.containsKey(fileList.elementAt(i))) {
                                            hosts = (Vector<hostInfo>) fileTable.get(fileList.elementAt(i));
                                            if (!hosts.contains(host)) {
                                                hosts.add(host);
                                                fileTable.put(fileList.elementAt(i), hosts);
                                            }
                                        } else {
                                            hosts.add(host);
                                            fileTable.put(fileList.elementAt(i), hosts);
                                        }
                                    }
                                } catch (ClassNotFoundException e) {
                                }
                            } else {
                                outToClient.writeObject("Duplicate.");
                                outToClient.writeObject("Client " + sock.getPort() + " is already on the server.");
                            }
                            break;
                        case "Update":
                            if (hostList.contains(host)) {
                                outToClient.writeObject("Add or delete file? (Add/Delete): ");
                                inputLine = (String) inFromClient.readObject();
                                if (inputLine.equals("Add")) {
                                    outToClient.writeObject("Enter file path: ");
                                    inputLine = (String) inFromClient.readObject();
                                    Vector<hostInfo> hosts = new Vector<hostInfo>();
                                    if (fileTable.containsKey(inputLine)) {
                                        hosts = (Vector<hostInfo>) fileTable.get(inputLine);
                                        if (!hosts.contains(host)) {
                                            hosts.add(host);
                                            fileTable.put(inputLine, hosts);
                                        }
                                    } else {
                                        hosts.add(host);
                                        fileTable.put(inputLine, hosts);
                                    }
                                    outToClient.writeObject("File is added.");
                                    break;
                                } else if (inputLine.equals("Delete")) {
                                    outToClient.writeObject("Enter file name: ");
                                    inputLine = (String) inFromClient.readObject();
                                    if (!fileTable.containsKey(inputLine)) {
                                        outToClient.writeObject("Error");
                                        outToClient.writeObject("File is not found.");
                                        break;
                                    } else {
                                        Vector<hostInfo> hosts = new Vector<hostInfo>();
                                        hosts = (Vector<hostInfo>) fileTable.get(inputLine);
                                        if (hosts.contains(host)) {
                                            hosts.remove(host);
                                            if (hosts.isEmpty()) {
                                                fileTable.remove(inputLine);
                                            } else {
                                                fileTable.put(inputLine, hosts);
                                            }
                                            outToClient.writeObject("File is removed.");
                                        } else {
                                            outToClient.writeObject("Error");
                                            outToClient.writeObject("You do not hold this file.");
                                        }
                                        break;
                                    }
                                } else {
                                    outToClient.writeObject("Error");
                                    outToClient.writeObject("Invalid input. Use Add or Delete");
                                    break;
                                }

                            } else {
                                outToClient.writeObject("Error.");
                                outToClient.writeObject("You have not joined the server file sharing system yet, use Join.");
                            }
                            break;
                        case "Leave":
                            if (hostList.contains(host)) {
                                outToClient.writeObject("Client " + sock.getPort() + " has left the server.");
                                hostList.remove(host);
                                names = fileTable.keys();
                                while (names.hasMoreElements()) {
                                    String key = (String) names.nextElement();
                                    Vector<hostInfo> hosts = new Vector<hostInfo>();
                                    hosts = (Vector<hostInfo>) fileTable.get(key);
                                    if (hosts.contains(host)) {
                                        hosts.remove(host);
                                        if (hosts.isEmpty()) {
                                            fileTable.remove(key);
                                        }
                                    }
                                }
                            } else {
                                outToClient.writeObject("Leave is not possible since you have not joined the server.");
                            }
                            break;
                        case "Display":
                            if (fileTable.isEmpty()) {
                                outToClient.writeObject("File list is empty.");
                            } else {
                                outputLine = "";
                                names = fileTable.keys();
                                while (names.hasMoreElements()) {
                                    String key = (String) names.nextElement();
                                    Vector<hostInfo> hosts = new Vector<hostInfo>();
                                    hosts = (Vector<hostInfo>) fileTable.get(key);
                                    Vector<Integer> hostPorts = new Vector<Integer>();
                                    for (int i = 0; i < hosts.size(); i++) {
                                        hostPorts.add(hosts.elementAt(i).portNumber);
                                    }
                                    outputLine = outputLine.concat("\nFile " + key + " belongs to hosts: "
                                            + hostPorts);
                                }
                                outToClient.writeObject(outputLine);
                            }
                            break;
                        case "Request":
                            String file = (String) inFromClient.readObject();
                            if (fileTable.containsKey(file)) {
                                Vector<hostInfo> hosts = new Vector<hostInfo>();
                                hosts = (Vector<hostInfo>) fileTable.get(file);
                                Vector<Integer> hostPorts = new Vector<Integer>();
                                Vector<InetAddress> hostIPs = new Vector<InetAddress>();
                                for (int i = 0; i < hosts.size(); i++) {
                                    hostPorts.add(hosts.elementAt(i).portNumber);
                                    hostIPs.add(hosts.elementAt(i).ip);
                                }
                                outToClient.writeObject("File found.");
                                outToClient.writeObject(hostPorts);
                                outToClient.writeObject(hostIPs);
                                outToClient.writeObject("File " + file + " belongs to hosts: "
                                        + hostPorts);
                            } else {
                                outToClient.writeObject("Error.");
                                outToClient.writeObject("File not found.");
                            }
                            break;
                        default:
                            outToClient.writeObject("Unknown command. Use Join, Display, Request, Update or Leave.");
                    }
                }
            } catch (ClassNotFoundException e) {
            }
        } catch (IOException e) {
            
        } finally {
            try {
                if (sock != null) {
                    sock.close();
                    System.out.println("Connection to " + sock + " is closed.");

                    // Clean up
                    if (hostList.contains(host)) {
                        hostList.remove(host);
                        Enumeration names = fileTable.keys();
                        names = fileTable.keys();
                        while (names.hasMoreElements()) {
                            String key = (String) names.nextElement();
                            Vector<hostInfo> hosts = new Vector<hostInfo>();
                            hosts = (Vector<hostInfo>) fileTable.get(key);
                            if (hosts.contains(host)) {
                                hosts.remove(host);
                                if (hosts.isEmpty()) {
                                    fileTable.remove(key);
                                }
                            }
                        }
                    }
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
