package net.propero.rdp.rdp5.disk;

import net.propero.rdp.RdpPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

class IRP {

    public RdpPacket data;

    public int deviceId;

    public int completionId;


    public IRP(int fileId, int majorFunction, int minorFunction) {

        var bout = new ByteArrayOutputStream();
        var out = new DataOutputStream(bout);
    }

}
