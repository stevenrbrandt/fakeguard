import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class Main {
    final static int nmax = 3*20;
    final static Map<Integer,IntVar> intmap = new HashMap<>();
    final static AtomicInteger testCount = new AtomicInteger(0);

    public static void finalTest(int num) {
        System.out.println("Done: "+num);
        int n = testCount.incrementAndGet();
        assert n == 2*num || n == 2*num-1 : String.format("%d != %d",testCount.get(),num);
    }

    static Guard mkGuard() {
        Guard g = new Guard();
        intmap.put(g.id, new IntVar());
        return g;
    }
    static void incr(Guard g) {
        IntVar iv = intmap.get(g.id);
        assert GuardTask.GUARDS_HELD.get().contains(g);
        iv.incr();
    }

    public static void main(String[] args) {
        try {
            main();
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(3);
        }
    }
    public static void main() throws Exception {
        Guard g = mkGuard();
        g.runGuarded(()->{ System.out.println("Hello"); });
        g.runGuarded(()->{ System.out.println("World"); });
        Runnable w = ()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                g.runGuarded(()->{
                    if(n == nmax-1) finalTest(1);
                    intmap.get(g.id).incr();
                });
            }
        };

        // Test one guard
        {
            Thread t1 = new DieThread(w);
            Thread t2 = new DieThread(w);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
        System.out.println("Test of one guard complete");

        Guard g2 = mkGuard();

        Guard.runGuarded(()->{ System.out.println("Two"); },g,g2);

        Runnable w2=()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                final Runnable r = new Runnable() {
                    volatile boolean hasRun = false;
                    public void run() {
                        assert !hasRun;
                        hasRun = true;
                        //System.out.println("n="+n+" "+ThreadID.get());
                        if(n == nmax-1) finalTest(2);
                        if(n % 3 == 0) incr(g);
                        else if(n % 3 == 1) incr(g2);
                        else if(n % 3 == 2) {
                            incr(g);
                            incr(g2);
                        }
                    }
                };
                if(n % 3 == 0)
                    g.runGuarded(r);
                else if(n % 3 == 1)
                    g2.runGuarded(r);
                else if(n % 3 == 2)
                    Guard.runGuarded(r,g,g2);
            }
        };

        // Test two guards
        {
            Thread t1 = new DieThread(w2);
            Thread t2 = new DieThread(w2);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
        System.out.println("Test of two guards complete");

        Guard g3 = mkGuard();
        Runnable w3=()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                final Runnable r = ()->{
                    if(n == nmax-1) finalTest(3);
                    if(n % 3 == 0) {
                        incr(g);
                        incr(g2);
                    } else if(n % 3 == 1) {
                        incr(g2);
                        incr(g3);
                    } else if(n % 3 == 2) {
                        incr(g);
                        incr(g2);
                        incr(g3);
                    }
                };
                if(n % 3 == 0)
                    Guard.runGuarded(r,g,g2);
                else if(n % 3 == 1)
                    Guard.runGuarded(r,g2,g3);
                else if(n % 3 == 2)
                    Guard.runGuarded(r,g,g2,g3);
            }
        };

        // Test three guards
        {
            Thread t1 = new DieThread(w3);
            Thread t2 = new DieThread(w3);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
        System.out.println("Test of three guards complete");

        Guard g4 = mkGuard();
        Runnable w4=()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                final Runnable r = ()->{
                    if(n % 3 == 0) {
                        incr(g); incr(g2); incr(g3);
                    } else if(n % 3 == 1) {
                        incr(g2); incr(g3); incr(g4);
                    } else if(n % 3 == 2) {
                        incr(g); incr(g2); incr(g3); incr(g4);
                    }
                    if(n == nmax-1) finalTest(4);
                };
                if(n % 3 == 0)
                    Guard.runGuarded(r,g,g2,g3);
                else if(n % 3 == 1)
                    Guard.runGuarded(r,g2,g3,g4);
                else if(n % 3 == 2)
                    Guard.runGuarded(r,g,g2,g3,g4);
            }
        };

        // Test three guards
        {
            Thread t1 = new DieThread(w4);
            Thread t2 = new DieThread(w4);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
        System.out.println("Test of tree guards complete");
        assert testCount.get() == 8;
        System.out.println("All tests complete");
    }
}
