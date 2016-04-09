package org.yinwang.pysonar;

public class Progress {

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


    public Progress(long total, long width) {
        this.startTime = System.currentTimeMillis();
        this.lastTickTime = System.currentTimeMillis();
        this.lastCount = 0;
        this.lastRate = 0;
        this.lastAvgRate = 0;
        this.total = total;
        this.width = width;
        this.segSize = total / width;
        if (segSize == 0) {
            segSize = 1;
        }
    }


    public void tick(int n) {
        count += n;
        if (count > total) {
            total = count;
        }

        long elapsed = System.currentTimeMillis() - lastTickTime;

        if (elapsed > 500 || count == total) {
            $.msg_("\r");
            int dlen = (int) Math.ceil(Math.log10((double) total));
            $.msg_($.percent(count, total) + " (" +
                    $.formatNumber(count, dlen) +
                    " of " + $.formatNumber(total, dlen) + ")");

            int rate;
            if (elapsed > 1) {
                rate = (int) ((count - lastCount) / (elapsed / 1000.0));
            } else {
                rate = lastRate;
            }

            lastRate = rate;
            $.msg_("   SPEED: " + $.formatNumber(rate, MAX_SPEED_DIGITS) + "/s");

            long totalElapsed = System.currentTimeMillis() - startTime;
            int avgRate;

            if (totalElapsed > 1) {
                avgRate = (int) (count / (totalElapsed / 1000.0));
            } else {
                avgRate = lastAvgRate;
            }
            avgRate = avgRate == 0 ? 1 : avgRate;

            $.msg_("   AVG SPEED: " + $.formatNumber(avgRate, MAX_SPEED_DIGITS) + "/s");

            long remain = total - count;
            long remainTime = remain / avgRate * 1000;
            $.msg_("   ETA: " + $.formatTime(remainTime));

            $.msg_("   PARSE ERRS: " + Analyzer.self.failedToParse.size());

            $.msg_("       ");      // overflow area

            lastTickTime = System.currentTimeMillis();
            lastAvgRate = avgRate;
            lastCount = count;
        }
    }


    public void tick() {
        if (!Analyzer.self.hasOption("quiet")) {
            tick(1);
        }
    }
}
