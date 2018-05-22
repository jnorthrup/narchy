package jcog.net.http;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

interface UpgradeWebSocketHandler {
    void upgradeWebSocketHandler(SocketChannel sChannel, ByteBuffer prependData);
}
