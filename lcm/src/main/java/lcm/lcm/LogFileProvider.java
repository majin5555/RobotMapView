//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.IOException;
import lcm.logging.Log;

public class LogFileProvider implements Provider {
    LCM lcm;
    Log log;
    double speed;
    double delay;
    boolean verbose;
    double skip;
    boolean writemode;
    long nanotime_start;
    long utime_start;
    ReaderThread reader;
    boolean publishWarned = false;

    public LogFileProvider(LCM var1, URLParser var2) throws IOException {
        this.lcm = var1;
        String var3 = var2.get("network", "");
        this.speed = var2.get("speed", (double)1.0F);
        this.delay = var2.get("delay", (double)0.5F);
        this.verbose = var2.get("verbose", false);
        this.skip = var2.get("skip", (double)0.0F);
        this.writemode = var2.get("mode", "r").equals("w");
        if (this.writemode) {
            this.log = new Log(var3, "rw");
            this.nanotime_start = System.nanoTime();
            this.utime_start = System.currentTimeMillis() * 1000L;
        } else {
            this.log = new Log(var3, "r");
            this.reader = new ReaderThread();
            this.reader.start();
        }

    }

    public synchronized void publish(String var1, byte[] var2, int var3, int var4) {
        if (!this.writemode) {
            if (this.publishWarned) {
                return;
            }

            System.err.println("LogFileProvider opened in read mode, no publishing allowed.");
            this.publishWarned = true;
        }

        Log.Event var5 = new Log.Event();
        var5.utime = this.utime_start + (System.nanoTime() - this.nanotime_start) / 1000L;
        var5.eventNumber = 0L;
        var5.data = new byte[var4];
        System.arraycopy(var2, var3, var5.data, 0, var4);
        var5.channel = var1;

        try {
            this.log.write(var5);
        } catch (IOException var7) {
            System.err.println("ex: " + var7);
        }

    }

    public synchronized void subscribe(String var1) {
    }

    public void unsubscribe(String var1) {
    }

    public synchronized void close() {
        if (this.reader != null) {
            this.reader.interrupt();

            try {
                this.reader.join();
            } catch (InterruptedException var3) {
            }
        }

        this.reader = null;

        try {
            this.log.close();
        } catch (IOException var2) {
        }

        this.log = null;
    }

    class ReaderThread extends Thread {
        ReaderThread() {
            this.setDaemon(true);
        }

        public void run() {
            try {
                this.runEx();
            } catch (InterruptedException var2) {
                return;
            } catch (IOException var3) {
                var3.printStackTrace();
            }

        }

        void runEx() throws IOException, InterruptedException {
            LogFileProvider.this.log.seekPositionFraction(LogFileProvider.this.skip);

            while(LogFileProvider.this.lcm.getNumSubscriptions() == 0) {
                Thread.sleep(10L);
            }

            Thread.sleep((long)((int)(LogFileProvider.this.delay * (double)1000.0F)));
            long var1 = System.nanoTime() / 1000L;
            long var3 = -1L;
            double var5 = (double)0.0F;
            double var7 = (double)0.0F;
            long var9 = -1L;

            while(true) {
                Log.Event var11 = LogFileProvider.this.log.readNext();
                if (var3 > 0L) {
                    double var12 = (double)(var11.utime - var3) / (double)1000000.0F;
                    if (var12 > (double)0.0F && LogFileProvider.this.speed > (double)0.0F) {
                        var5 += var12 / LogFileProvider.this.speed;
                    }
                }

                var3 = var11.utime;
                long var18 = System.nanoTime() / 1000L;
                double var14 = (double)(var18 - var1) / (double)1000000.0F;
                var1 = var18;
                var5 -= var14;
                var7 += var14;
                if (var7 > (double)1.0F && LogFileProvider.this.verbose) {
                    double var16 = (double)(var3 - var9) / (double)1000000.0F;
                    var9 = var3;
                    System.err.printf("LogFile: rate = %8.3f, position = %8.3f %%\n", var16 / var7, LogFileProvider.this.log.getPositionFraction() * (double)100.0F);
                    var7 = (double)0.0F;
                }

                if (var5 > 0.001) {
                    int var19 = (int)(var5 * (double)1000.0F);
                    Thread.sleep((long)var19);
                }

                LogFileProvider.this.lcm.receiveMessage(var11.channel, var11.data, 0, var11.data.length);
            }
        }
    }
}
