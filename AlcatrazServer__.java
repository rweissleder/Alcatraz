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
 * @author Vladyslav
 */
public class AlcatrazServer implements Serializable, Remote {

    public String _serverHost = "localhost";
    public int _spreadPort = 13335;
    public String _serverName; /*= "RegServer";*/
    //public String _backupServer = "backupServer";
    public AlcatrazServerImpl server;
    //public boolean primary;
    LinkedList<IRMIClient> buf;
    //SpreadConnection connection;
    SpreadConnection con = new SpreadConnection();
    String GroupName; /*= "AlcatrazSpreadGroup";*/

    AlcatrazServer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    public void init() throws RemoteException, IOException, SpreadException {
        server = new AlcatrazServerImpl();
    }
    

    public static void main(String args[]) throws IOException, SpreadException {
        AlcatrazServer s = new AlcatrazServer();
        

        System.out.println("Starting registration server...");
        Registry registry;
        //trying to create registry
        try {
            s.init();  //starts Spread 
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

    public class AlcatrazServerImpl extends UnicastRemoteObject implements IAlcatrazServer, Serializable, Remote {
        
        public class ServerState implements Cloneable, Serializable {
            int generation; // # of state
            HashMap<String, IRMIClient> queue[]; // array of queues by amount of players to play with 
            HashMap<String, Integer> regNames; // names of players and amount of other players with which they want to play
            
            public ServerState(){
                queue[0] = new HashMap<>();
                queue[1] = new HashMap<>();
                queue[2] = new HashMap<>();
                regNames = new HashMap<>();
                generation = 0;
            }

            public ServerState(HashMap<String, Integer> names, HashMap<String, IRMIClient>[] queue){
                this.queue = queue.clone();
                regNames = names;
                generation = 0;
            }
            
            public ServerState(ServerState state){
                this.queue = state.queue;
                this.regNames = state.regNames;
                this.generation = state.generation;
            }
            
            public boolean ifExists(String name){
                return regNames.containsKey(name);
            }
            public boolean addPlayer(IRMIClient p, String name,int playercount){
                if (ifExists(name)) {
                    return false;
                }
                int i = queue[playercount-1].size();
                queue[playercount-1].put(name, p);
                regNames.put(name, playercount - 1);
                generation++;
                return true;
            };
            
            public boolean deletePlayer(String name){
                if(!ifExists(name)){
                    return false;
                }
                queue[regNames.get(name)].remove(name);
                regNames.remove(name);
                generation++;
                return true;
            };
            
            public HashMap<String, IRMIClient> getOtherPlayers(String name){
                if(!this.ifReadyToPlay(name)){
                    return null;
                }
                int playerqueue = regNames.get(name);
                HashMap<String, IRMIClient> res = new HashMap<>(queue[playerqueue]);
                res.remove(name);
                return res;
            };
            
            public LinkedList<String> getOtherPlayersNames(String name, int playercount){
                LinkedList<String> list = new LinkedList<>(queue[playercount-1].keySet());
                list.remove(name);
                return list;
            };
            
            public boolean ifReadyToPlay(String name){
                if(!this.ifExists(name)){
                    return false;
                }
                return queue[regNames.get(name)].size() >= regNames.get(name)+1;
            }
        };

        private ServerState state;
        
        
        public AlcatrazServerImpl() throws RemoteException {
            super();
            
        }
        

        
        @Override
        public LinkedList<String> register(IRMIClient p, String name,int playercount) throws RemoteException {
            
            if(!state.addPlayer(p, name, playercount)){
                Logger.getLogger(AlcatrazServer.class.getName()).log(Level.WARNING, "Player already registered");
                return null;
            }
            System.out.println("Player " + name + " joined.");
            return state.getOtherPlayersNames(name, playercount);
        }

        @Override
        public int unregister(String name) throws RemoteException {
            if (!state.deletePlayer(name)) {
                return -1;
            }
            System.out.println("Player " + name + " left.");
            return 0;
        }

        public boolean isTeamReady(String name) throws RemoteException {
            return state.ifReadyToPlay(name);
        }
        
        /**
         * sends the list of other players remote interfaces, who are also in queue
         *
         * @param p
         * @return
         * @throws RemoteException
         */
        public HashMap<String, IRMIClient> start(String name) throws RemoteException {
            HashMap<String, IRMIClient> res = state.getOtherPlayers(name);
            return res;
        }
        
    }
}
