package client;

import at.falb.games.alcatraz.api.Alcatraz;
import at.falb.games.alcatraz.api.MoveListener;
import at.falb.games.alcatraz.api.Player;
import at.falb.games.alcatraz.api.Prisoner;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import common.IRMIClient;
import java.util.InputMismatchException;
import common.*;
import common.ServerState.ClientRMIPos;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.ini4j.Wini;


/**
 * A test class initializing a local Alcatraz game -- illustrating how
 * to use the Alcatraz API.
 */
public class AlcatrazClient implements MoveListener, Runnable{
    private HashMap<String, String> opts;
    private HashMap<String, ServerState.ClientRMIPos> clients;
    private RMIClientImpl clientRMI;
    private IAlcatrazServer regserver;
    private Alcatraz alca;
    
    
    private String username = "player";
    private int numPlayer = 3;
    private int port = 11001;
    
    private int gamestep = 0;
    
    public AlcatrazClient(){
        clientRMI = new RMIClientImpl();
        servers = new LinkedList<>();
        servers.add(new RegServerParams());
    }
    @Override
    public void run() {
        Scanner scn = new Scanner(System.in);
        System.out.println("Print exit to cancel registration process and terminate program");
        try{
            while(scn.hasNext()){
                if(scn.next().equals("exit")){
                    if(this.regserver.unregister(username) != 0){
                        throw new RemoteException();
                    }
                    break;
                }
            }
        } catch (RemoteException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Couldn't unregister");
        }catch (Exception e){
            if(e instanceof InterruptedException){
                //do nothing
            }
        }
        this.gameDrawBufferWatcher();
    }
    
    private class RegServerParams{
        private String regservername = "RegServer";
        private String regserverip = "127.0.0.1";
        private int regserverport = 11010;
    }
    
    LinkedList<RegServerParams> servers;
    

    public void setRegServer(IAlcatrazServer server){
        this.regserver = server;
    }
    
    public int getNumPlayer() {
        return numPlayer;
    }

    public void setNumPlayer(int numPlayer) {
        this.numPlayer = numPlayer;
    }

    public void moveDone(Player player, Prisoner prisoner, int rowOrCol, int row, int col) {
        this.gamestep++;
        this.clientRMI.drawbuf.add(new GameDraw(gamestep, player, prisoner, rowOrCol, row, col));
        int i=0;
        Iterator it = this.clients.entrySet().iterator();
        while(it.hasNext()){
            ClientRMIPos r = (ClientRMIPos)((Map.Entry)it.next()).getValue();
            try {
                // last saved draw of remote client 
                i = r.getRMI().performMove(player, prisoner, rowOrCol, row, col, this.gamestep);
                if(i<this.gamestep){
                    while(i<this.gamestep){
                        i++;
                        i = r.getRMI().performMove(this.clientRMI.drawbuf.get(i).getPlayer(), 
                                this.clientRMI.drawbuf.get(i).getPrisoner(), 
                                this.clientRMI.drawbuf.get(i).getRowOrCol(),
                                this.clientRMI.drawbuf.get(i).getRow(), 
                                this.clientRMI.drawbuf.get(i).getCol(),
                                this.clientRMI.drawbuf.get(i).getGamestep());
                    }        
                }
            } catch (RemoteException ex) {
                Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
                i=0;
                continue;
            }
            i=0;
        }
     }
    
    public void gameWon(Player player) {
        System.out.println("Player " + player.getId() + " wins.");
    }

    public void setUsername(String username){
        
        this.username=username;
    }
    public String getUsername(){
        return this.username;
    }
    
    public void gameDrawBufferWatcher(){

        while(true){
            try {
                if(!this.clientRMI.drawbuf.isEmpty()){
                    if(this.clientRMI.drawbuf.size() > this.gamestep){
                        this.alca.doMove(this.clientRMI.drawbuf.getLast().getPlayer(), 
                                this.clientRMI.drawbuf.getLast().getPrisoner(),
                                this.clientRMI.drawbuf.getLast().getRowOrCol(),
                                this.clientRMI.drawbuf.getLast().getRow(), 
                                this.clientRMI.drawbuf.getLast().getCol());
                        this.gamestep++;
                    }
                }
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
        }
    }
 
    /**
     * @param args Command line args
     */
    public static void main(String[] args) throws MalformedURLException{
        
        String [] ServerList;
        ServerList = new String[3];
        ServerList[0]="rmi://192.168.0.100:1099/Server0";
        ServerList[1]="rmi://192.168.0.101:1099/Server1";
        ServerList[2]="rmi://192.168.0.102:1099/Server2";

        
        String primaryRMI=null;

        
        
        AlcatrazClient client = new AlcatrazClient();
        if(args.length>0){
            try {
                client.parseConfigFile(args[1]);
            } catch (Exception ex) {
                Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        InetAddress address; 
        Thread t = new Thread(client);
        t.start();
        Registry registry;
        
        for(int i=0; i<=1; i++){
            
            try{
            registry = LocateRegistry.getRegistry("rmi://" + ServerList[i] + ":1099");
            client.setRegServer((IAlcatrazServer) Naming.lookup(ServerList[i]));        
            System.out.println("RMI OK");
            System.out.println("Verbunden mit Server " + ServerList[i]);
            primaryRMI=ServerList[i];
            break;
            }
            catch(Exception e){
            System.out.println("Server " + i + " not reachable");
        }
        }
        try {
            // connecting to rmi registry of primary server
            

            int playerswaiting = client.regToGame();
            if(playerswaiting < 0)
            {
                System.out.println("Error by registering");
                System.exit(1);
            }
            while(!client.regserver.isTeamReady(client.username)){
                if(!t.isAlive()){
                    System.out.println("Execution terminated.");
                    System.exit(0);
                }
                Thread.sleep(300);
            }
            if(client.receivePlayersInterfaces() < 0){
                System.out.println("Error by game start");
                System.exit(1);
            }
            t.interrupt();
            client.startGame();
        } catch (RemoteException | InterruptedException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private int regToGame(){
        try{
            LinkedList<String> playernames = regserver.register(clientRMI, username, numPlayer,"");
            if(playernames.size() > 0){
                System.out.println("List of other players already waiting:");
                System.out.print(playernames);
                return playernames.size();
            }else
            {
                System.out.println("There are no other players currently waiting");
            }
        }catch(RemoteException ex){
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        return 0;
    }
    private int receivePlayersInterfaces(){
        try {
            this.clients = new HashMap<>(this.regserver.start(this.username));
        } catch (RemoteException | NullPointerException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        return 0;
    } 
    void startGame(){
        this.alca = new Alcatraz();
        this.alca.init(numPlayer, this.clients.get(this.username).getPos());
        Iterator it = this.clients.entrySet().iterator();
        this.alca.addMoveListener(this);
        this.alca.start();
    }
    public void parseConfigFile(String config) throws IOException{
        Wini ini = new Wini(new File(config));
        this.username = ini.get("client", "name");
        this.numPlayer = ini.get("client", "playercount", int.class);
    }
    
    

}
