/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import common.ServerState;
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
import java.util.Iterator;
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
public class AlcatrazServer implements Serializable, Remote{

    public String _serverHost;
    public int _spreadPort = 13335;
    public String _serverName; /*= "RegServer";*/
    //public String _backupServer = "backupServer";
    public AlcatrazServerImpl server;
    //public boolean primary;
    LinkedList<IRMIClient> buf;
    //SpreadConnection connection;
    private QueueUpdated queueupdater;
    
    public AlcatrazServer(){
    //  server.state = new ServerState();
    }
    
    public String init() throws RemoteException, IOException, SpreadException  {
        ServerSpread spreadserver = new ServerSpread();
        server = new AlcatrazServerImpl(spreadserver);
        server.spread.init(server.state);
        queueupdater = new QueueUpdated();
        return server.spread._serverName;
    }
    

    public static void main(String args[]) throws IOException, SpreadException {
        AlcatrazServer s = new AlcatrazServer();
        
        System.out.println("Starting registration server...");
        Registry registry;
        //trying to create registry
        try {
            s._serverName = s.init();  //starts ServerRMI 
            try {       
            InetAddress address = InetAddress.getLocalHost(); 
            s._serverHost = address.getHostAddress() ;   
            }   catch(UnknownHostException e) {
                e.printStackTrace();
            }
            LocateRegistry.createRegistry(1099);
            System.out.println("rmi://" + s._serverHost + ":" + "1099" + "/" + s._serverName);
            Naming.rebind("rmi://" + s._serverHost + ":" + "1099" + "/" + s._serverName, s.server);
            
        } catch (RemoteException | MalformedURLException ex) {
            Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Server could not be initialized");
            return;
        }
        Thread stateWatcher = new Thread(s.queueupdater);
        stateWatcher.start();
        System.out.println("Server up and running");
        
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            if ("exit".equals(sc.next())) {
                break;
            }
        }
    }
    
    public class ServerSpread implements AdvancedMessageListener, Serializable {
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
            _serverHost = null;
            try {       
            InetAddress address = InetAddress.getLocalHost(); 
            _serverHost = address.getHostAddress() ;   
            System.out.println("Host "+_serverHost+" wird gestartet...");
            }   catch(UnknownHostException e) {
                e.printStackTrace();
            }
            //Liest IP von Primary und Backup und Spread Gruppenname aus config.txt File ein.
            FileReader fr = new FileReader("src/common/config.txt");
            BufferedReader br = new BufferedReader(fr);
            GroupName = br.readLine();
            String srv1_ip = br.readLine();
            String srv1_name = br.readLine();
//            String srv2_ip = br.readLine();
//            String srv2_name = br.readLine();
//            String srv3_ip = br.readLine();
//            String srv3_name = br.readLine();
            br.close();
            System.out.println("Reading Spread config... "+srv1_name+": "+srv1_ip+"; ");//+srv2_name+": "+srv2_ip+"; "+srv3_name+": "+srv3_ip+"; ");

            if (_serverHost.equals(srv1_ip)) {
                _serverName = srv1_name;    
                System.out.println("PrimaryServer / Name within SpreadGroup is: "+_serverName);
            }
//            else if (_serverHost.equals(srv2_ip)){
//                _serverName = srv2_name;    
//                System.out.println("BackupServer / Name within SpreadGroup is: "+_serverName);
//            }else if (_serverHost.equals(srv3_ip)) {
//                _serverName = srv3_name;    
//                System.out.println("PrimaryServer / Name within SpreadGroup is: "+_serverName);
//            }
            else {
                System.out.println("IP-Address not in Spread config");
            }

           //Connect to Spread Deamon
           try {
               con.add(this);
               con.connect(InetAddress.getByName(_serverHost), 0, GroupName, false, true);
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
            msg.setSelfDiscard(true);
            try {
                con.multicast(msg);
            } catch (SpreadException e) {
                System.err.println("Could not send message from Method sendHelloMsg....");
            }
        }

        public void sendBackup() throws  SpreadException {
            SpreadMessage msg = new SpreadMessage();
            System.out.println("ServerState:" + server.state);
            msg.setObject(server.state);
            msg.addGroup(GroupName);
            msg.setReliable();
            msg.setSelfDiscard(true);
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
                            System.out.println("New synchronisation message from "+message.getSender()+"received ");
                            System.out.println("Message "+message.getClass()+", Content: "+message.getObject());
                            System.out.println("---END OF MESSAGE");
                            // send current ServerState
                            sendBackup();
                        }
                        //Liste mit Teams empfangen. Nr wird geprüft für Synchronisation.
                        //Es kann auch eine Message für ein Backup sein (dann bräuchte man VersionsNr nicht prüfen aber es macht auch nichts wenn es gerüft wird
                        //if ((x == 10) || (x == 15)) { }
                        else if (verify.contains("ServerState")){
                            System.out.println("---NEW MESSAGE");
                            System.out.println("New List of teams from "+message.getSender()+"received. Checking synchronisation...");
                            System.out.println("Message "+message.getClass()+"; "+message.getObject());
                            System.out.println("---END OF MESSAGE");
                            ServerState state_received = new ServerState();
                            state_received = (ServerState) message.getObject();
                            System.out.println("...Synchronisation");
                            System.out.println("...Empfangene Version: "+state_received.getGeneration());
                            System.out.println("...Eigene Version    : "+server.state.getGeneration());
                            if (state_received.getGeneration() > server.state.getGeneration()) {
                                server.state = state_received;  //eigene Liste wird mit Liste received überschrieben
                                System.out.println("...Liste wurde aktualisiert. Neue Version: "+server.state.getGeneration());
                            }
                            else if (state_received.getGeneration() <= server.state.getGeneration()) {
                                System.out.println("...Kein Synch notwendig.");
                            }
                        }
                        else {
                            System.out.println("Unknown message");
                        }
                        
                    } catch (SpreadException ex) {
                        Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (SpreadException ex) {
                Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
            }
                        System.out.println("ServerState:" + server.state);

        }

            public void membershipMessageReceived(SpreadMessage message) {
                //not used
            }

            

    }

    private class QueueUpdated implements Runnable{
        public void run(){
            while(true){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
                if(server.state.isQueueReadyToPlay()){
                    LinkedList<String> list = server.state.getPlayersInGame();
                    Iterator it = list.iterator();
                    while(it.hasNext()){
                        server.state.deletePlayer((String)it.next());
                    }
                }
            }
        }
    }
}
