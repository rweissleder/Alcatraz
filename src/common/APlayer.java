/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import at.falb.games.alcatraz.api.Player;


/**
 *
 * @author Vladyslav
 */
public class APlayer {
    public Player player;
    public long alivetime = 0;
    
    public APlayer(Player p){
        player = p;
    }
    
    public Player getPlayer(){
        return player;
    }
    
}
