package org.yinwang.pysonar;

import java.text.DecimalFormat;

public class FancyProgress {

    long startTime;
    long lastTickTime;
    long lastCount;
    int lastRate;
    long total;
    long count;
    long width;
    long segSize;


    public FancyProgress(long total, long width) {
        this.startTime = System.currentTimeMillis();
        this.lastTickTime = System.currentTimeMillis();
        this.lastCount = 0;
        this.lastRate = 0;
        this.total = total;
        this.width = width;
        this.segSize = total / width;
        if (segSize == 0) segSize = 1;
    }


    public void setTotal(long total) {
        this.total = total;
    }


    public void tick(int n) {
        count += n;
        if (count > total) {
            total = count;
        }

        long elapsed = System.currentTimeMillis() - lastTickTime;

        if (elapsed > 500 || count == total || count % segSize == 0) {
            int len = (int) Math.floor(width * count / total);

            System.out.print("\r[");

            for (int i = 0; i < len; i++) {
                System.out.print("=");
            }

            for (int j = len; j < width; j++) {
                System.out.print(" ");
            }

            System.out.print("]  ");
            System.out.print(Util.percent(count, total) + " (" + count + "/" + total + ")");

            DecimalFormat df = new DecimalFormat("#");

            double rate;

            if (elapsed > 1) {
                rate = (count - lastCount) / (elapsed / 1000.0);
            } else {
                rate = lastRate;
            }

            System.out.print(" " + df.format(rate) + "/s    ");

            lastTickTime = System.currentTimeMillis();
            lastCount = count;
        }
    }


    public void tick() {
        tick(1);
    }
}
