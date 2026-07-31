//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package lcm.util;

public class ColorMapper {
    double minval;
    double maxval;
    int[] colors;
    double opaqueMax = Double.MAX_VALUE;
    double opaqueMin = -Double.MAX_VALUE;

    public ColorMapper(int[] var1, double var2, double var4) {
        this.colors = var1;
        this.minval = var2;
        this.maxval = var4;
    }

    public void setMinMax(double var1, double var3) {
        this.minval = var1;
        this.maxval = var3;
    }

    public void setOpaqueMax(double var1) {
        this.opaqueMax = var1;
    }

    public void setOpaqueMin(double var1) {
        this.opaqueMin = var1;
    }

    public boolean isVisible(double var1) {
        return !(var1 > this.opaqueMax) && !(var1 < this.opaqueMin);
    }

    public int map(double var1) {
        if (!this.isVisible(var1)) {
            return 0;
        } else {
            double var3 = (double)this.colors.length * (var1 - this.minval) / (this.maxval - this.minval);
            int var5 = (int)Math.floor(var3);
            if (var5 < 0) {
                var5 = 0;
            }

            if (var5 >= this.colors.length) {
                var5 = this.colors.length - 1;
            }

            int var6 = var5 + 1;
            if (var6 >= this.colors.length) {
                var6 = this.colors.length - 1;
            }

            double var7 = var3 - (double)var5;
            if (var7 < (double)0.0F) {
                var7 = (double)0.0F;
            }

            if (var7 > (double)1.0F) {
                var7 = (double)1.0F;
            }

            int var9 = 0;

            for(int var10 = 0; var10 < 4; ++var10) {
                int var11 = var10 * 8;
                int var12 = (int)((double)(this.colors[var5] >> var11 & 255) * ((double)1.0F - var7) + (double)(this.colors[var6] >> var11 & 255) * var7);
                var12 &= 255;
                var9 |= var12 << var11;
            }

            return var9 | -16777216;
        }
    }
}
