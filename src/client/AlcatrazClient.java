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

//import common.ServerState.ClientRMIPos;
//import commontest.ClientInterface;
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
 * A test class initializing a local Alcatraz game -- illustrating how to use
 * the Alcatraz API.
 */
public class AlcatrazClient extends UnicastRemoteObject implements MoveListener, Runnable, Remote, Serializable, ClientInterface {

    private HashMap<String, String> opts;
    private HashMap<String, String> clients;
    private RMIClientImpl clientRMI;
    private String clientRMIString;
    private IAlcatrazServer regserver;
    private Alcatraz alca;
    private int position;
    private Alcatraz other[] = new Alcatraz[4];
    private String username = "player";
    public String rmi[] = new String[3];
    public int ownPosition;
    public static int randomPort = (int) (Math.random() * 1000) + 1090;
    public static InetAddress address;
    // public static String IPaddr = (String) address.getHostAddress();
    /*public String[] rmiArray() {
        String rmi[] = new String[3];
        rmi[0]="rmi://";        
        rmi[1]="rmi://";
        rmi[2]="rmi://";
        rmi[3]="rmi://";
        return rmi;
    }*/

    //anzahl spilere mit denen will ich spiel starten
    private int numPlayer = 2;
    private int port = 11001;

    private int gamestep = 0;

    public AlcatrazClient() throws RemoteException {
        // IPString;
        //this.address = InetAddress.getLocalHost();
        clientRMI = new RMIClientImpl();
        servers = new LinkedList<>();
        servers.add(new RegServerParams());
    }

