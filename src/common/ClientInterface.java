package commontest;

import java.rmi.Remote;
import java.rmi.RemoteException;
import at.falb.games.alcatraz.api.Alcatraz;
import at.falb.games.alcatraz.api.MoveListener;
import at.falb.games.alcatraz.api.Player;
import at.falb.games.alcatraz.api.Prisoner;

//Interface für Client-Client Kommunikation
public interface ClientInterface extends Remote {
    
    public void remoteMoveDone(Player player, Prisoner prisoner, int rowOrCol, int row, int col) throws RemoteException;
    public void messageCopy (Player player, Prisoner prisoner, int rowOrCol, int row, int col) throws RemoteException;

}