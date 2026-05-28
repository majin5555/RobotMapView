//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

public class LCM {
    ArrayList<SubscriptionRecord> subscriptions = new ArrayList();
    ArrayList<Provider> providers = new ArrayList();
    HashMap<String, ArrayList<SubscriptionRecord>> subscriptionsMap = new HashMap();
    boolean closed = false;
    static LCM singleton;
    LCMDataOutputStream encodeBuffer = new LCMDataOutputStream(new byte[1024]);

    public LCM(String... var1) throws IOException {
        if (var1.length == 0) {
            String var2 = System.getenv("LCM_DEFAULT_URL");
            if (var2 == null) {
                var1 = new String[]{"udpm://224.0.0.1:7667?ttl=1"};
            } else {
                var1 = new String[]{var2};
            }
        }

        for(String var5 : var1) {
            if (null == var5 || var5.equals("")) {
                var5 = System.getenv("LCM_DEFAULT_URL");
                if (var5 == null) {
                    var5 = "udpm://224.0.0.1:7667?ttl=1";
                }
            }

            URLParser var6 = new URLParser(var5);
            String var7 = var6.get("protocol");
            if (var7.equals("udpm")) {
                this.providers.add(new UDPMulticastProvider(this, var6));
            } else if (var7.equals("tcpq")) {
                this.providers.add(new TCPProvider(this, var6));
            } else if (var7.equals("file")) {
                this.providers.add(new LogFileProvider(this, var6));
            } else {
                System.err.println("LCM: Unknown URL protocol: " + var7);
            }
        }

    }

    public static LCM getSingleton() {
        if (singleton == null) {
            try {
                singleton = new LCM(new String[0]);
            } catch (Exception var1) {
                System.err.println("LC singleton fail: " + var1);
                System.exit(-1);
                return null;
            }
        }

        return singleton;
    }

    public int getNumSubscriptions() {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            return this.subscriptions.size();
        }
    }

    public void publish(String var1, String var2) throws IOException {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            var2 = var2 + "\u0000";
            byte[] var3 = var2.getBytes();
            this.publish(var1, var3, 0, var3.length);
        }
    }

    public synchronized void publish(String var1, LCMEncodable var2) {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            try {
                this.encodeBuffer.reset();
                var2.encode(this.encodeBuffer);
                this.publish(var1, this.encodeBuffer.getBuffer(), 0, this.encodeBuffer.size());
            } catch (IOException var4) {
                System.err.println("LC publish fail: " + var4);
            }

        }
    }

    public synchronized void publish(String var1, byte[] var2, int var3, int var4) throws IOException {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            for(Provider var6 : this.providers) {
                var6.publish(var1, var2, var3, var4);
            }

        }
    }

    public void subscribe(String var1, LCMSubscriber var2) {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            SubscriptionRecord var3 = new SubscriptionRecord();
            var3.regex = var1;
            var3.pat = Pattern.compile(var1);
            var3.lcsub = var2;
            synchronized(this) {
                for(Provider var6 : this.providers) {
                    var6.subscribe(var1);
                }
            }

            synchronized(this.subscriptions) {
                this.subscriptions.add(var3);

                for(String var12 : this.subscriptionsMap.keySet()) {
                    if (var3.pat.matcher(var12).matches()) {
                        ArrayList var7 = (ArrayList)this.subscriptionsMap.get(var12);
                        var7.add(var3);
                    }
                }

            }
        }
    }

    public void unsubscribe(String var1, LCMSubscriber var2) {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            synchronized(this) {
                for(Provider var5 : this.providers) {
                    var5.unsubscribe(var1);
                }
            }

            synchronized(this.subscriptions) {
                Iterator var11 = this.subscriptions.iterator();

                while(var11.hasNext()) {
                    SubscriptionRecord var13 = (SubscriptionRecord)var11.next();
                    if ((var2 == null || var13.lcsub == var2) && (var1 == null || var13.regex.equals(var1))) {
                        var11.remove();
                    }
                }

                for(String var14 : this.subscriptionsMap.keySet()) {
                    Iterator var6 = ((ArrayList)this.subscriptionsMap.get(var14)).iterator();

                    while(var6.hasNext()) {
                        SubscriptionRecord var7 = (SubscriptionRecord)var6.next();
                        if ((var2 == null || var7.lcsub == var2) && (var1 == null || var7.regex.equals(var1))) {
                            var6.remove();
                        }
                    }
                }

            }
        }
    }

    public void receiveMessage(String var1, byte[] var2, int var3, int var4) {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            synchronized(this.subscriptions) {
                ArrayList<SubscriptionRecord> var6 = (ArrayList)this.subscriptionsMap.get(var1);
                if (var6 == null) {
                    var6 = new ArrayList<>();
                    this.subscriptionsMap.put(var1, var6);

                    for(SubscriptionRecord var8 : this.subscriptions) {
                        if (var8.pat.matcher(var1).matches()) {
                            var6.add(var8);
                        }
                    }
                }

                for(SubscriptionRecord var12 : var6) {
                    var12.lcsub.messageReceived(this, var1, new LCMDataInputStream(var2, var3, var4));
                }

            }
        }
    }

    public synchronized void subscribeAll(LCMSubscriber var1) {
        this.subscribe(".*", var1);
    }

    public synchronized void close() {
        if (this.closed) {
            throw new IllegalStateException();
        } else {
            for(Provider var2 : this.providers) {
                var2.close();
            }

            this.providers = null;
            this.closed = true;
        }
    }

    public static void main(String[] var0) {
        LCM var1;
        try {
            var1 = new LCM(new String[0]);
        } catch (IOException var3) {
            System.err.println("ex: " + var3);
            return;
        }

        var1.subscribeAll(new SimpleSubscriber());

        while(true) {
            try {
                Thread.sleep(1000L);
                var1.publish("TEST", "foobar");
            } catch (Exception var4) {
                System.err.println("ex: " + var4);
            }
        }
    }

    static class SubscriptionRecord {
        String regex;
        Pattern pat;
        LCMSubscriber lcsub;
    }

    static class SimpleSubscriber implements LCMSubscriber {
        public void messageReceived(LCM var1, String var2, LCMDataInputStream var3) {
            System.err.println("RECV: " + var2);
        }
    }
}
