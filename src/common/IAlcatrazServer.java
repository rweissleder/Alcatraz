/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import at.falb.games.alcatraz.api.Player;
import java.rmi.Remote;
import java.rmi.RemoteException;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
/**
 * RMI interface for registration server
 * @author Vladyslav
 */
public interface IAlcatrazServer extends Remote{
    
    public enum SRESPONSE {OK, EXIT};
    
    public LinkedList<String> register(IRMIClient p, String name, int playercount) throws RemoteException;
    public int unregister(String name) throws RemoteException;
    /**
     * Shows to server that a IRMIClient is "still there", that said it is still waiting for game start.
     * Returns true if the IRMIClient is still in queue, otherwise false.
     * @param p
     * @return
     * @throws RemoteException 
     */
    //public boolean stillAlive(IRMIClient p) throws RemoteException;
    public HashMap<String, ServerState.ClientRMIPos> isTeamReady(String name) throws RemoteException;
    public boolean start(String name) throws RemoteException;
}
