//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;

public class TCPProvider implements Provider {
    LCM lcm;
    static final int DEFAULT_PORT = 7700;
    static final String DEFAULT_NETWORK = "127.0.0.1:7700";
    InetAddress inetAddr;
    int inetPort;
    TCPThread tcp;
    public static final int MAGIC_SERVER = 678828026;
    public static final int MAGIC_CLIENT = 678828027;
    public static final int VERSION = 256;
    public static final int MESSAGE_TYPE_PUBLISH = 1;
    public static final int MESSAGE_TYPE_SUBSCRIBE = 2;
    public static final int MESSAGE_TYPE_UNSUBSCRIBE = 3;
    HashSet<String> subscriptions = new HashSet();

    public TCPProvider(LCM var1, URLParser var2) throws IOException {
        this.lcm = var1;
        String[] var3 = var2.get("network", "127.0.0.1:7700").split(":");
        if (var3.length == 1) {
            this.inetAddr = InetAddress.getByName(var3[0]);
            this.inetPort = 7700;
        } else if (var3.length == 2) {
            this.inetAddr = InetAddress.getByName(var3[0]);
            this.inetPort = Integer.valueOf(var3[1]);
        } else {
            System.err.println("TCPProvider: Don't know how to parse " + var2.get("network", "127.0.0.1:7700"));
            System.exit(-1);
        }

        this.tcp = new TCPThread();
        this.tcp.start();
    }

    public synchronized void publish(String var1, byte[] var2, int var3, int var4) {
        try {
            this.publishEx(var1, var2, var3, var4);
        } catch (Exception var6) {
            System.err.println("TCPProvider ex: " + var6);
        }

    }

    byte[] stringToBytes(String var1) {
        try {
            return var1.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException var3) {
            System.err.println("lcm.TCPProvider: Bad channel name" + var1);
            throw new RuntimeException("Don't know how to recover from this");
        }
    }

    public synchronized void subscribe(String var1) {
        this.subscriptions.add(var1);
        this.tcp.sendSubscribe(var1);
    }

    public synchronized void unsubscribe(String var1) {
        this.subscriptions.remove(var1);
        this.tcp.sendUnsubscribe(var1);
    }

    public synchronized void close() {
        if (null != this.tcp) {
            this.tcp.close();

            try {
                this.tcp.join();
            } catch (InterruptedException var2) {
            }
        }

        this.tcp = null;
    }

    static final void safeSleep(int var0) {
        try {
            Thread.sleep((long)var0);
        } catch (InterruptedException var2) {
        }

    }

    void publishEx(String var1, byte[] var2, int var3, int var4) throws Exception {
        byte[] var5 = this.stringToBytes(var1);
        int var6 = var5.length + var4;
        ByteArrayOutputStream var7 = new ByteArrayOutputStream(var4 + var1.length() + 32);
        DataOutputStream var8 = new DataOutputStream(var7);
        var8.writeInt(1);
        var8.writeInt(var5.length);
        var8.write(var5, 0, var5.length);
        var8.writeInt(var4);
        var8.write(var2, var3, var4);
        this.tcp.write(var7.toByteArray());
    }

    class TCPThread extends Thread {
        Socket sock;
        DataInputStream ins;
        OutputStream outs;
        boolean exit = false;
        int serverVersion;

        synchronized void write(byte[] var1) throws IOException {
            if (this.outs != null) {
                this.outs.write(var1);
                this.outs.flush();
            }
        }

        synchronized void sendSubscribe(String var1) {
            byte[] var2 = TCPProvider.this.stringToBytes(var1);

            try {
                ByteArrayOutputStream var3 = new ByteArrayOutputStream(var1.length() + 8);
                DataOutputStream var4 = new DataOutputStream(var3);
                var4.writeInt(2);
                var4.writeInt(var2.length);
                var4.write(var2, 0, var2.length);
                this.write(var3.toByteArray());
            } catch (IOException var5) {
                System.out.println("ex: " + var5);
            }

        }

        synchronized void sendUnsubscribe(String var1) {
            byte[] var2 = TCPProvider.this.stringToBytes(var1);

            try {
                ByteArrayOutputStream var3 = new ByteArrayOutputStream(var1.length() + 8);
                DataOutputStream var4 = new DataOutputStream(var3);
                var4.writeInt(3);
                var4.writeInt(var2.length);
                var4.write(var2, 0, var2.length);
                this.write(var3.toByteArray());
            } catch (IOException var5) {
            }

        }

        public void run() {
            while(!this.exit) {
                synchronized(this) {
                    try {
                        this.sock = new Socket(TCPProvider.this.inetAddr, TCPProvider.this.inetPort);
                        OutputStream var2 = this.sock.getOutputStream();
                        DataOutputStream var3 = new DataOutputStream(var2);
                        var3.writeInt(678828027);
                        var3.writeInt(256);
                        var3.flush();
                        this.outs = var2;
                        this.ins = new DataInputStream(new BufferedInputStream(this.sock.getInputStream()));
                        int var4 = this.ins.readInt();
                        if (var4 == 678828026) {
                            this.serverVersion = this.ins.readInt();
                        } else {
                            this.sock.close();
                            continue;
                        }
                    } catch (IOException var7) {
                        System.err.println("lcm.TCPProvider: Unable to connect to " + TCPProvider.this.inetAddr + ":" + TCPProvider.this.inetPort);
                        TCPProvider.safeSleep(500);
                        continue;
                    }

                    for(String var11 : TCPProvider.this.subscriptions) {
                        System.out.println("resending subscription " + var11);
                        this.sendSubscribe(var11);
                    }
                }

                try {
                    while(!this.exit) {
                        int var1 = this.ins.readInt();
                        int var10 = this.ins.readInt();
                        byte[] var12 = new byte[var10];
                        this.ins.readFully(var12);
                        int var13 = this.ins.readInt();
                        byte[] var5 = new byte[var13];
                        this.ins.readFully(var5);
                        TCPProvider.this.lcm.receiveMessage(new String(var12), var5, 0, var5.length);
                    }
                } catch (IOException var6) {
                }
            }

        }

        void close() {
            try {
                this.sock.close();
            } catch (IOException var2) {
            }

            this.exit = true;
        }

        OutputStream getOutputStream() {
            return this.outs;
        }
    }
}
