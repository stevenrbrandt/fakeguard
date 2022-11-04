public class DieThread extends Thread {
    public DieThread(Runnable r) {
        super(()->{
            try {
                r.run();
            } catch(Throwable t) {
                t.printStackTrace();
                System.exit(4);
            }
        });
    }
}
