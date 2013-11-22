package org.yinwang.pysonar;

public class FancyProgress {

    private static final int MAX_SPEED_DIGITS = 5;

    long startTime;
    long lastTickTime;
    long lastCount;
    int lastRate;
    int lastAvgRate;
    long total;
    long count;
    long width;
    long segSize;


    public FancyProgress(long total, long width) {
        this.startTime = System.currentTimeMillis();
        this.lastTickTime = System.currentTimeMillis();
        this.lastCount = 0;
        this.lastRate = 0;
        this.lastAvgRate = 0;
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

        if (elapsed > 500 || count == total) {
            System.out.print("\r");

//            int len = (int) Math.floor(width * count / total);
//            System.out.print("[");
//
//            for (int i = 0; i < len; i++) {
//                System.out.print("=");
//            }
//
//            for (int j = len; j < width; j++) {
//                System.out.print(" ");
//            }
//
//            System.out.print("]  ");

            int dlen = (int) Math.ceil(Math.log10((double) total));

            System.out.print(Util.percent(count, total) + " (" +
                    Util.format(count, dlen) +
                    " of " + Util.format(total, dlen) + ")");

            int rate;
            if (elapsed > 1) {
                rate = (int) ((count - lastCount) / (elapsed / 1000.0));
            } else {
                rate = lastRate;
            }

            lastRate = rate;
            System.out.print("   speed: " + Util.format(rate, MAX_SPEED_DIGITS) + "/s");

            long totalElapsed = System.currentTimeMillis() - startTime;
            int avgRate;

            if (totalElapsed > 1) {
                avgRate = (int) (count / (totalElapsed / 1000.0));
            } else {
                avgRate = lastAvgRate;
            }
            lastAvgRate = avgRate;
            System.out.print("   avg speed: " + Util.format(avgRate, MAX_SPEED_DIGITS) + "/s");
            System.out.print("       ");      // overflow area

            lastTickTime = System.currentTimeMillis();
            lastCount = count;
        }
    }


    public void tick() {
        tick(1);
    }
}
