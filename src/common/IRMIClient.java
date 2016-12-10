/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import at.falb.games.alcatraz.api.Player;
import at.falb.games.alcatraz.api.Prisoner;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Vladyslav
 */
public interface IRMIClient extends Remote{
    
    public int performMove(Player player, Prisoner prnsr, int rowcol, int row, int col, int gamestep) throws RemoteException;
    
    
    public void gameEnded(Player player) throws RemoteException;
}

