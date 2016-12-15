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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
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

    public String _serverHost;
    public int _spreadPort = 13335;
    public String _serverName; 
    public AlcatrazServerImpl server;
    LinkedList<IRMIClient> buf;
    public AlcatrazServer(){
    //  server.state = new ServerState();
    }
    
    public String init() throws RemoteException, IOException, SpreadException, FileNotFoundException, InterruptedException  {
        ServerSpread spreadserver = new ServerSpread();
        server = new AlcatrazServerImpl(spreadserver);
        server.spread.init(server.state);
        return server.spread._serverName;
    }
    

    public static void main(String args[]) throws IOException, SpreadException, FileNotFoundException, InterruptedException {
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
        System.out.println("Server up and running");
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            if ("exit".equals(sc.next())) {
                break;
            }
        }
    }
    
    public class ServerSpread implements AdvancedMessageListener, Serializable {
        public String allMembers = null;
        public String _serverHost;
        public int _spreadPort = 13335;
        public String _serverName; 
        public String _primaryServer;
        public int _numberOfServers;
        public boolean _isPrimary=false;
        LinkedList<IRMIClient> buf;
        SpreadConnection con = new SpreadConnection();
        String GroupName;


        public void init(ServerState state) throws RemoteException, IOException, SpreadException, FileNotFoundException, InterruptedException {
            //server = new AlcatrazServer.AlcatrazServerImpl();
            _serverHost = null;
            try {       
            InetAddress address = InetAddress.getLocalHost(); 
            _serverHost = address.getHostAddress() ;   
            System.out.println("Host "+_serverHost+" wird gestartet...");
            }   catch(UnknownHostException e) {
                e.printStackTrace();
            }
            //Ermittelt eigenen Namen, Gruppenname und Serveranzahl 
            FileReader fr = new FileReader("src/common/config.txt");
            BufferedReader br = new BufferedReader(fr);
            GroupName = br.readLine();
            _numberOfServers = Integer.parseInt(br.readLine());
            for (int i=0; i<_numberOfServers; i++) {
                String serverip = br.readLine();
                String servername = br.readLine();
                    if (_serverHost.equals(serverip)) {
                        _serverName = servername;   
                        System.out.println("Host "+_serverName+" is starting...");
                }
            }
           //Connect to Spread Deamon
           try {
               con.add(this);
               con.connect(InetAddress.getByName(_serverHost), 0, GroupName, false, true);
               System.out.println("Host "+_serverName+" connected to local Spread Daemon");
           } catch (SpreadException | UnknownHostException exc) {
               System.out.println("Connection Spread Deamon failed");
                exc.printStackTrace();
           }

           //Neue Gruppe
           SpreadGroup group = new SpreadGroup();

           //Join 
            try {
                group.join(con, GroupName);
                sendSynchMsg("synch");
                //Thread.sleep(2000);
               _primaryServer = electingPrimaryServer();
               System.out.println("Successfully joined as '"+_serverName+"' to '"+GroupName+"' (Primary: "+_primaryServer+"; Number of servers: "+_numberOfServers+")");
            } catch (SpreadException exc) {
               System.out.println("Join failed");       
                exc.printStackTrace();
            }
            


        }   //end of init

        public String electingPrimaryServer() throws FileNotFoundException, IOException, SpreadException, InterruptedException {
            FileReader fr = new FileReader("src/common/config.txt");
            BufferedReader br = new BufferedReader(fr);
            String primary = null;
     
            //erzeugt dynamisch eine Liste aller Gruppenmitglieder indem beim Empfang der Sender gespeichert wird. 
            //allMembers=_serverName;
            allMembers="";
            SpreadMessage msg = new SpreadMessage();
            msg.setObject("Who is in the group");
            msg.addGroup(GroupName);
            msg.setReliable();
            msg.setSelfDiscard(false);
            try {
                con.multicast(msg);
            } catch (SpreadException e) {
                System.err.println("Could not send message...");
            }
            Thread.sleep(1000);
            //allMembers=allMembers+_serverName;
            //System.out.println("All members: "+allMembers);
                br.readLine();  //Skip this line //Groupname
                int c = Integer.parseInt(br.readLine()); //Line 2, number of servers
                for (int i=0; i<c; i++) {                
                    br.readLine();  //Skip this line // IP
                    String nextMachine = br.readLine();
                    if (allMembers.contains(nextMachine)) {
                        primary = nextMachine;
                        break;
                    }
            }
            SpreadMessage primarymsg = new SpreadMessage();
            primarymsg.setObject(primary);
            primarymsg.addGroup(GroupName);
            primarymsg.setReliable();
            primarymsg.setSelfDiscard(true);
            try {
                con.multicast(primarymsg);
            } catch (SpreadException e) {
                System.err.println("Could not send message...");
            }
            return primary;
        }
        
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
            //System.out.println("ServerState:" + server.state);
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
                String verify = message.getObject().toString();
                if (message.getObject().toString()==null) {
                    System.out.println("...INFO: Empty state received and will be ignored.");
                }
                else if (verify.contains("Who is in the group")) {
                            SpreadMessage msg = new SpreadMessage();
                            msg.setObject("I am in the group");
                            msg.addGroup(GroupName);
                            msg.setReliable();
                            msg.setSelfDiscard(false);
                            try {
                                con.multicast(msg);
                            } catch (SpreadException e) {
                                System.err.println("Could not send message...");
                            }  
                        }        
                else if (verify.contains("I am in the group")) {
                            String member = message.getSender().toString();
                            allMembers=allMembers+member;
                            //System.out.println("Checking group members..."+allMembers);
                        }
                //Aktionen für Primary Server
                else if (_primaryServer!=null) {
                    try {
                        if (verify.contains("synch") && _serverName.equals(_primaryServer)){  
                            System.out.println("---NEW MESSAGE");
                            System.out.println("New synchronisation message from "+message.getSender()+"received.");
                            System.out.println("---END OF MESSAGE");
                            sendBackup(); 
                            System.out.println("Primary server"+_serverName+" has sended ServerState...");
                            //Nur Primary Server sendet Synchronisation
                        }
                        else if (verify.contains("ServerState")){
                            System.out.println("DEBUG INFO: State empfangen von "+message.getSender()+" ...ich bin "+_serverName+" und Primary="+_primaryServer);    
                            if (_serverName.equals(_primaryServer) && !message.getSender().toString().contains(_primaryServer)) {
                            //wenn ich primary bin && wenn message nicht von primary kommt schicke ich an Absender meinen State.
                            //Prüfung ob Message nicht von Primary kommt ist nötig da trotz setSelfDiscard=true die Message auch beim Primary ankam und dann entsteht Endlosschleife 
                                sendBackup(); //Bin ich Primary sende ich nun State an alle. Bin ich nicht Primary und empfange State dann ignoriere ich es. 
                            }
                            else if (message.getSender().toString().contains(_primaryServer))
                                System.out.println("---NEW MESSAGE"); //State von Primary empfangen
                                System.out.println(message.getObject()+" from "+message.getSender()+" received. Checking synchronisation...");
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
                            //wird ein ServerState von Backup Server an Backup Server gesendet, so wird es ignoriert.
                        }   
                    } catch (SpreadException ex) {
                        Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else if ((verify.contains("ServerState"))) {
                    System.out.println("---NEW MESSAGE"); //State von Primary empfangen
                    System.out.println(message.getObject()+" from "+message.getSender()+" received. Checking synchronisation...");
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
                
            } catch (SpreadException ex) {
                Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
            }
                   //     System.out.println("ServerState:" + server.state);

        }

        public void membershipMessageReceived(SpreadMessage message) {
            MembershipInfo info = message.getMembershipInfo();
                if (info.isCausedByJoin()) {
                        System.out.println("member '" + info.getJoined().toString() + "' joined the group.");
                }

                if (info.isCausedByLeave()) {
                        System.out.println("member '" + info.getLeft().toString() + "' left the group.");
                        if (info.getLeft().toString().contains(_primaryServer)) {
                                System.out.println("Primary server disconnected from group. Electing new primary...");
                                    try {
                                        FileReader fr = new FileReader("src/common/config.txt");
                                        BufferedReader br = new BufferedReader(fr);
                                        br.readLine();  //Skip this line //Groupname
                                        int c = Integer.parseInt(br.readLine()); //Line 2, number of servers
                                        for (int i=0; i<c; i++) {                
                                            br.readLine();  //Skip this line // IP
                                            String nextMachine = br.readLine();
                                                if (info.getDisconnected().toString().contains(nextMachine)) {
                                                br.readLine();  //Skip this line // IP
                                                _primaryServer = br.readLine();
                                                break;
                                                }
                                        }
                                        System.out.println("New Primary is "+_primaryServer);
                                    }  catch (Exception ex) {
                                        System.out.println(ex);
                                    }    
                        }
                } else if (info.isCausedByDisconnect()) { 
                        System.out.println("member '" + info.getDisconnected().toString() + "' was disconnected from the group.");
                        if (info.getDisconnected().toString().contains(_primaryServer)) {
                            System.out.println("Primary server disconnected from group. Electing new primary...");
                                    try {
                                        FileReader fr = new FileReader("src/common/config.txt");
                                        BufferedReader br = new BufferedReader(fr);
                                        br.readLine();  //Skip this line //Groupname
                                        int c = Integer.parseInt(br.readLine()); //Line 2, number of servers
                                        for (int i=0; i<c; i++) {                
                                            br.readLine();  //Skip this line // IP
                                            String nextMachine = br.readLine();
                                                if (info.getDisconnected().toString().contains(nextMachine)) {
                                                br.readLine();  //Skip this line // IP
                                                _primaryServer = br.readLine();
                                                break;
                                                }
                                        }
                                        System.out.println("New Primary is "+_primaryServer);
                                    }  catch (Exception ex) {
                                        System.out.println(ex);
                                    }    
                        }
                }
                else if (info.isCausedByNetwork()) {
                    System.out.println("caused by network..."+info.getDisconnected().toString()+info.getLeft().toString());
                }
                /*else {
                    System.out.println("Unknown membership msg ???");
                }*/
            /*if (message.getMembershipInfo().getLeft()!=null || 
                    message.getMembershipInfo().isCausedByDisconnect()==true || 
                    message.getMembershipInfo().isCausedByLeave()==true || 
                    message.getMembershipInfo().isCausedByNetwork()==true || 
                    message.getMembershipInfo().getDisconnected()!=null || 
                    message.getMembershipInfo().isSelfLeave()==true) 
                    {
                        System.out.println(message.getMembershipInfo().getDisconnected());
                        System.out.println(message.getMembershipInfo().getLeft());
                        System.out.println(message.getSender().toString()+" left the group");
                        
                        if (message.getSender().toString().contains(_primaryServer)) {
                        try {
                            _primaryServer = electingPrimaryServer(message.getSender());  //Primary server left... Choosing new Primary
                            System.out.println(_primaryServer+" is now Primary");
                        } catch (IOException ex) {
                            Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (SpreadException ex) {
                            Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(AlcatrazServer.class.getName()).log(Level.SEVERE, null, ex);
                            }       
                        } 
                }
                else if (message.getMembershipInfo().getJoined().toString()!=null) {
                    System.out.println("Group Change: "+message.getMembershipInfo().getJoined().toString()+" joined SpreadGroup");
                } 
                else {
                    System.out.println("Unknown membership message...");
                }*/

            }


    }

    
}