import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.lang.ThreadLocal;
import java.lang.*;

public class temperature 
{
	// Modify in order to change the number of sensors.
	public static final int SENSORS = 8;

	// Modify to change the time the program runs for.
	public static final int HOURS = 24;

	public static final int MINUTES = 60;
	public static final int TOPHIGHORLOW = 5;
	public static final int LARGEDIFFINTERVAL = 10;

	public static AtomicInteger count = new AtomicInteger(SENSORS);

	public static class sensorThread implements Runnable
	{
		int threadId;

		public sensorThread(int threadId)
		{
			this.threadId = threadId;
		}

		@Override
		public void run()
		{
			//while (!go.get()) {}
			while (true)
			{
				int[] store = new int[MINUTES];
				for (int i = 0; i < MINUTES; i++)
				{
					// generate random int between -100 and 70
					Random rand = new Random();
					int randInt = rand.nextInt(171) - 100;
					// store in array
					store[i] = randInt;
					// if i > largediffinterval then do stuff
					if (i >= LARGEDIFFINTERVAL)
					{
						diff.add(Math.abs(store[i]-store[i-LARGEDIFFINTERVAL]));
					}
					// add to outside priorityblockingqueues as necessary
					max.add(randInt);
					min.add(randInt);
				}

				// decrement count and wait
				synchronized (sensorArr[threadId]) {
					try {
						counter.getAndIncrement();
						(sensorArr[threadId]).wait();
					}
					catch (Exception ex) {}
				}
			}
		}
	}

	public static PriorityBlockingQueue<Integer> max;
	public static PriorityBlockingQueue<Integer> min;
	public static PriorityBlockingQueue<Integer> diff;

	public static AtomicInteger counter;
	//public static AtomicBoolean go; 
	public static Thread[] sensorArr;

	public static void main (String[] args)
	{
		counter = new AtomicInteger(0);

		// at the end, poll all and last value will be desired one
		max = new PriorityBlockingQueue<>((MINUTES*SENSORS), Collections.reverseOrder());
		min = new PriorityBlockingQueue<>();
		diff = new PriorityBlockingQueue<>((MINUTES*SENSORS), Collections.reverseOrder());

		sensorArr = new Thread[SENSORS];

		for (int i = 0; i < SENSORS; i++)
		{
			sensorArr[i] = new Thread(new sensorThread(i));
			(sensorArr[i]).start();
		}

		AtomicInteger added = new AtomicInteger(0);

		for (int i = 0; i < HOURS; i++)
		{
			while (counter.get() != SENSORS) {}

			int minp = Integer.MIN_VALUE, maxp = Integer.MAX_VALUE, diffp = Integer.MIN_VALUE;
			System.out.println("Hour " + (i+1));

			// do stuff
			System.out.println("Top max temps:");
			int printed = 0;
			while (!max.isEmpty()) {
				int lastmax = maxp;
				maxp = max.poll();
				if (lastmax != maxp && printed < TOPHIGHORLOW) 
				{
					System.out.println(maxp);
					printed++;
				}
			}
			printed = 0;
			System.out.println("Top min temps:");
			while (!min.isEmpty()) {
				int lastmin = minp;
				minp = min.poll();
				if (lastmin != minp && printed < TOPHIGHORLOW) 
				{
					System.out.println(minp);
					printed++;
				}
			}
			printed = 0;
			while (!diff.isEmpty()) {
				if (printed < 1) 
				{
					System.out.println("Maximum 10 minute difference: " + diff.poll());
					printed++;
				}
				diff.poll();
			}

			counter.getAndSet(0);

			// notify all
			for (int j = 0; j < SENSORS; j++)
			{
				synchronized (sensorArr[j]) {
					try {
						(sensorArr[j]).notify();
					}
					catch (Exception ex) {}
				}
			}
		}

		System.out.println("Execution finished. ");
		System.exit(1);
	}
}