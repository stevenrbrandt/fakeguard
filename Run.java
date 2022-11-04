import java.util.TreeSet;

public class Run {
    public static void run(Runnable r) {
        try {
            r.run();
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(2);
        } finally {
            ;
        }
    }
}
