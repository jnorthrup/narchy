package net.propero.rdp.rdp5.disk;

import net.propero.rdp.RdpPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

class IRP {

    private final int fileId;

    private final int majorFunction;

    private final int minorFunction;

    private final DataOutputStream out;
    private final ByteArrayOutputStream bout;

    public RdpPacket data;

    public int deviceId;

    public int completionId;


    public IRP(int fileId, int majorFunction, int minorFunction) {
        this.fileId = fileId;
        this.majorFunction = majorFunction;
        this.minorFunction = minorFunction;

        bout = new ByteArrayOutputStream();
        out = new DataOutputStream(bout);
    }

}
