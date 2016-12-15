/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import at.falb.games.alcatraz.api.Alcatraz;
import at.falb.games.alcatraz.api.Player;
import at.falb.games.alcatraz.api.Prisoner;
import common.IRMIClient;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import common.*;
import java.io.Serializable;
import java.util.LinkedList;


/**
 *
 * @author rweis
 */
public class RMIClientImpl implements IRMIClient, Serializable{
    //Alcatraz other[] = new Alcatraz[4];
    
    public LinkedList<GameDraw> drawbuf = new LinkedList<>();
    static String RMIString;
    static int RMIPort;
    public int performMove(Player player, Prisoner prisoner, int rowOrCol, int row, int col, int gamestep) throws RemoteException { 
        if(this.drawbuf.size() < gamestep-1){
            return gamestep;
        }
        drawbuf.add(new GameDraw(gamestep, player, prisoner, rowOrCol, row, col));
        return this.drawbuf.size();
    }
    
    
    @Override
    public void gameEnded(Player player) throws RemoteException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

