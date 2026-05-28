//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.HashMap;

public class UDPMulticastProvider implements Provider {
    MulticastSocket sock;
    static final String DEFAULT_NETWORK = "239.255.76.67:7667";
    static final int DEFAULT_TTL = 0;
    static final int MAGIC_SHORT = 1279471666;
    static final int MAGIC_LONG = 1279471667;
    static final int FRAGMENTATION_THRESHOLD = 64000;
    ReaderThread reader;
    int msgSeqNumber = 0;
    HashMap<SocketAddress, FragmentBuffer> fragBufs = new HashMap();
    LCM lcm;
    InetAddress inetAddr;
    int inetPort;

    public UDPMulticastProvider(LCM var1, URLParser var2) throws IOException {
        this.lcm = var1;
        String[] var3 = var2.get("network", "239.255.76.67:7667").split(":");
        this.inetAddr = InetAddress.getByName(var3[0]);
        this.inetPort = Integer.valueOf(var3[1]);
        this.sock = new MulticastSocket(this.inetPort);
        this.sock.setReuseAddress(true);
        this.sock.setLoopbackMode(false);
        int var4 = var2.get("ttl", 0);
        if (var4 == 0) {
            System.err.println("LCM: TTL set to zero, traffic will not leave localhost.");
        } else if (var4 > 1) {
            System.err.println("LCM: TTL set to > 1... That's almost never correct!");
        } else {
            System.err.println("LCM: TTL set to 1.");
        }

        this.sock.setTimeToLive(var2.get("ttl", 0));
        this.sock.joinGroup(this.inetAddr);
    }

    public synchronized void publish(String var1, byte[] var2, int var3, int var4) {
        try {
            this.publishEx(var1, var2, var3, var4);
        } catch (Exception var6) {
            System.err.println("ex: " + var6);
        }

    }

    public synchronized void subscribe(String var1) {
        if (null == this.reader) {
            this.reader = new ReaderThread();
            this.reader.start();
        }

    }

    public void unsubscribe(String var1) {
    }

    public synchronized void close() {
        if (null != this.reader) {
            this.reader.interrupt();

            try {
                this.reader.join();
            } catch (InterruptedException var2) {
            }
        }

        this.reader = null;
        this.sock.close();
        this.sock = null;
        this.fragBufs = null;
    }

