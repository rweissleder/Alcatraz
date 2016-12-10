/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import at.falb.games.alcatraz.api.Player;
import at.falb.games.alcatraz.api.Prisoner;

/**
 *
 * @author Florian
 */
public class GameDraw {
    int gamestep;
    Player player;
    Prisoner prisoner;
    int rowOrCol, row, col;
    
    public GameDraw(int gamestep, Player player, Prisoner prisoner, int rowOrCol, int row, int col){
        this.gamestep = gamestep;
        this.player = player;
        this.prisoner = prisoner;
        this.rowOrCol = rowOrCol;
        this.row = row;
        this.col = col;
    }
    public int getGamestep(){
        return gamestep;
    }
    public Player getPlayer(){
        return player;
    }
    
    public Prisoner getPrisoner(){
        return prisoner;
    }
    
    public int getRowOrCol(){
        return rowOrCol;
    }
    
    public int getRow(){
        return row;
    }
    
    public int getCol(){
        return col;
    }
}
