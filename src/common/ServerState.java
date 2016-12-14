/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import common.IRMIClient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author Florian
 */
public class ServerState implements Cloneable, Serializable {
    public class ClientRMIPos implements Serializable {
        IRMIClient rmi;
        int pos;
      
        public ClientRMIPos(IRMIClient rmi, int pos){
            this.rmi = rmi;
            this.pos = pos;
        
        }
        public int getPos(){
            return this.pos;
        }
        public IRMIClient getRMI(){
            return this.rmi;
        }
    }
            int generation; // # of state
            HashMap<String, ClientRMIPos> queue[]; // array of queues by amount of players to play with 
            HashMap<String, Integer> regNames; // names of players and amount of other players with which they want to play
            private LinkedList<String> playersInPlay;
            public ServerState getState(){
                return new ServerState(this);
            }
            
            public ServerState(){
                playersInPlay = new LinkedList<String>();
                queue = new HashMap[3];
                queue[0] = new HashMap<>(); //2 players
                queue[1] = new HashMap<>(); //3
                queue[2] = new HashMap<>(); //4
                regNames = new HashMap<>();
                generation = 0;  //Versionsnr
            }
            

            public ServerState(HashMap<String, Integer> names, HashMap<String, ClientRMIPos>[] queue){
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
                queue[playercount-1].put(name, new ClientRMIPos(p, queue[playercount-1].size()));
                regNames.put(name, playercount - 1);
                generation++;
                System.out.println("Player " + name + " added.");
                return true;
            };
            
            public boolean deletePlayer(String name){
                if(!ifExists(name)){
                    return false;
                }
                queue[regNames.get(name)].remove(name);
                regNames.remove(name);
                generation++;
                System.out.println("Player " + name + " deleted.");
                return true;
            };
            
            public HashMap<String, ClientRMIPos> getPlayers(String name){
                if(!this.ifExists(name)){
                    return null;
                }
                int playerqueue = regNames.get(name);
                HashMap<String, ClientRMIPos> res = new HashMap<>(queue[playerqueue]);
                return res;
            }
            
            public HashMap<String, ClientRMIPos> getOtherPlayers(String name){
                if(!this.ifReadyToPlay(name)){
                    return null;
                }
                int playerqueue = regNames.get(name);
                HashMap<String, ClientRMIPos> res = new HashMap<>(queue[playerqueue]);
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
            
            public void playerPutInPlay(String name){
                this.playersInPlay.add(name);
            }
            //search for at least 1 queue that is full
            public boolean isQueueReadyToPlay(){
                for(int i=0; i<3; i++){
                    //if queue is full
                    
                    if(this.queue[i].size() >= i+1){
                        LinkedList<String> list = new LinkedList<>(this.queue[i].keySet());
                        if(this.playersInPlay.containsAll(list))
                                return true;
                    }
                }
                return false;
            }
            
            public LinkedList<String> getPlayersInGame(){
                LinkedList<String> list = new LinkedList<>();
                for(int i=0; i<3; i++){
                    //if queue is full
                    if(this.queue[i].size() >= i+1){
                        list.addAll(this.queue[i].keySet());
                    }
                }
                return list;
            }
            public int getGeneration(){
                return generation;
            }
            
        };