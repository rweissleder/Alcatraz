package Server;

import Server.AlcatrazServer;
import common.ServerState;
import common.IAlcatrazServer;
import common.IRMIClient;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import spread.SpreadException;

public class AlcatrazServerImpl extends UnicastRemoteObject implements IAlcatrazServer, Serializable, Remote {
        
        public final AlcatrazServer.ServerSpread spread;
        protected ServerState state;
        private LinkedList<String> playersInPlay;//to state
        
        
        public AlcatrazServerImpl(AlcatrazServer.ServerSpread s) throws RemoteException {
            super();
            this.spread = s;
            this.state = new ServerState();
        }
        
        AlcatrazServer alcatrazserver = new AlcatrazServer();
        
        
        @Override
        public LinkedList<String> register(IRMIClient p, String name,int playercount, String RMIString) throws RemoteException {
            
            if(!state.addPlayer(p, name, playercount, RMIString)){
                Logger.getLogger(AlcatrazServer.class.getName()).log(Level.WARNING, "Player already registered");
                return null;
            }
            
            System.out.println("Player " + name + " joined (via " + RMIString + ") and wants to play a " + playercount + " - player game");
            System.out.println("");
            try {
                this.spread.sendBackup();
            } catch (SpreadException ex) {
                Logger.getLogger(AlcatrazServerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            return state.getOtherPlayersNames(name, playercount, RMIString);
        }

        @Override
        public int unregister(String name) throws RemoteException {
            if (!state.deletePlayer(name)) {
                return -1;
            }
            System.out.println("Player " + name + " left.");
            try {
                this.spread.sendBackup();
            } catch (SpreadException ex) {
                Logger.getLogger(AlcatrazServerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
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
        public HashMap<String, String> start(String name) throws RemoteException {
           // HashMap<String, ServerState.ClientRMIPos> res = state.getPlayers(name);
            //TODO
            //this.playersInPlay.add(name);
            //backup
            HashMap<String, String> res = state.getRMIStrings();
            return res;
        }
        
    }