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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import spread.*;

/**
 *
 * @author Vladyslav
 */
public class AlcatrazServer implements AdvancedMessageListener, Serializable, Remote {

    public String _serverHost = "localhost";
    public int _spreadPort = 13335;
    public String _serverName = "RegServer";
    public String _backupServer = "backupServer";
    public AlcatrazServerImpl server;
    public boolean primary;
    LinkedList<IRMIClient> buf;
    //SpreadConnection connection;
    SpreadConnection con = new SpreadConnection();
    String GroupName = "AlcatrazSpreadGroup";
    
    
    public void init() throws RemoteException, IOException, SpreadException {
        server = new AlcatrazServerImpl();
        String ownIP = null;

        /*
        //Liest IP von Primary und Backup und Spread Gruppenname aus config.txt File ein.
        FileReader fr = new FileReader("src/common/config.txt");
        BufferedReader br = new BufferedReader(fr);
        String ipprimary = br.readLine();
        String ipbackup = br.readLine();
        GroupName = br.readLine();
        br.close();
        */
        
        try {       
        InetAddress address = InetAddress.getLocalHost(); 
        ownIP = address.getHostAddress() ;   
        System.out.println("Host "+ownIP+" wird gestartet...");
        }   catch(UnknownHostException e) {
            e.printStackTrace();
        }

            //RMI Bind zu RMI Reg damit Server von Client aus zu erreichen ist
          /*System.out.println("Registration Server wird geladen...");
          RegServerImpl stub = null;
        try {      
          stub = new RegServerImpl(con);
          Naming.rebind("rmi://"+ownIP+":1099/RegServer", stub);
          System.out.println("Server ist bereit!");
            } catch (Exception e) {
            System.out.println("Laden des Servers fehlgeschlagen...");
            e.printStackTrace();
          }*/

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
    
    public void sendBackup(AlcatrazServerImpl.spreadObj backup) throws RemoteException, SpreadException {
        SpreadMessage msg = new SpreadMessage();
        msg.setObject(backup);
        msg.addGroup(GroupName);
        msg.setReliable();
        msg.setSelfDiscard(false);
        try {
            con.multicast(msg);
        } catch (SpreadException e) {
            System.err.println("Could not send message from Method sendBackup....");
        }        
    }
    

    public static void main(String args[]) throws IOException, SpreadException {
        AlcatrazServer s = new AlcatrazServer();
        

        System.out.println("Starting registration server...");
        Registry registry;
        //trying to create registry
        try {
            s.init();
            LocateRegistry.createRegistry(1099);
            Naming.rebind("rmi://" + s._serverHost + ":" + "1099" + "/" + s._serverName, s.server);
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



    public class AlcatrazServerImpl extends UnicastRemoteObject implements IAlcatrazServer {
        
        private HashMap<String, IRMIClient> queue[];
        private HashMap<String, Integer> regNames;
        
        
        public AlcatrazServerImpl() throws RemoteException {
            super();
            //queue[0] = new HashMap<>();
            //queue[1] = new HashMap<>();
            //queue[2] = new HashMap<>();
        }
        
        public class spreadObj implements Serializable {
            HashMap<String, IRMIClient> queue[];
            HashMap<String, Integer> regNames;            
        }

        @Override
        public LinkedList<String> register(IRMIClient p, String name,int playercount) throws RemoteException {
            
            if (queue[playercount-1].containsKey(name) || regNames.containsKey(name)) {
                throw new RemoteException("Already registered in queue.");
            }
            int i = queue[playercount-1].size();
            queue[playercount-1].put(name, p);
            regNames.put(name, playercount - 1);
            System.out.println("Player " + name + " joined.");
            //Backup
            spreadObj backup = new spreadObj();
            backup.queue = queue;
            backup.regNames = regNames;
            try {
                sendBackup(backup);
            } catch (SpreadException ex) {
                Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            return new LinkedList<>(queue[playercount-1].keySet());
        }

        @Override
        public int unregister(String name) throws RemoteException {
            if (!regNames.containsKey(name)) {
                return -1;
            }
            queue[regNames.get(name)].remove(name);
            regNames.remove(name);
            System.out.println("Player " + name + " left.");
            //Backup
            spreadObj backup = new spreadObj();
            backup.queue = queue;
            backup.regNames = regNames;
            try {
                sendBackup(backup);
            } catch (SpreadException ex) {
                Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
        }

        public boolean isTeamReady(String name) throws RemoteException {
            return queue[regNames.get(name)].size() >= regNames.get(name)+1;
        }
        
        /**
         * sends the list of other players remote interfaces, who are also in queue
         *
         * @param p
         * @return
         * @throws RemoteException
         */
        public HashMap<String, IRMIClient> start(String name) throws RemoteException {
            int playerqueue = regNames.get(name);
            if (queue[playerqueue].size() < regNames.get(name)+1) {
                return null;
            }
            HashMap<String, IRMIClient> res = new HashMap<>(queue[playerqueue]);
            res.remove(name);
            return res;
        }
        
        //Methods for receiving Spread messages
        public void regularMessageReceived(SpreadMessage message) {        
            try {
             String verify = message.getObject().toString();
              if (verify.contains("synch")){
                  System.out.println("---NEW MESSAGE");
                  System.out.println("New synchronisation message from "+message.getSender()+"received");
                  System.out.println("Message "+message.getClass()+", Content: "+message.getObject());
                  System.out.println("---END OF MESSAGE");
                  SpreadMessage send_teamlist = new SpreadMessage();
                  spreadObj TeamsList = new spreadObj();
                  TeamsList.queue = queue;
                  TeamsList.regNames = regNames;
                  send_teamlist.setObject(TeamsList);
                  send_teamlist.addGroup(GroupName);
                  send_teamlist.setReliable();
                  send_teamlist.setSelfDiscard(false);
                  try {
                     con.multicast(send_teamlist);
                  } catch (SpreadException e) {
                      System.out.println("Sending team message failed..."+e);
                  }
              }

              //Liste mit Teams empfangen. Nr wird geprüft für Synchronisation.
              //Es kann auch eine Message für ein Backup sein (dann bräuchte man VersionsNr nicht prüfen aber es macht auch nichts wenn es gerüft wird
              else if (verify.contains("TeamsList")){
                  System.out.println("---NEW MESSAGE");
                  System.out.println("New List of teams from "+message.getSender()+"received. Checking synchronisation...");
                  System.out.println("Message "+message.getClass()+"; "+message.getObject());
                  System.out.println("---END OF MESSAGE");
                  spreadObj list_received = new spreadObj();
                  list_received = (spreadObj) message.getObject();
                  
                  //Synchronisieren ??
                  queue = list_received.queue;
                  regNames = list_received.regNames;
                  
                  /*
                  System.out.println("...Synchronisation");
                  System.out.println("...Empfangene Version: "+list_received.getNr());
                  System.out.println("...Eigene Version    : "+own_list.getNr());
                    if (list_received.getNr() > own_list.getNr()) {
                        own_list = list_received;  //eigene Liste wird mit Liste received überschrieben
                        System.out.println("...Liste wurde aktualisiert. Neue Version: "+own_list.getNr());
                    }
                    else if (list_received.getNr() <= own_list.getNr()) {
                        System.out.println("...Kein Synch notwendig.");
                    } */
              } 
              else {
                System.out.println("Unknown message");
              }

          } catch (SpreadException ex) {
              Logger.getLogger(RegServer.class.getName()).log(Level.SEVERE, null, ex);
          }
        }

        public void membershipMessageReceived(SpreadMessage message) {
            //not used
        }

        
        
    }
}
