//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.logging;

import java.io.IOException;

import lcm.lcm.LCMDataOutputStream;
import lcm.lcm.LCMEncodable;
import lcm.util.BufferedRandomAccessFile;

public class Log {
    BufferedRandomAccessFile raf;
    static final int LOG_MAGIC = -308159999;
    String path;
    long numMessagesWritten = 0L;

    public Log(String var1, String var2) throws IOException {
        this.path = var1;
        this.raf = new BufferedRandomAccessFile(var1, var2);
    }

    public String getPath() {
        return this.path;
    }

    public void flush() throws IOException {
        this.raf.flush();
    }

    public synchronized Event readNext() throws IOException {
        int var1 = 0;
        Event var2 = new Event();
        int var3 = 0;
        int var4 = 0;

        while(true) {
            int var5 = this.raf.readByte() & 255;
            var1 = var1 << 8 | var5;
            if (var1 == -308159999) {
                var2.eventNumber = this.raf.readLong();
                var2.utime = this.raf.readLong();
                var3 = this.raf.readInt();
                var4 = this.raf.readInt();
                if (var3 > 0 && var4 > 0 && var3 < 256 && var4 < 16777216) {
                    byte[] var8 = new byte[var3];
                    var2.data = new byte[var4];
                    this.raf.readFully(var8);
                    var2.channel = new String(var8);
                    this.raf.readFully(var2.data);
                    return var2;
                }

                System.out.printf("Bad log event eventnumber = 0x%08x utime = 0x%08x channellen = 0x%08x datalen=0x%08x\n", var2.eventNumber, var2.utime, var3, var4);
            }
        }
    }

    public synchronized double getPositionFraction() throws IOException {
        return (double)this.raf.getFilePointer() / (double)this.raf.length();
    }

    public synchronized void seekPositionFraction(double var1) throws IOException {
        this.raf.seek((long)((double)this.raf.length() * var1));
    }

    public synchronized void write(Event var1) throws IOException {
        byte[] var2 = var1.channel.getBytes();
        this.raf.writeInt(-308159999L);
        this.raf.writeLong(var1.eventNumber);
        this.raf.writeLong(var1.utime);
        this.raf.writeInt((long)var2.length);
        this.raf.writeInt((long)var1.data.length);
        this.raf.write(var2, 0, var2.length);
        this.raf.write(var1.data, 0, var1.data.length);
    }

    public synchronized void write(long var1, String var3, LCMEncodable var4) throws IOException {
        Event var5 = new Event();
        var5.utime = var1;
        var5.channel = var3;
        LCMDataOutputStream var6 = new LCMDataOutputStream();
        var4.encode(var6);
        var5.data = var6.toByteArray();
        var5.eventNumber = this.numMessagesWritten;
        this.write(var5);
        ++this.numMessagesWritten;
    }

    public synchronized void close() throws IOException {
        this.raf.close();
    }

    public static class Event {
        public long utime;
        public long eventNumber;
        public byte[] data;
        public String channel;
    }
}
