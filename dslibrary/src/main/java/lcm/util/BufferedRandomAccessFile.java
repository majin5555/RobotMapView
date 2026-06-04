//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRandomAccessFile {
    RandomAccessFile raf;
    static final int BUFFER_SIZE = 32768;
    boolean bufferDirty = false;
    byte[] buffer = new byte['耀'];
    long bufferOffset = -1L;
    int bufferLength = -1;
    int bufferPosition = -1;
    long fileLength;
    public static boolean check;

    public BufferedRandomAccessFile(File var1, String var2) throws IOException {
        this.raf = new RandomAccessFile(var1, var2);
        this.fileLength = this.raf.length();
        this.bufferSeek(0L);
    }

    public BufferedRandomAccessFile(String var1, String var2) throws IOException {
        this.raf = new RandomAccessFile(var1, var2);
        this.fileLength = this.raf.length();
        this.bufferSeek(0L);
    }

    public void close() throws IOException {
        this.flushBuffer();
        this.raf.close();
    }

    public long getFilePointer() {
        return this.bufferOffset + (long)this.bufferPosition;
    }

    public long length() throws IOException {
        return this.fileLength;
    }

    int max(int var1, int var2) {
        return var1 > var2 ? var1 : var2;
    }

    long max(long var1, long var3) {
        return var1 > var3 ? var1 : var3;
    }

    long min(long var1, long var3) {
        return var1 < var3 ? var1 : var3;
    }

    public void seek(long var1) throws IOException {
        this.bufferSeek(var1);
    }

    public void flush() throws IOException {
        this.flushBuffer();
    }

    void flushBuffer() throws IOException {
        if (this.bufferDirty) {
            this.raf.seek(this.bufferOffset);
            this.raf.write(this.buffer, 0, this.bufferLength);
            this.bufferDirty = false;
        }
    }

    void bufferSeek(long var1) throws IOException {
        this.flushBuffer();
        long var3 = var1 - (var1 & 32767L);
        if (var3 == this.bufferOffset) {
            this.bufferPosition = (int)(var1 - this.bufferOffset);
        } else {
            this.bufferOffset = var3;
            this.bufferLength = (int)this.min(32768L, this.fileLength - this.bufferOffset);
            if (this.bufferLength < 0) {
                this.bufferLength = 0;
            }

            this.bufferPosition = (int)(var1 - this.bufferOffset);
            this.raf.seek(this.bufferOffset);
            this.raf.readFully(this.buffer, 0, this.bufferLength);
        }
    }

    public final int read() throws IOException {
        if (this.bufferOffset + (long)this.bufferPosition >= this.fileLength) {
            throw new EOFException("EOF");
        } else {
            if (this.bufferPosition >= this.bufferLength) {
                this.bufferSeek(this.bufferOffset + (long)this.bufferPosition);
            }

            return this.buffer[this.bufferPosition++] & 255;
        }
    }

    public boolean hasMore() throws IOException {
        return (long)this.bufferPosition + this.bufferOffset < this.fileLength;
    }

    public byte peek() throws IOException {
        if (this.bufferPosition < this.bufferLength) {
            return this.buffer[this.bufferPosition];
        } else {
            this.raf.seek(this.bufferOffset + (long)this.bufferPosition);
            return this.raf.readByte();
        }
    }

    public void write(int var1) throws IOException {
        this.write((byte)(var1 & 255));
    }

    public void writeBoolean(boolean var1) throws IOException {
        this.write((byte)(var1 ? 1 : 0));
    }

    public boolean readBoolean() throws IOException {
        return this.read() != 0;
    }

    public void writeShort(short var1) throws IOException {
        this.write((byte)(var1 >> 8));
        this.write((byte)(var1 & 255));
    }

    public byte readByte() throws IOException {
        int var1 = this.read();
        return (byte)(var1 & 255);
    }

    public short readShort() throws IOException {
        short var1 = 0;
        var1 = (short)(var1 | this.read() << 8);
        var1 = (short)(var1 | this.read());
        return var1;
    }

    public void readFully(byte[] var1, int var2, int var3) throws IOException {
        while(var3 > 0) {
            int var4 = this.bufferLength - this.bufferPosition;
            int var5 = Math.min(var4, var3);
            if (var5 == 0) {
                this.flushBuffer();
                if (this.bufferOffset + (long)this.bufferPosition >= this.fileLength) {
                    throw new EOFException("EOF");
                }

                this.bufferSeek(this.bufferOffset + (long)this.bufferLength);
            } else {
                System.arraycopy(this.buffer, this.bufferPosition, var1, var2, var5);
                this.bufferPosition += var5;
                var2 += var5;
                var3 -= var5;
            }
        }

    }

    public void readFully(byte[] var1) throws IOException {
        this.readFully(var1, 0, var1.length);
    }

    public void writeInt(long var1) throws IOException {
        this.write((byte)((int)(var1 >> 24)));
        this.write((byte)((int)(var1 >> 16)));
        this.write((byte)((int)(var1 >> 8)));
        this.write((byte)((int)(var1 & 255L)));
    }

    public int readInt() throws IOException {
        int var1 = 0;
        var1 |= this.read() << 24;
        var1 |= this.read() << 16;
        var1 |= this.read() << 8;
        var1 |= this.read();
        return var1;
    }

    public void writeLong(long var1) throws IOException {
        this.write((byte)((int)(var1 >> 56)));
        this.write((byte)((int)(var1 >> 48)));
        this.write((byte)((int)(var1 >> 40)));
        this.write((byte)((int)(var1 >> 32)));
        this.write((byte)((int)(var1 >> 24)));
        this.write((byte)((int)(var1 >> 16)));
        this.write((byte)((int)(var1 >> 8)));
        this.write((byte)((int)(var1 & 255L)));
    }

    public long readLong() throws IOException {
        long var1 = 0L;
        var1 |= (long)this.read() << 56;
        var1 |= (long)this.read() << 48;
        var1 |= (long)this.read() << 40;
        var1 |= (long)this.read() << 32;
        var1 |= (long)this.read() << 24;
        var1 |= (long)this.read() << 16;
        var1 |= (long)this.read() << 8;
        var1 |= (long)this.read();
        return var1;
    }

    public void writeFloat(float var1) throws IOException {
        this.writeInt((long)Float.floatToIntBits(var1));
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    public void writeDouble(double var1) throws IOException {
        this.writeLong(Double.doubleToLongBits(var1));
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    public void writeUTF(String var1) throws IOException {
        this.writeShort((short)var1.length());

        for(int var2 = 0; var2 < var1.length(); ++var2) {
            var1.charAt(var2);
            this.write(var1.charAt(var2) & 255);
        }

    }

    public String readUTF() throws IOException {
        short var1 = this.readShort();
        StringBuffer var2 = new StringBuffer(var1);

        for(int var3 = 0; var3 < var1; ++var3) {
            var2.append((char)this.read());
        }

        return var2.toString();
    }

    public void write(byte[] var1, int var2, int var3) throws IOException {
        for(int var4 = var2; var4 < var2 + var3; ++var4) {
            this.write(var1[var4]);
        }

    }

    public void write(byte var1) throws IOException {
        this.bufferDirty = true;
        if (this.bufferPosition < this.bufferLength) {
            this.buffer[this.bufferPosition++] = var1;
        } else if (this.bufferLength < 32768) {
            this.buffer[this.bufferPosition++] = var1;
            ++this.bufferLength;
            ++this.fileLength;
        } else {
            this.flushBuffer();
            this.bufferSeek(this.bufferOffset + (long)this.bufferPosition);
            this.write(var1);
        }
    }

    public String readLineCheck() throws IOException {
        if (!check) {
            return this.readLine();
        } else {
            this.raf.seek(this.bufferOffset + (long)this.bufferPosition);
            String var1 = this.raf.readLine();
            String var2 = this.readLine();
            System.out.println("braf: " + var2);
            System.out.println(" raf: " + var1);
            return var2;
        }
    }

    public String readLine() throws IOException {
        StringBuilder var1 = null;

        while(true) {
            int var2 = this.bufferPosition;
            String var3 = null;

            while(this.bufferPosition < this.bufferLength) {
                char var4 = (char)(this.buffer[this.bufferPosition++] & 255);
                if (var4 == '\n') {
                    var3 = new String(this.buffer, var2, this.bufferPosition - var2 - 1);
                    break;
                }

                if (var4 == '\r') {
                    var3 = new String(this.buffer, var2, this.bufferPosition - var2 - 1);
                    break;
                }
            }

            if (var3 != null) {
                if (var1 == null) {
                    return var3;
                }

                var1.append(var3);
                return var1.toString();
            }

            var3 = new String(this.buffer, var2, this.bufferPosition - var2);
            if (var1 == null) {
                var1 = new StringBuilder();
            }

            var1.append(var3);
            if (this.bufferOffset + (long)this.bufferPosition >= this.fileLength) {
                if (var1.length() > 0) {
                    return var1.toString();
                }

                return null;
            }

            this.bufferSeek(this.bufferOffset + (long)this.bufferPosition);
        }
    }

    public static void main(String[] var0) {
        try {
            BufferedRandomAccessFile var1 = new BufferedRandomAccessFile(var0[0], "r");

            String var2;
            while((var2 = var1.readLine()) != null) {
                System.out.printf("^%s$\n", var2);
            }
        } catch (IOException var3) {
            System.out.println("Ex: " + var3);
        }

    }
}
