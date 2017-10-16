package com.ymlion.rtmp.bean;

import com.ymlion.rtmp.util.ByteUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.ymlion.rtmp.util.ByteUtil.writeInt;

/**
 * Created by YMlion on 2017/10/13.
 */

public class RtmpHeader {
    /**
     * chunk type, 2 bit
     */
    public int fmt;
    /**
     * chunk stream id, basic head size - 2 bit, little-endian
     */
    public int CSID;
    /**
     * time stamp, 3 bytes
     */
    public int timestamp;
    /**
     * message length, indicate chunk data size, 3 bytes
     */
    public int msgLength;

    /**
     * message type, like video, audio etc. 1 byte
     */
    public int msgType;

    /**
     * message stream id, 4 bytes, little-endian
     */
    public int msgSID;

    public void write(OutputStream out) throws IOException {
        int total;
        byte[] header = new byte[12];
        if (CSID < 64) {// [3, 63]
            total = 1;
            header[0] = (byte) (fmt << 6 | CSID);
        } else if (CSID < 320) {// [64, 319]
            total = 2;
            header[0] = (byte) (fmt << 6 | (CSID & 0x3f));
            header[1] = (byte) (CSID >> 6);
        } else {// [320, 65599]
            total = 3;
            header[0] = (byte) (fmt << 6 | (CSID & 0x3f));
            header[1] = (byte) (CSID >> 6 & 0xff);
            header[2] = (byte) (CSID >> 14);
        }
        switch (fmt) {
            case 0:
                writeInt(3, timestamp, header, total);
                writeInt(3, msgLength, header, total + 3);
                writeInt(1, msgType, header, total + 6);
                header[8] = (byte) (msgSID & 0xff);
                header[9] = (byte) (msgSID >> 8 & 0xff);
                header[10] = (byte) (msgSID >> 16 & 0xff);
                header[8] = (byte) (msgSID >> 24 & 0xff);
                total += 11;
                break;
            case 1:
                writeInt(3, timestamp, header, total);
                writeInt(3, msgLength, header, total + 3);
                writeInt(1, msgType, header, total + 6);
                total += 7;
                break;
            case 2:
                writeInt(3, timestamp, header, total);
                total += 3;
                break;
            default:
                break;
        }
        out.write(header, 0, total);
    }

    public int read(InputStream in) throws IOException {
        int b1 = in.read();
        if (b1 == -1) {
            return -1;
        }
        fmt = b1 >>> 6;
        CSID = b1 & 0x3f;
        System.out.println("rtmp chunk header fmt is " + fmt + " ; cs id is " + CSID);
        byte[] head = new byte[11];
        int r = 0;
        switch (fmt) {
            case 0:
                r = in.read(head);
                msgLength = ByteUtil.bytes2Int(3, head, 3);
                msgType = head[6];
                System.out.println("rtmp chunk header type is " + msgType);
                break;
            case 1:
                r = in.read(head, 0, 7);
                msgLength = ByteUtil.bytes2Int(3, head, 3);
                break;
            case 2:
                r = in.read(head, 0, 3);
                msgLength = 128;
                break;
            default:
                msgLength = 128;
                break;
        }
        if (r <= 0) {
            return -1;
        }

        return msgLength;
    }
}