
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
    AtomicInteger validCount, invalidCount, errCount;
    boolean ggMXOnly;
    Main parentWindows;
    String separator;

    public Worker(BlockingQueue<Email> queue, PrintWriter out, AtomicInteger validCount,
            AtomicInteger invalidCount, AtomicInteger errCount, boolean ggMXOnly, Main parentWindows, String separator) {
        this.queue = queue;
        this.out = out;
        this.validCount = validCount;
        this.invalidCount = invalidCount;
        this.errCount = errCount;
        this.ggMXOnly = ggMXOnly;
        this.parentWindows = parentWindows;
        this.separator = separator;
    }

    @Override
    public void run() {
        Email e;
        while ((e = queue.poll()) != null) {
            int code = 1;
            for (String ignore : IGNORE_LIST) {
                if (e.email.toLowerCase().contains(ignore)) {
                    code = 0;
                    e.log.append("IN INGORE LIST.\r\n");
                    break;
                }
            }

            if (code == 1) {
                if (e.status == 0 || e.status == 1) {
                    // Already checked (rechecking feature).
                    code = e.status;
                } else {
                    // 60s timeout.
                    code = EmailChecker.isAddressValid(e.email, 60000, e, ggMXOnly);
                }
            }
            e.status = code;
            switch (code) {
                case 1:
                    validCount.incrementAndGet();
                    break;
                case 0:
                    invalidCount.incrementAndGet();
                    break;
                default:
                    // Connection error.
                    errCount.incrementAndGet();
                    break;
            }
            String result = e.lineInfo + separator + e.getStatusString();
            synchronized (out) {
                out.println(result);
                out.flush();
            }
            parentWindows.updateEmail(e);
        }
    }
}
