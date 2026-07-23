//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.lcm;

import java.io.DataOutput;

public final class LCMDataOutputStream implements DataOutput {
    byte[] buf;
    int pos;

    public LCMDataOutputStream() {
        this(512);
    }

    public LCMDataOutputStream(int var1) {
        this.buf = new byte[var1];
    }

    public LCMDataOutputStream(byte[] var1) {
        this.buf = var1;
    }

    public void reset() {
        this.pos = 0;
    }

    void ensureSpace(int var1) {
        if (this.pos + var1 >= this.buf.length) {
            int var2;
            for(var2 = this.buf.length; var2 < this.pos + var1; var2 *= 2) {
            }

            byte[] var3 = new byte[var2];
            System.arraycopy(this.buf, 0, var3, 0, this.pos);
            this.buf = var3;
        }

    }

    public void write(byte[] var1) {
        this.ensureSpace(var1.length);
        System.arraycopy(var1, 0, this.buf, this.pos, var1.length);
        this.pos += var1.length;
    }

    public void write(byte[] var1, int var2, int var3) {
        this.ensureSpace(var3);
        System.arraycopy(var1, var2, this.buf, this.pos, var3);
        this.pos += var3;
    }

    public void writeCharsAsBytes(char[] var1) {
        this.ensureSpace(var1.length);

        for(int var2 = 0; var2 < var1.length; ++var2) {
            this.write(var1[var2]);
        }

    }

    public void write(int var1) {
        this.ensureSpace(1);
        this.buf[this.pos++] = (byte)var1;
    }

    public void writeBoolean(boolean var1) {
        this.ensureSpace(1);
        this.buf[this.pos++] = (byte)(var1 ? 1 : 0);
    }

    public void writeByte(int var1) {
        this.ensureSpace(1);
        this.buf[this.pos++] = (byte)var1;
    }

    public void writeBytes(String var1) {
        this.ensureSpace(var1.length());

        for(int var2 = 0; var2 < var1.length(); ++var2) {
            this.buf[this.pos++] = (byte)var1.charAt(var2);
        }

    }

    public void writeChar(int var1) {
        this.writeShort(var1);
    }

    public void writeChars(String var1) {
        this.ensureSpace(2 * var1.length());

        for(int var2 = 0; var2 < var1.length(); ++var2) {
            char var3 = var1.charAt(var2);
            this.buf[this.pos++] = (byte)(var3 >>> 8);
            this.buf[this.pos++] = (byte)(var3 >>> 0);
        }

    }

    public void writeStringZ(String var1) {
        this.ensureSpace(var1.length() + 1);

        for(int var2 = 0; var2 < var1.length(); ++var2) {
            this.buf[this.pos++] = (byte)var1.charAt(var2);
        }

        this.buf[this.pos++] = 0;
    }

    public void writeDouble(double var1) {
        this.writeLong(Double.doubleToLongBits(var1));
    }

    public void writeFloat(float var1) {
        this.writeInt(Float.floatToIntBits(var1));
    }

    public void writeInt(int var1) {
        this.ensureSpace(4);
        this.buf[this.pos++] = (byte)(var1 >>> 24);
        this.buf[this.pos++] = (byte)(var1 >>> 16);
        this.buf[this.pos++] = (byte)(var1 >>> 8);
        this.buf[this.pos++] = (byte)(var1 >>> 0);
    }

    public void writeLong(long var1) {
        this.ensureSpace(8);
        this.buf[this.pos++] = (byte)((int)(var1 >>> 56));
        this.buf[this.pos++] = (byte)((int)(var1 >>> 48));
        this.buf[this.pos++] = (byte)((int)(var1 >>> 40));
        this.buf[this.pos++] = (byte)((int)(var1 >>> 32));
        this.buf[this.pos++] = (byte)((int)(var1 >>> 24));
        this.buf[this.pos++] = (byte)((int)(var1 >>> 16));
        this.buf[this.pos++] = (byte)((int)(var1 >>> 8));
        this.buf[this.pos++] = (byte)((int)(var1 >>> 0));
    }

    public void writeShort(int var1) {
        this.ensureSpace(2);
        this.buf[this.pos++] = (byte)(var1 >>> 8);
        this.buf[this.pos++] = (byte)(var1 >>> 0);
    }

    public void writeUTF(String var1) {
        assert false;

    }

    public byte[] toByteArray() {
        byte[] var1 = new byte[this.pos];
        System.arraycopy(this.buf, 0, var1, 0, this.pos);
        return var1;
    }

    public byte[] getBuffer() {
        return this.buf;
    }

    public int size() {
        return this.pos;
    }
}
