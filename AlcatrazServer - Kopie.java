/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import common.APlayer;
import common.IAlcatrazServer;
import common.IRMIClient;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import spread.*;

/**
 *
 * @author Florian
 */
public class AlcatrazServer implements Serializable, Remote {

    public String _serverHost = "localhost";
    public int _spreadPort = 13335;
    public String _serverName; /*= "RegServer";*/
    //public String _backupServer = "backupServer";
    public AlcatrazServerImpl server;
    private ServerSpread spread;
    //public boolean primary;
    LinkedList<IRMIClient> buf;
    //SpreadConnection connection; 
    
    public AlcatrazServer(){
    //  server.state = new ServerState();
    }
    
    public void init() throws RemoteException, IOException, SpreadException {
        server = new AlcatrazServerImpl();
        ServerSpread spreadserver = new ServerSpread();
        spreadserver.init(server.state);
    }
    

    public static void main(String args[]) throws IOException, SpreadException {
        AlcatrazServer s = new AlcatrazServer();
        //ServerSpread 

        System.out.println("Starting registration server...");
        Registry registry;
        //trying to create registry
        try {
            s.init();  //starts ServerRMI 
            LocateRegistry.createRegistry(1099);
            Naming.rebind("rmi://" + s._serverHost + ":" + "1099" + "/" + s._serverName, s.server);
            System.out.println("rmi://" + s._serverHost + ":" + "1099" + "/" + s._serverName + s.server);
        } catch (RemoteException | MalformedURLException ex) {
            Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Server could not be initialized");
            return;
        }
        System.out.println("Server up and running");
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            if ("exit".equals(sc.next())) {
                break;
            }
        }
    }
    
    private class ServerSpread implements AdvancedMessageListener, Serializable {
        public String _serverHost;
        public int _spreadPort = 13335;
        public String _serverName; /*= "RegServer";*/
        //public String _backupServer = "backupServer";
        //public boolean primary;
        LinkedList<IRMIClient> buf;
        //SpreadConnection connection;
        SpreadConnection con = new SpreadConnection();
        String GroupName; /*= "AlcatrazSpreadGroup";*/


        public void init(ServerState state) throws RemoteException, IOException, SpreadException {
            //server = new AlcatrazServer.AlcatrazServerImpl();
            String ownIP = null;

            try {       
            InetAddress address = InetAddress.getLocalHost(); 
            ownIP = address.getHostAddress() ;   
            System.out.println("Host "+ownIP+" wird gestartet...");
            }   catch(UnknownHostException e) {
                e.printStackTrace();
            }
            _serverHost = ownIP;

            //Liest IP von Primary und Backup und Spread Gruppenname aus config.txt File ein.
            FileReader fr = new FileReader("src/common/config.txt");
            BufferedReader br = new BufferedReader(fr);
            GroupName = br.readLine();
            String srv1_ip = br.readLine();
            String srv1_name = br.readLine();
            String srv2_ip = br.readLine();
            String srv2_name = br.readLine();
            br.close();
            System.out.println("Reading Spread config... "+srv1_name+": "+srv1_ip+"; "+srv2_name+": "+srv2_ip+"; ");

            if (ownIP.equals(srv1_ip)) {
                System.out.println("I am Primary Server!");
                _serverName = srv1_name;    
            }
            else if (ownIP.equals(srv2_ip)){
                System.out.println("I am Backup Server1");
                _serverName = srv2_name;
            }
            else {
                System.out.println("IP-Address not in Spread config");
            }

           //Connect to Spread Deamon
           try {
               con.add(this);
               con.connect(InetAddress.getByName(ownIP), 0, GroupName, false, true);
               System.out.println("Connected to local Spread Daemon");
           } catch (SpreadException | UnknownHostException exc) {
               System.out.println("Connection Spread Deamon failed");
                exc.printStackTrace();
           }

           //Neue Gruppe
           SpreadGroup group = new SpreadGroup();

           //Join 
            try {
                group.join(con, GroupName);
               System.out.println("Successfully joined group: "+GroupName);
            } catch (SpreadException exc) {
               System.out.println("Join failed");       
                exc.printStackTrace();
            }

            System.out.println("Sending request for synchronisation...");
            sendSynchMsg("synch");


        }   //end of init

        public void sendSynchMsg(String synch) throws RemoteException, SpreadException {
            SpreadMessage msg = new SpreadMessage();
            msg.setObject(synch);
            msg.addGroup(GroupName);
            msg.setReliable();
            msg.setSelfDiscard(false);
            try {
                con.multicast(msg);
            } catch (SpreadException e) {
                System.err.println("Could not send message from Method sendHelloMsg....");
            }
        }

        public void sendBackup() throws  SpreadException {
            SpreadMessage msg = new SpreadMessage();
            msg.setObject(server.state);
            msg.addGroup(GroupName);
            msg.setReliable();
            msg.setSelfDiscard(false);
            try {
                con.multicast(msg);
                System.out.println("...ServerState "+msg.getObject()+" was sended within SpreadGroup");
            } catch (SpreadException e) {
                System.err.println("Could not send message from Method sendBackup....");
            }        
        }


         //Methods for receiving Spread messages
        @Override
        public void regularMessageReceived(SpreadMessage message) {        
            try {
                if (message.getObject()==null) {
                    System.out.println("...INFO: Empty state received and will be ignored.");
                }
                else {
                    try {
                        String verify = message.getObject().toString();
                        if (verify.contains("synch")){
                            System.out.println("---NEW MESSAGE");
                            System.out.println("New synchronisation message from "+message.getSender()+"received");
                            System.out.println("Message "+message.getClass()+", Content: "+message.getObject());
                            System.out.println("---END OF MESSAGE");
                            // send current ServerState
                            sendBackup();
                        }
                        //Liste mit Teams empfangen. Nr wird geprüft für Synchronisation.
                        //Es kann auch eine Message für ein Backup sein (dann bräuchte man VersionsNr nicht prüfen aber es macht auch nichts wenn es gerüft wird
                        //if ((x == 10) || (x == 15)) { }
                        else if (verify.contains("state")){
                            System.out.println("---NEW MESSAGE");
                            System.out.println("New List of teams from "+message.getSender()+"received. Checking synchronisation...");
                            System.out.println("Message "+message.getClass()+"; "+message.getObject());
                            System.out.println("---END OF MESSAGE");
                            ServerState state_received = new ServerState();
                            state_received = (ServerState) message.getObject();
                            System.out.println("...Synchronisation");
                            System.out.println("...Empfangene Version: "+state_received.generation);
                            System.out.println("...Eigene Version    : "+server.state.generation);
                            if (state_received.generation > server.state.generation) {
                                server.state = state_received;  //eigene Liste wird mit Liste received überschrieben
                                System.out.println("...Liste wurde aktualisiert. Neue Version: "+server.state.generation);
                            }
                            else if (state_received.generation <= server.state.generation) {
                                System.out.println("...Kein Synch notwendig.");
                            }
                        }
                        else {
                            System.out.println("Unknown message");
                        }
                        
                    } catch (SpreadException ex) {
                        Logger.getLogger(RegServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (SpreadException ex) {
                Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

            public void membershipMessageReceived(SpreadMessage message) {
                //not used
            }



    }

    
}
