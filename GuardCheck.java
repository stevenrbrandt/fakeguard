import java.util.Map;
import java.util.HashMap;

public class GuardCheck {
    static class Check {
        final int threadId;
        volatile int count;
        Check(int t,int c) {
            threadId = t;
            count = c;
        }
        public String toString() {
            return "Count(tid="+threadId+",cnt="+count+")";
        }
    }
    public synchronized static void assertClean() {
        System.out.println("Active count: "+GuardTask.activeCount.get());
        assert guards.size() == 0 : "Not clean: "+guards.toString();
        assert GuardTask.activeCount.get() == 0;
    }
    static Map<Integer,Check> guards = new HashMap<>();
    public static synchronized void checkLock(Guard g) {
        Check check = guards.get(g.id);
        if(check == null) {
            check = new Check(ThreadID.get(),1);
            guards.put(g.id, check);
            //System.out.printf("Lock of %s by thread %d: %s%n",g,ThreadID.get(),guards);
        } else {
            assert check.threadId == ThreadID.get() : String.format("threadID mismatch: %d != %d",check.threadId,ThreadID.get());
            check.count += 1;
        }
    }
    public static synchronized void checkUnlock(Guard g) {
        Check check = guards.get(g.id);
        assert check != null : String.format("Failed unlocking %s",g);
        check.count -= 1;
        if(check.count == 0) {
            //System.out.printf("Unlock of %s by thread %d: %s%n",g,ThreadID.get(),guards);
            guards.remove(g.id);
        }
    }
}
