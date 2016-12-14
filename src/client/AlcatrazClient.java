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
import java.util.Set;
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
    private InputScanner scanner;
    
    private String username = "playerD";
    
    //total amount of players
    private int numPlayer = 2;
    private int port = 11001;
    
    private int gamestep = 0;
    
    private Thread watcher;
    
    public AlcatrazClient(){
        
        clientRMI = new RMIClientImpl();
        servers = new LinkedList<>();
        servers.add(new RegServerParams());
        scanner = new InputScanner();
    }
    
    @Override
    public void run(){
       drawBufWatcher(); 
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

    @Override
    public void moveDone(Player player, Prisoner prisoner, int rowOrCol, int row, int col) {
        //this.alca.doMove(player, prisoner, rowOrCol, row, col);
        this.gamestep++;
        this.clientRMI.drawbuf.add(new GameDraw(gamestep, player, prisoner, rowOrCol, row, col));
        int i=0;
        Iterator it = this.clients.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, ClientRMIPos> entry = (Map.Entry)it.next();
            if(entry.getKey().equals(this.username))
                continue;
            ClientRMIPos r = (ClientRMIPos)entry.getValue();
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
    
 
    /**
     * @param args Command line args
     */
    public static void main(String[] args) throws MalformedURLException{
        
        String [] ServerList;
        ServerList = new String[1];
        ServerList[0]="rmi://192.168.242.139:1099/Server2";
        //ServerList[0]="rmi://192.168.0.102:1099/Server2";
//        ServerList[1]="rmi://192.168.0.101:1099/Server1";
//        ServerList[2]="rmi://192.168.0.102:1099/Server2";

        
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
        Thread t = new Thread(client.scanner);
        t.start();
        Registry registry;
        
        if(!client.connectToServer(ServerList)){
            System.out.println("No servers available.");
        }
        try {
            // connecting to rmi registry of primary server
            

            int playerswaiting = client.regToGame();
            if(playerswaiting < 0)
            {
                System.out.println("Error by registering");
                System.exit(1);
            }
            while(t.isAlive()){
                try{
                    do{
                        Thread.sleep(300);
                        client.clients = client.regserver.isTeamReady(client.username);
                    }while(client.clients.size() != client.numPlayer);
                    break;
                }catch(RemoteException e){
                    System.out.println("Primary is unreachable. Reconecting to backup(s).");
                    if(!client.connectToServer(ServerList)){
                        System.out.println("No servers available.");
                        System.exit(0);
                    }
                    continue;
                }
            }
            while(!client.requestStart()){
                //System.out.println("Error by game start");
                Thread.sleep(100);
            }
            t.stop();
            client.startGame();
        } catch (InterruptedException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            try {
                client.regserver.unregister(client.username);
            } catch (RemoteException ex1) {
                Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }
    private boolean connectToServer(String[] ServerList){
        Registry registry;
        String primaryRMI;
        for(int i=0; i<=1; i++){
            
            try{
                registry = LocateRegistry.getRegistry("rmi://" + ServerList[i] + ":1099");
                this.setRegServer((IAlcatrazServer) Naming.lookup(ServerList[i]));        
                System.out.println("RMI OK");
                System.out.println("Verbunden mit Server " + ServerList[i]);
                primaryRMI=ServerList[i];
                return true;
            }
            catch(Exception e){
                System.out.println("Server " + i + " not reachable");
                continue;
            }
        }
        return false;
    } 
    private int regToGame(){
        try{
            LinkedList<String> playernames = regserver.register(clientRMI, username, numPlayer);
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
    private boolean requestStart(){
        try {
            return this.regserver.start(this.username);
        } catch (RemoteException | NullPointerException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    } 
    
    void startGame(){
        this.alca = new Alcatraz();
        this.alca.init(numPlayer, this.clients.get(this.username).getPos());
        LinkedList<String> names = new LinkedList<>(this.clients.keySet());
        for(int i=0; i<this.clients.size(); i++){
            this.alca.getPlayer(i).setName(names.get(i));
        }
        Iterator it = this.clients.entrySet().iterator();
        this.alca.showWindow();
        this.alca.addMoveListener(this);
        watcher = new Thread(this);
        watcher.start();
        this.alca.start();
    }
    
    public void parseConfigFile(String config) throws IOException{
        Wini ini = new Wini(new File(config));
        this.username = ini.get("client", "name");
        this.numPlayer = ini.get("client", "playercount", int.class);
    }
    
    private class InputScanner implements Runnable{
        
        @Override
        public void run() {
            Scanner scn = new Scanner(System.in);
            System.out.println("Print exit to cancel registration process and terminate program");
            try{
                while(scn.hasNext()){
                    if(scn.next().equals("exit")){
                        if(regserver.unregister(username) != 0){
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
                }
            }
        }
    }
    public void drawBufWatcher(){
        int gamestep = this.gamestep;//this.clientRMI.drawbuf.size();
        try{
            while(true){
                if(gamestep != this.clientRMI.drawbuf.size()){
                    this.alca.doMove(this.clientRMI.drawbuf.getLast().getPlayer(), 
                            this.clientRMI.drawbuf.getLast().getPrisoner(), 
                            this.clientRMI.drawbuf.getLast().getRowOrCol(), 
                            this.clientRMI.drawbuf.getLast().getRow(), 
                            this.clientRMI.drawbuf.getLast().getCol());
                    gamestep = this.gamestep;//this.clientRMI.drawbuf.size();
                    Thread.sleep(300);
                }
            }
        }catch(InterruptedException e){
            return;
        }
    }
}