    void publishEx(String var1, byte[] var2, int var3, int var4) throws Exception {
        byte[] var5 = var1.getBytes("US-ASCII");
        int var6 = var5.length + var4;
        if (var6 <= 64000) {
            LCMDataOutputStream var7 = new LCMDataOutputStream(var4 + var1.length() + 32);
            var7.writeInt(1279471666);
            var7.writeInt(this.msgSeqNumber);
            var7.writeStringZ(var1);
            var7.write(var2, var3, var4);
            this.sock.send(new DatagramPacket(var7.getBuffer(), 0, var7.size(), this.inetAddr, this.inetPort));
        } else {
            int var15 = var6 / '切';
            if (var6 % '切' > 0) {
                ++var15;
            }

            if (var15 > 65535) {
                System.err.println("LC error: too much data for a single message");
                return;
            }

            ByteArrayOutputStream var8 = new ByteArrayOutputStream(64010);
            DataOutputStream var9 = new DataOutputStream(var8);
            int var10 = 0;
            byte var11 = 0;
            var9.writeInt(1279471667);
            var9.writeInt(this.msgSeqNumber);
            var9.writeInt(var4);
            var9.writeInt(var10);
            var9.writeShort(var11);
            var9.writeShort(var15);
            var9.write(var5, 0, var5.length);
            var9.writeByte(0);
            int var12 = '切' - (var5.length + 1);
            var9.write(var2, var3, var12);
            byte[] var13 = var8.toByteArray();
            this.sock.send(new DatagramPacket(var13, 0, var13.length, this.inetAddr, this.inetPort));
            var10 += var12;

            for(int var19 = 1; var19 < var15; ++var19) {
                var8 = new ByteArrayOutputStream(64010);
                var9 = new DataOutputStream(var8);
                var9.writeInt(1279471667);
                var9.writeInt(this.msgSeqNumber);
                var9.writeInt(var4);
                var9.writeInt(var10);
                var9.writeShort(var19);
                var9.writeShort(var15);
                int var14 = Math.min(64000, var4 - var10);
                var9.write(var2, var3 + var10, var14);
                var13 = var8.toByteArray();
                this.sock.send(new DatagramPacket(var13, 0, var13.length, this.inetAddr, this.inetPort));
                var10 += var14;
            }
        }

        ++this.msgSeqNumber;
    }

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.err.println("LCM: Disabling IPV6 support");
    }

    class FragmentBuffer {
        SocketAddress from = null;
        String channel = null;
        int msgSeqNumber = 0;
        int data_size = 0;
        int fragments_remaining = 0;
        byte[] data = null;
        boolean[] frag_received;

        FragmentBuffer(SocketAddress var2, String var3, int var4, int var5, int var6) {
            this.from = var2;
            this.channel = var3;
            this.msgSeqNumber = var4;
            this.data_size = var5;
            this.fragments_remaining = var6;
            this.data = new byte[var5];
            this.frag_received = new boolean[var6];
        }
    }

    class ReaderThread extends Thread {
        ReaderThread() {
            this.setDaemon(true);
        }

        public void run() {
            DatagramPacket var1 = new DatagramPacket(new byte[65536], 65536);

            while(true) {
                try {
                    UDPMulticastProvider.this.sock.receive(var1);
                    this.handlePacket(var1);
                } catch (IOException var3) {
                    System.err.println("ex: " + var3);
                }
            }
        }

        void handleShortMessage(DatagramPacket var1, LCMDataInputStream var2) throws IOException {
            int var3 = var2.readInt();
            String var4 = var2.readStringZ();
            UDPMulticastProvider.this.lcm.receiveMessage(var4, var2.getBuffer(), var2.getBufferOffset(), var2.available());
        }

        void handleFragment(DatagramPacket var1, LCMDataInputStream var2) throws IOException {
            int var3 = var2.readInt();
            int var4 = var2.readInt() & -1;
            int var5 = var2.readInt() & -1;
            int var6 = var2.readShort() & '\uffff';
            int var7 = var2.readShort() & '\uffff';
            byte[] var8 = new byte[var2.available()];
            var2.readFully(var8);
            if (var2.available() > 0) {
                System.err.println("Unread data! " + var2.available());
            }

            int var9 = 0;
            int var10 = var8.length;
            SocketAddress var11 = var1.getSocketAddress();
            FragmentBuffer var12 = (FragmentBuffer)UDPMulticastProvider.this.fragBufs.get(var11);
            if (var12 != null && (var12.msgSeqNumber != var3 || var12.data_size != var4)) {
                UDPMulticastProvider.this.fragBufs.remove(var12.from);
                var12 = null;
            }

            if (null == var12 && 0 == var6) {
                int var13;
                for(var13 = 0; var13 < var8.length && 0 != var8[var13]; ++var13) {
                }

                var9 = var13 + 1;
                var10 -= var13 + 1;
                String var14 = new String(var8, 0, var13, "US-ASCII");
                var12 = UDPMulticastProvider.this.new FragmentBuffer(var11, var14, var3, var4, var7);
                UDPMulticastProvider.this.fragBufs.put(var12.from, var12);
            }

            if (null != var12) {
                if (var5 + var10 > var12.data_size) {
                    System.err.println("LC: dropping invalid fragment");
                    UDPMulticastProvider.this.fragBufs.remove(var12.from);
                } else {
                    if (!var12.frag_received[var6]) {
                        var12.frag_received[var6] = true;
                        System.arraycopy(var8, var9, var12.data, var5, var10);
                        --var12.fragments_remaining;
                    }

                    if (0 == var12.fragments_remaining) {
                        UDPMulticastProvider.this.lcm.receiveMessage(var12.channel, var12.data, 0, var12.data_size);
                        UDPMulticastProvider.this.fragBufs.remove(var12.from);
                    }

                }
            }
        }

        void handlePacket(DatagramPacket var1) throws IOException {
            LCMDataInputStream var2 = new LCMDataInputStream(var1.getData(), var1.getOffset(), var1.getLength());
            int var3 = var2.readInt();
            if (var3 == 1279471666) {
                this.handleShortMessage(var1, var2);
            } else {
                if (var3 != 1279471667) {
                    System.err.println("bad magic: " + Integer.toHexString(var3));
                    return;
                }

                this.handleFragment(var1, var2);
            }

        }
    }
}
