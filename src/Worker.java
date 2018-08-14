
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author hovinhthinh
 */
public class Worker extends Thread {

    private static final String[] IGNORE_LIST = new String[]{"@yahoo.com"};

    PrintWriter out;
    BlockingQueue<Email> queue;
    AtomicInteger validCount, invalidCount;
    boolean ggMXOnly;
    Main parentWindows;
    String separator;

    public Worker(BlockingQueue<Email> queue, PrintWriter out, AtomicInteger validCount,
            AtomicInteger invalidCount, boolean ggMXOnly, Main parentWindows, String separator) {
        this.queue = queue;
        this.out = out;
        this.validCount = validCount;
        this.invalidCount = invalidCount;
        this.ggMXOnly = ggMXOnly;
        this.parentWindows = parentWindows;
        this.separator = separator;
    }

    @Override
    public void run() {
        Email e;
        while ((e = queue.poll()) != null) {
            boolean valid = true;
            for (String ignore : IGNORE_LIST) {
                if (e.email.toLowerCase().contains(ignore)) {
                    valid = false;
                    e.log.append("IN INGORE LIST.\r\n");
                    break;
                }
            }
            if (valid) {
                // 30s timeout.
                valid = EmailChecker.isAddressValid(e.email, 30000, e, ggMXOnly);
            }
            if (valid) {
                validCount.incrementAndGet();
                e.status = 1;
            } else {
                invalidCount.incrementAndGet();
                e.status = 0;
            }
            String result = e.lineInfo + separator + (valid ? "valid" : "invalid");
            synchronized (out) {
                out.println(result);
                out.flush();
            }
            parentWindows.updateEmail(e);
        }
    }
}
