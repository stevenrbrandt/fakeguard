public class Main {
    final static int nmax = 3*1;
    public static void main(String[] args) {
        try {
            main();
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(3);
        }
    }
    public static void main() throws Exception {
        Guard g = new Guard();
        IntVar iv = new IntVar();
        g.runGuarded(()->{ System.out.println("Hello"); });
        GuardCheck.assertClean();
        g.runGuarded(()->{ System.out.println("World"); });
        GuardCheck.assertClean();
        Runnable w = ()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                g.runGuarded(()->{
                    if(n == nmax-1) System.out.println("Done");
                    iv.incr();
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
        GuardCheck.assertClean();

        Guard g2 = new Guard();
        IntVar iv2 = new IntVar();

        Guard.runGuarded(()->{ System.out.println("Two"); },g,g2);
        GuardCheck.assertClean();

        Runnable w2=()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                final Runnable r = new Runnable() {
                    volatile boolean hasRun = false;
                    public void run() {
                        assert !hasRun;
                        hasRun = true;
                        //System.out.println("n="+n+" "+ThreadID.get());
                        if(n == nmax-1) System.out.println("Done2");
                        if(n % 3 == 0) iv.incr();
                        else if(n % 3 == 1) iv2.incr();
                        else if(n % 3 == 2) { iv.incr(); iv2.incr(); }
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
        GuardCheck.assertClean();

        Guard g3 = new Guard();
        IntVar iv3 = new IntVar();
        Runnable w3=()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                final Runnable r = ()->{
                    if(n == nmax-1) System.out.println("Done3");
                    if(n % 3 == 0) {
                        iv.incr();
                        iv2.incr();
                    } else if(n % 3 == 1) {
                        iv2.incr();
                        iv3.incr();
                    } else if(n % 3 == 2) {
                        iv.incr();
                        iv2.incr();
                        iv3.incr();
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

        Runnable w4=()->{
            for(int i=0;i<nmax;i++) {
                final int n = i;
                final Runnable r = ()->{
                    if(n == nmax-1) System.out.println("Done4");
                    iv2.incr();
                };
                if(n % 3 == 0)
                    Guard.runGuarded(r,g,g2);
                else if(n % 3 == 1)
                    Guard.runGuarded(r,g2,g3);
                else if(n % 3 == 2)
                    ;//Guard.runGuarded(r,g,g3);
                    ;//Guard.runTree(r,g,g2,g3);
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
    }
}
