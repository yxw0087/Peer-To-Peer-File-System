To run the server, run java -jar "filepath\Server.jar" on command line.

To run the client, run java -jar "filepath\Client.jar" on command line.


Where: filepaths are the paths where you put Server and Client.jars. By default they will be in Client/dist or Server/dist folders.


User interface:

First, run the server on a host machine. Then change the target hostname in the client module to match the IP address of the machine hosting the server in order for the client to connect successfully to the server. After that, run the client on client machine(s). 

The server will request the client to enter the file path that stores his files. All file information in this path including the ones in any subfolder will be stored in the server file table when the client uses "Join" command.
(Without using Join, the client will still be connected to the server and be able to use Request command, but its file will not appear in the server's sharing list, in other word, this client will only be downloading without uploading).

Other commands:

Leave: Leaves the file sharing system and close the connection to the server. All records in the file table that belong to this client will be erased.

Update: Updates list of file to be stored in the sharing list.

Request: Requests a file from server, if found, server will return a list of file holders to the requestor. Then the client will automatically pick the first one in the list and attempt a connection to the file holder. The file holder will always choose to accept the request and send the file. Upon receiving the file, requestor will be asked to enter a path to save this file. Upon fulfilling the Request, both the requestor and file holder will close their connection automatically.

Display: ONLY used for debugging purpose, displays a whole list of files and their owners in the server's file table.