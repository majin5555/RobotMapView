//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class TCPService {
    ServerSocket serverSocket;
    AcceptThread acceptThread;
    ArrayList<ClientThread> clients = new ArrayList();
    int bytesCount = 0;

    public TCPService(int var1) throws IOException {
        this.serverSocket = new ServerSocket(var1);
        this.acceptThread = new AcceptThread();
        this.acceptThread.start();
        long var2 = System.currentTimeMillis();
        long var4 = System.currentTimeMillis();

        while(true) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException var10) {
            }

            long var6 = System.currentTimeMillis();
            double var8 = (double)(var6 - var4) / (double)1000.0F;
            var4 = var6;
            System.out.printf("%10.3f : %10.1f kB/s, %d clients\n", (double)(var6 - var2) / (double)1000.0F, (double)this.bytesCount / (double)1024.0F / var8, this.clients.size());
            this.bytesCount = 0;
        }
    }

    public void relay(byte[] var1, byte[] var2) {
        String var3 = new String(var1);
        synchronized(this.clients) {
            for(ClientThread var6 : this.clients) {
                var6.send(var3, var1, var2);
            }

        }
    }

    public static void main(String[] var0) {
        try {
            int var1 = 7700;
            if (var0.length > 0) {
                var1 = Integer.parseInt(var0[0]);
            }

            new TCPService(var1);
        } catch (IOException var2) {
            System.out.println("Ex: " + var2);
        }

    }

    class AcceptThread extends Thread {
        public void run() {
            while(true) {
                try {
                    Socket var1 = TCPService.this.serverSocket.accept();
                    ClientThread var2 = TCPService.this.new ClientThread(var1);
                    var2.start();
                    synchronized(TCPService.this.clients) {
                        TCPService.this.clients.add(var2);
                    }
                } catch (IOException var6) {
                }
            }
        }
    }

    class ClientThread extends Thread {
        Socket sock;
        DataInputStream ins;
        DataOutputStream outs;
        ArrayList<SubscriptionRecord> subscriptions = new ArrayList();

        public ClientThread(Socket var2) throws IOException {
            this.sock = var2;
            this.ins = new DataInputStream(var2.getInputStream());
            this.outs = new DataOutputStream(var2.getOutputStream());
            this.outs.writeInt(678828026);
            this.outs.writeInt(256);
        }

        public void run() {
            try {
                while(true) {
                    int var1 = this.ins.readInt();
                    if (var1 == 1) {
                        int var16 = this.ins.readInt();
                        byte[] var18 = new byte[var16];
                        this.ins.readFully(var18);
                        int var19 = this.ins.readInt();
                        byte[] var5 = new byte[var19];
                        this.ins.readFully(var5);
                        TCPService.this.relay(var18, var5);
                        TCPService var10000 = TCPService.this;
                        var10000.bytesCount += var16 + var19 + 8;
                    } else if (var1 == 2) {
                        int var15 = this.ins.readInt();
                        byte[] var17 = new byte[var15];
                        this.ins.readFully(var17);
                        synchronized(this.subscriptions) {
                            this.subscriptions.add(new SubscriptionRecord(new String(var17)));
                        }
                    } else if (var1 == 3) {
                        int var2 = this.ins.readInt();
                        byte[] var3 = new byte[var2];
                        this.ins.readFully(var3);
                        String var4 = new String(var3);
                        synchronized(this.subscriptions) {
                            int var6 = 0;

                            for(int var7 = this.subscriptions.size(); var6 < var7; ++var6) {
                                if (((SubscriptionRecord)this.subscriptions.get(var6)).regex.equals(var4)) {
                                    this.subscriptions.remove(var6);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (IOException var14) {
                try {
                    this.sock.close();
                } catch (IOException var11) {
                }

                synchronized(TCPService.this.clients) {
                    TCPService.this.clients.remove(this);
                }
            }
        }

        public void send(String var1, byte[] var2, byte[] var3) {
            try {
                synchronized(this.subscriptions) {
                    for(SubscriptionRecord var6 : this.subscriptions) {
                        if (var6.pat.matcher(var1).matches()) {
                            this.outs.writeInt(1);
                            this.outs.writeInt(var2.length);
                            this.outs.write(var2);
                            this.outs.writeInt(var3.length);
                            this.outs.write(var3);
                            this.outs.flush();
                            return;
                        }
                    }
                }
            } catch (IOException var9) {
            }

        }

        class SubscriptionRecord {
            String regex;
            Pattern pat;

            SubscriptionRecord(String var2) {
                this.regex = var2;
                this.pat = Pattern.compile(var2);
            }
        }
    }
}