    @Override
    public void run() {
        Scanner scn = new Scanner(System.in);
        System.out.println("Print exit to cancel registration process and terminate program");
        try {
            while (scn.hasNext()) {
                if (scn.next().equals("exit")) {
                    if (this.regserver.unregister(username) != 0) {
                        throw new RemoteException();
                    }
                    break;
                }
            }
        } catch (RemoteException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Couldn't unregister");
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                //do nothing
            }
        }
        // this.gameDrawBufferWatcher();
    }

    private class RegServerParams implements Serializable {

        private String regservername = "RegServer";
        private String regserverip = "127.0.0.1";
        private int regserverport = 11010;
    }

    LinkedList<RegServerParams> servers;

    public void setRegServer(IAlcatrazServer server) {
        this.regserver = server;
    }

    public int getNumPlayer() {
        return numPlayer;
    }

    public void setNumPlayer(int numPlayer) {
        this.numPlayer = numPlayer;
    }

    public void setOther(int i, Alcatraz t) {
        this.other[i] = t;
    }

    @Override
    public void gameWon(Player player) {
        System.out.println("Player " + player.getId() + " wins.");
    }

    @Override
    public void moveDone(Player player, Prisoner prisoner, int rowOrCol, int row, int col) {
        try {
            otherMoveDone(player, prisoner, rowOrCol, row, col);
        } catch (InterruptedException ex) {
            //   Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        ownMoveDone(player, prisoner, rowOrCol, row, col);
    }

    //Sendet Remote Aufruf für Move an alle Spieler
    public void otherMoveDone(Player player, Prisoner prisoner, int rowOrCol, int row, int col) throws InterruptedException /*throws NotBoundException, MalformedURLException, RemoteException*/ {
        //Übergabe RMI Aufrufe 
        // String rmi[] = rmiArray();

        /* boolean lookup = false;
        int attempt = 1;
        System.out.println(clients);
        for (Map.Entry entry : this.clients.entrySet()) {
            String name = (String) entry.getKey();
            String location = (String) entry.getValue();

            if (!name.equals(this.username)) {   //exkludiert Spieler, der den Move macht (dem muss man RMI nicht schicken)
                while (lookup == false) {
                    try {
                        ClientInterface remoteinterface = (ClientInterface) Naming.lookup(location); //rmi Array
                        System.out.println(player);
                        System.out.println(prisoner);
                        System.out.println(rowOrCol);
                        System.out.println(row);
                        System.out.println(col);

                        remoteinterface.remoteMoveDone(player, prisoner, rowOrCol, row, col);
                        System.out.println("Successfully done RMI " + location);
                        lookup = true;
                    } catch (Exception ex) {

                        lookup = false;
                        System.out.println(attempt + ": Trying to reach Player " + name + ": " + location + ": " + ex);
                        attempt++;
                        Thread.sleep(5000);
                        // Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                      }  
                }
            } 
        }*/
        boolean lookup = false;
        int attempt = 1;
        for (int k = 0; k < getNumPlayer(); k++) {   //Geht rmi Array durch und schickt rmi dorthin
            if (k != player.getId()) {   //exkludiert Spieler, der den Move macht (dem muss man RMI nicht schicken)
                while (lookup == false) {
                    try {
                        ClientInterface remoteinterface = (ClientInterface) Naming.lookup(rmi[k]); //rmi Array           
                        remoteinterface.remoteMoveDone(player, prisoner, rowOrCol, row, col);
                        System.out.println("Successfully done RMI " + rmi[k]);
                        lookup = true;
                    } catch (Exception ex) {

                        lookup = false;
                        System.out.println("DES GEHT NED" + rmi[k] + attempt + ": Trying to reach Player " + k + ": " + rmi[k] + ": " + ex);
                        attempt++;
                        Thread.sleep(3000);
                        // Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    //Methode empfängt Move und führt diesen Remote bei anderen Spielern durch
    @Override
    public void remoteMoveDone(Player player, Prisoner prisoner, int rowOrCol, int row, int col) throws RemoteException {
        // String rmi[] = rmiArray(); 
        //doMove wird für ID des Spielers durchgeführt dessen Zug übergeben wurde

        int i = player.getId();

        //Jetzt schicke ich Nachrichten-Kopien an Alle außer mich und den Spieler der Move macht        
        //Damit finde ich heraus welche ID ich habe
        int myId = 0;
        for (int k = 0; k < getNumPlayer(); k++) {
            if (other[k] == null) {
                myId = k;  // Das ist meine Player ID
            }
        }
        //NACHRICHTEN KOPIEN WERDEN GESENDET
        for (int k = 0; k < getNumPlayer(); k++) {
            if (k != player.getId() && k != myId) {   //Ungleich meine ID und ID des Spielers der Move macht
                try {
                    ClientInterface messageCopyInterface = (ClientInterface) Naming.lookup(this.rmi[k]); //rmi Array           
                    messageCopyInterface.messageCopy(player, prisoner, rowOrCol, row, col);
                } catch (RemoteException | MalformedURLException | NotBoundException ex) {
                    //             Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        other[i].doMove(other[i].getPlayer(player.getId()), other[i].getPrisoner(prisoner.getId()), rowOrCol, row, col);
        System.out.println("Doing remote Move: i=" + i + " " + other[i].getPlayer(player.getId()) + " - rowOrCol:" + rowOrCol + " row:" + row + "col:" + col);
    }

    //EMPFÄNGT NACHRICHTEN-KOPIE UND FÜHRT MOVE AUS / RELIABILITY
    //Falls Sender während Senden abstürzt
    public void messageCopy(Player player, Prisoner prisoner, int rowOrCol, int row, int col) throws RemoteException {
        int i = player.getId();
        other[i].doMove(other[i].getPlayer(player.getId()), other[i].getPrisoner(prisoner.getId()), rowOrCol, row, col);
        System.out.println("Received messageCopy: i=" + i + " " + other[i].getPlayer(player.getId()) + " - rowOrCol:" + rowOrCol + " row:" + row + "col:" + col);
    }

    //wird zuletzt aufgerufen und ist der Abschluss des Moves
    public void ownMoveDone(Player player, Prisoner prisoner, int rowOrCol, int row, int col) {
        System.out.println("Own Move: moving " + prisoner + " to " + (rowOrCol == Alcatraz.ROW ? "row" : "col") + " " + (rowOrCol == Alcatraz.ROW ? row : col));
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    /*public void gameDrawBufferWatcher(){

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
    }*/
    /**
     * @param args Command line args
     */
    public static void main(String[] args) throws MalformedURLException, java.net.UnknownHostException, AlreadyBoundException, RemoteException {
        String[] ServerList;
        ServerList = new String[4];
        ServerList[0] = "rmi://192.168.0.100:1099/machine0";
        ServerList[1] = "rmi://192.168.0.101:1099/machine1";
        ServerList[2] = "rmi://192.168.0.102:1099/machine2";
        ServerList[3] = "rmi://192.168.0.102:1099/machine3";

        InetAddress IPaddress = InetAddress.getLocalHost();
        String hostIP = IPaddress.getHostAddress();

        String primaryRMI = null;

        AlcatrazClient client = new AlcatrazClient();

        System.out.println("Please enter your username: ");
        Scanner usernamescan = new Scanner(System.in);
        client.username = usernamescan.next();
        System.out.println("Do you want to start a 2,3 or 4 Player game?");
        Scanner numPlayerscan = new Scanner(System.in);
        client.numPlayer = numPlayerscan.nextInt();

        String ownRMI = "rmi://" + hostIP + ":" + randomPort + "/" + client.username;

        LocateRegistry.createRegistry(randomPort);
        Naming.rebind(ownRMI, client);

        if (args.length > 0) {
            try {
                client.parseConfigFile(args[1]);
            } catch (Exception ex) {
                Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        InetAddress address;
        Thread t = new Thread(client);
        t.start();
        if (!client.connectToServer(ServerList)) {
            System.out.println("No servers available.");
        }
        try {
            // connecting to rmi registry of primary server

            int playerswaiting = client.regToGame();
            if (playerswaiting < 0) {
                System.out.println("Error by registering");
                System.exit(1);
            }
            while (t.isAlive()) {
                try {
                    while (!client.regserver.isTeamReady(client.username)) {
                        Thread.sleep(300);
                    }
                    break;
                } catch (RemoteException e) {
                    System.out.println("Primary is unreachable. Reconecting to backup(s).");
                    if (!client.connectToServer(ServerList)) {
                        System.out.println("No servers available.");
                        System.exit(0);
                    }
                    continue;
                }
            }
            if (client.receivePlayersInterfaces() < 0) {
                System.out.println("Error by game start");
                System.exit(1);
            }
            t.interrupt();
            client.startGame(client.numPlayer, client.position);
        } catch (InterruptedException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean connectToServer(String[] ServerList) {
        Registry registry;
        String primaryRMI;
        for (int i = 0; i <= ServerList.length; i++) {

            try {
                registry = LocateRegistry.getRegistry("rmi://" + ServerList[i] + ":1099");
                this.setRegServer((IAlcatrazServer) Naming.lookup(ServerList[i]));
                System.out.println("RMI OK");
                System.out.println("Verbunden mit Server " + ServerList[i]);
                primaryRMI = ServerList[i];
                return true;
            } catch (Exception e) {
                System.out.println("Server " + i + " not reachable");
                continue;
            }
        }
        return false;
    }

    private int regToGame() throws java.net.UnknownHostException, AlreadyBoundException {

        InetAddress address2 = InetAddress.getLocalHost();
        String hostIP2 = address2.getHostAddress();

        try {

            clientRMI.RMIPort = randomPort;
            clientRMI.RMIString = "rmi://" + hostIP2 + ":" + randomPort + "/" + this.username;
            //Naming.rebind(clientRMI.RMIString, this);

            System.out.println(clientRMI.RMIPort);

            LinkedList<String> playernames = regserver.register(clientRMI, username, numPlayer, clientRMI.RMIString);
            if (playernames.size() > 0) {
                System.out.println("List of other players already waiting:");
                System.out.print(playernames);

                return playernames.size();
            } else {
                System.out.println("There are no other players currently waiting");
            }
        } catch (RemoteException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        return 0;
    }

    private int receivePlayersInterfaces() {
        try {
            this.clients = new HashMap<>(this.regserver.start(this.username));
        } catch (RemoteException | NullPointerException ex) {
            Logger.getLogger(AlcatrazClient.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        return 0;
    }

    void startGame(int playerCount, int position) throws RemoteException {

        String[] clientNames = new String[clients.size()];
        String[] clientRMIS = new String[clients.size()];
        //  AlcatrazClient sub1 = new AlcatrazClient();
        int index = 0;
        for (Map.Entry<String, String> mapEntry : clients.entrySet()) {
            clientNames[index] = mapEntry.getKey();
            clientRMIS[index] = mapEntry.getValue();
            index++;
        }

        for (int i = 0; i < numPlayer; i++) {
            System.out.println(username);
            System.out.println(clientNames[i]);
            if (clientNames[i].equals(username)) {
                ownPosition = i;
                System.out.println("Own Position: " + ownPosition);
            }

            rmi[i] = clientRMIS[i];

            System.out.println("Player " + clientNames[i] + " via " + rmi[i]);

        }

        // clients.keySet().toArray();
        // clients.values().toArray();
        // client.setOther(2,this.alca);
        //TO DO wie kann ich den Namen des anderen Spielers bekommen?
        //TO DO getPos ist immer 3....
        //TO DO RMI Strings an alle Clients schicken
        //Logik für Spielstart
        Alcatraz alca = new Alcatraz();
        this.setNumPlayer(numPlayer);

        alca.init(numPlayer, ownPosition);   //hier meine Nummer eintragen
        alca.getPlayer(ownPosition).setName(username);
        for (int i = 0; i < numPlayer; i++) {
            alca.getPlayer(i).setName(clientNames[i]);
            if (this.ownPosition != i) {
                this.setOther(i, alca);
            }
            System.out.println("Spieler " + (i + 1) + ": " + clientNames[i] + ", Position: " + i);
        }

        alca.showWindow();
        alca.addMoveListener(this);
        alca.start();
        if (this.regserver.unregister(username) != 0) {
            throw new RemoteException();
        }
    }

    public void parseConfigFile(String config) throws IOException {
        Wini ini = new Wini(new File(config));
        this.username = ini.get("client", "name");
        this.numPlayer = ini.get("client", "playercount", int.class);
    }

}
