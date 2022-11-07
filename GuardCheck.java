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
    public static synchronized void checkLock(Guard g,int id) {
        Check check = guards.get(g.id);
        if(check == null) {
            check = new Check(id,1);
            guards.put(g.id, check);
            //System.out.printf("Lock of %s by thread %d: %s%n",g,ThreadID.get(),guards);
        } else {
            assert false : "No doubles "+g+" "+check;
            assert check.threadId == id : String.format("threadID mismatch for locking guard %s: %d != %d: %s",g,check.threadId,ThreadID.get(),guards);
            check.count += 1;
        }
    }
    public static synchronized void checkUnlock(Guard g,int id) {
        Check check = guards.get(g.id);
        assert check != null : String.format("Failed unlocking %s",g);
        check.count -= 1;
        assert check.threadId == id : String.format("threadID mismatch for unlocking guard %s: %d != %d: %s",g,check.threadId,ThreadID.get(),guards);
        if(check.count == 0) {
            //System.out.printf("Unlock of %s by thread %d: %s%n",g,ThreadID.get(),guards);
            guards.remove(g.id);
        } else {
            assert false : "Didn't clear";
        }
    }
}
