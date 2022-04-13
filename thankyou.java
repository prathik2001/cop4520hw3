import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.lang.ThreadLocal;
import java.lang.*;

public class thankyou 
{
	// Modify in order to change the number of guests.
	public static final int SERVANTS = 4;
	public static final int PRESENTS = 5000000;

	public static class servantThread implements Runnable
	{
		int threadId;

		public servantThread(int threadId)
		{
			this.threadId = threadId;
		}

		@Override
		public void run()
		{
			while (true)
			{	
				// pick either add, remove, or contains randomly (1 to 3)
				Random random = new Random();
				int rand = random.nextInt(3) + 1; 

				// add: rand #, check if in hashset and presentCounter, if not call add
				if (rand == 1 && presentCounter.get() < PRESENTS)
				{
					int r2 = random.nextInt(PRESENTS) + 1;
					if (!hashset.containsKey(r2))
					{
						hashset.put(r2, 1);
						removeset.put(r2, 1);
						//System.out.println(threadId + "Putting " + r2);

						if (list.add(r2)) presentCounter.getAndIncrement();
						//System.out.println(presentCounter);
					}
				}

				// remove: rand #, call remove until true returned
				else if (rand == 2)
				{
					Set<Integer> check = removeset.keySet();
					int size = check.size();
					if (size <= 0) {}
					else
						{
						int r2 = random.nextInt(size);
						//r2 = Integer.parseInt(checkArr[r2].toString());
						int curr = 0, iter = 0;
						for (Integer i : check)
						{
							curr = i;
							iter++;
							if (iter > r2) break;
						}
						r2 = curr;
						//System.out.println(threadId +  "Removing " + r2);
						if (!removeset.containsKey(r2) || !list.remove(r2))
						{
							//System.out.println(threadId +  "Failed.");
							//r2 = random.nextInt(PRESENTS) + 1;
						}
						else { 
							removeset.remove(r2);
							noteCounter.getAndIncrement();
							//System.out.println(noteCounter);
						}
					}
				}

				// contains: call contains
				else if (rand == 3)
				{
					int r3 = random.nextInt(PRESENTS) + 1;
					//System.out.println(threadId + "Containsing " + r3);
					list.contains(r3);
				}
			}
		}
	}

	public static class LockFreeList
	{
		public Present head;

		public LockFreeList()
		{
			Present headPres = new Present(Integer.MIN_VALUE);
			headPres.next = new AtomicMarkableReference(new Present(Integer.MAX_VALUE), false);
			head = headPres;
		}

		class Window {
			public Present pred, curr;
			public Window(Present myPred, Present myCurr)
			{
				pred = myPred; curr = myCurr;
			}
		}

		public Window find(Present head, int key)
		{
			Present pred = null, curr = null, succ = null;
			boolean[] marked = {false};
			boolean snip;
			retry: while (true)
			{
				pred = head;
				if (pred.next != null) curr = pred.next.getReference();
				else return new Window(pred, curr);
				while (true) {
					if (curr != null && curr.next != null) succ = curr.next.get(marked);
					else return new Window(pred, curr);
					while (marked[0]) {
						snip = pred.next.compareAndSet(curr, succ, false, false);
						if (!snip) continue retry;
						curr = succ;
						if (curr != null && curr.next != null) succ = curr.next.get(marked);
						else return new Window(pred, curr);
					}
					if (curr != null && curr.tagNum >= key)
						return new Window(pred, curr);
					pred = curr;
					curr = succ;
				}
			}
		}

		public boolean add(int tagNum)
		{
			int key = tagNum;
			while (true) {
				Window window = find(head, key);
				Present pred = window.pred, curr = window.curr;
				if (curr != null && curr.tagNum == key) {
					return false;
				} else {
					Present node = new Present(key);
					node.next = new AtomicMarkableReference(curr, false);
					if (pred.next.compareAndSet(curr, node, false, false))
						return true;
				}
			}
		}

		public boolean remove(int tagNum)
		{
			int key = tagNum;
			boolean snip;
			while(true) {
				Window window = find(head, key);
				Present pred = window.pred, curr = window.curr;
				if (curr != null && curr.tagNum != key)
					return false;
				else {
					if (curr == null) return false;
					Present succ;
					if (curr.next != null) succ = curr.next.getReference();
					else succ = null;
					snip = curr.next.compareAndSet(succ, succ, false, true);
					if (!snip) continue;
					pred.next.compareAndSet(curr, succ, false, false);
					return true;
				}
			}
		}

		public boolean contains(int tagNum)
		{
			boolean[] marked = {false};
			int key = tagNum;
			Present curr = head;
			while (curr != null && curr.tagNum < key) {
				if (curr.next != null) curr = curr.next.getReference();
				else 
				{
					curr = null;
					break;
				}
				Present succ;
				if (curr.next != null) succ = curr.next.get(marked);
				else break;
			}
			return (curr != null && curr.tagNum == key && !marked[0]);
		}
	}

	public static class Present
	{
		int tagNum;
		AtomicMarkableReference<Present> next;

		public Present(int tag)
		{
			tagNum = tag;
			next = null;
		}
	}

	public static AtomicInteger presentCounter;
	public static AtomicInteger noteCounter;
	public static ConcurrentHashMap hashset;
	public static ConcurrentHashMap removeset;
	public static LockFreeList list;
	public static Thread[] servantsArr;

	// Initialize head with Integer.MIN_VALUE
	public static void main (String[] args)
	{
		presentCounter = new AtomicInteger(0);
		noteCounter = new AtomicInteger(0);
		hashset = new ConcurrentHashMap();
		removeset = new ConcurrentHashMap();
		list = new LockFreeList();

		servantsArr = new Thread[SERVANTS];

		for (int i = 0; i < SERVANTS; i++)
		{
			servantsArr[i] = new Thread(new servantThread(i));
		}

		AtomicInteger counter = new AtomicInteger(SERVANTS);

		for (int i = 0; i < SERVANTS; i++)
		{
			(servantsArr[i]).start();
		}
		while (noteCounter.get() < PRESENTS)
		{
			//System.out.println(presentCounter);
		}

		System.out.println("Execution finished.");
		System.exit(1);
	}
}