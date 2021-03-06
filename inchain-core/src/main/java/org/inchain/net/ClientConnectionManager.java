package org.inchain.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import org.inchain.listener.NewInConnectionListener;
import org.inchain.network.Seed;

public interface ClientConnectionManager {
	
    Future<Seed> openConnection(InetSocketAddress address, StreamConnection connection);

    int getConnectedClientCount();

    void closeConnections(int n);

    void setNewInConnectionListener(NewInConnectionListener newInConnectionListener);
    
    void start();
    
    void stop() throws IOException;
}
