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
            HashMap<String, String> RMIStrings;
            HashMap<String, ClientRMIPos> queue[]; // array of queues by amount of players to play with 
            HashMap<String, Integer> regNames; // names of players and amount of other players with which they want to play
            public ServerState getState(){
                return new ServerState(this);
            }
            
            public ServerState(){
                queue = new HashMap[3];
                queue[0] = new HashMap<>();
                queue[1] = new HashMap<>();
                queue[2] = new HashMap<>();
                regNames = new HashMap<>();
                generation = 0;  //Versionsnr
                RMIStrings = new HashMap<>();
            }
            

            public ServerState(HashMap<String, Integer> names, HashMap<String, ClientRMIPos>[] queue, HashMap<String, String> RMIStrings){
                this.queue = queue.clone();
                regNames = names;
                generation = 0;
                this.RMIStrings = RMIStrings;
                
            }
            
            public ServerState(ServerState state){
                this.queue = state.queue;
                this.regNames = state.regNames;
                this.generation = state.generation;
                this.RMIStrings = state.RMIStrings;
            }
            
            public boolean ifExists(String name){
                return regNames.containsKey(name);
            }
            public boolean addPlayer(IRMIClient p, String name,int playercount, String RMIString){
                if (ifExists(name)) {
                    return false;
                }
                int i = queue[playercount-1].size();
                queue[playercount-1].put(name, new ClientRMIPos(p, queue.length));
                regNames.put(name, playercount - 1);
                generation++;
                return true;
            };
            
            public boolean deletePlayer(String name){
                if(!ifExists(name)){
                    return false;
                }
                queue[regNames.get(name)].remove(name);
                regNames.remove(name);
                generation++;
                return true;
            };
            
            public HashMap<String, ClientRMIPos> getPlayers(String name){
                if(!this.ifReadyToPlay(name)){
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
            
            public LinkedList<String> getOtherPlayersNames(String name, int playercount, String RMIString){
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
            
            public int getGeneration(){
                return generation;
            }
            
        };