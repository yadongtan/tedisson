package com.tedisson.lock;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

public class TedissonAbstractQueueSynchronizer extends AbstractOwnableSynchronizer{

        /**
         * Creates a new {@code AbstractQueuedSynchronizer} instance
         * with initial synchronization state of zero.
         */
        protected TedissonAbstractQueueSynchronizer() { }

        /**
         * Wait queue node class.
         *
         * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
         * Hagersten) lock queue. CLH locks are normally used for
         * spinlocks.  We instead use them for blocking synchronizers, but
         * use the same basic tactic of holding some of the control
         * information about a thread in the predecessor of its node.  A
         * "status" field in each node keeps track of whether a thread
         * should block.  A node is signalled when its predecessor
         * releases.  Each node of the queue otherwise serves as a
         * specific-notification-style monitor holding a single waiting
         * thread. The status field does NOT control whether threads are
         * granted locks etc though.  A thread may try to acquire if it is
         * first in the queue. But being first does not guarantee success;
         * it only gives the right to contend.  So the currently released
         * contender thread may need to rewait.
         *
         * <p>To enqueue into a CLH lock, you atomically splice it in as new
         * tail. To dequeue, you just set the head field.
         * <pre>
         *      +------+  prev +-----+       +-----+
         * head |      | <---- |     | <---- |     |  tail
         *      +------+       +-----+       +-----+
         * </pre>
         *
         * <p>Insertion into a CLH queue requires only a single atomic
         * operation on "tail", so there is a simple atomic point of
         * demarcation from unqueued to queued. Similarly, dequeuing
         * involves only updating the "head". However, it takes a bit
         * more work for nodes to determine who their successors are,
         * in part to deal with possible cancellation due to timeouts
         * and interrupts.
         *
         * <p>The "prev" links (not used in original CLH locks), are mainly
         * needed to handle cancellation. If a node is cancelled, its
         * successor is (normally) relinked to a non-cancelled
         * predecessor. For explanation of similar mechanics in the case
         * of spin locks, see the papers by Scott and Scherer at
         * http://www.cs.rochester.edu/u/scott/synchronization/
         *
         * <p>We also use "next" links to implement blocking mechanics.
         * The thread id for each node is kept in its own node, so a
         * predecessor signals the next node to wake up by traversing
         * next link to determine which thread it is.  Determination of
         * successor must avoid races with newly queued nodes to set
         * the "next" fields of their predecessors.  This is solved
         * when necessary by checking backwards from the atomically
         * updated "tail" when a node's successor appears to be null.
         * (Or, said differently, the next-links are an optimization
         * so that we don't usually need a backward scan.)
         *
         * <p>Cancellation introduces some conservatism to the basic
         * algorithms.  Since we must poll for cancellation of other
         * nodes, we can miss noticing whether a cancelled node is
         * ahead or behind us. This is dealt with by always unparking
         * successors upon cancellation, allowing them to stabilize on
         * a new predecessor, unless we can identify an uncancelled
         * predecessor who will carry this responsibility.
         *
         * <p>CLH queues need a dummy header node to get started. But
         * we don't create them on construction, because it would be wasted
         * effort if there is never contention. Instead, the node
         * is constructed and head and tail pointers are set upon first
         * contention.
         *
         * <p>Threads waiting on Conditions use the same nodes, but
         * use an additional link. Conditions only need to link nodes
         * in simple (non-concurrent) linked queues because they are
         * only accessed when exclusively held.  Upon await, a node is
         * inserted into a condition queue.  Upon signal, the node is
         * transferred to the main queue.  A special value of status
         * field is used to mark which queue a node is on.
         *
         * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
         * Scherer and Michael Scott, along with members of JSR-166
         * expert group, for helpful ideas, discussions, and critiques
         * on the design of this class.
         */
        static final class Node {
            /** Marker to indicate a node is waiting in shared mode */
            static final Node SHARED = new Node();
            /** Marker to indicate a node is waiting in exclusive mode */
            static final Node EXCLUSIVE = null;

            /** waitStatus value to indicate thread has cancelled */
            static final int CANCELLED =  1;
            /** waitStatus value to indicate successor's thread needs unparking */
            static final int SIGNAL    = -1;
            /** waitStatus value to indicate thread is waiting on condition */
            static final int CONDITION = -2;
            /**
             * waitStatus value to indicate the next acquireShared should
             * unconditionally propagate
             */
            static final int PROPAGATE = -3;

            volatile int waitStatus;


            volatile Node prev;

            volatile Node next;

            volatile Thread thread;

            Node nextWaiter;

            final boolean isShared() {
                return nextWaiter == SHARED;
            }

            final Node predecessor() throws NullPointerException {
                Node p = prev;
                if (p == null)
                    throw new NullPointerException();
                else
                    return p;
            }

            Node() {    // Used to establish initial head or SHARED marker
            }

            Node(Thread thread, Node mode) {     // Used by addWaiter
                this.nextWaiter = mode;
                this.thread = thread;
            }

            Node(Thread thread, int waitStatus) { // Used by Condition
                this.waitStatus = waitStatus;
                this.thread = thread;
            }
        }

        private transient volatile Node head;

        private transient volatile Node tail;

        private volatile int state;

        protected final int getState() {
            return state;
        }

        protected final void setState(int newState) {
            state = newState;
        }

        protected final boolean compareAndSetState(int expect, int update) {
            // See below for intrinsics setup to support this
            return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
        }

        // Queuing utilities
        static final long spinForTimeoutThreshold = 1000L;

        private Node enq(final Node node) {
            for (;;) {
                Node t = tail;
                if (t == null) { // Must initialize
                    if (compareAndSetHead(new Node()))
                        tail = head;
                } else {
                    node.prev = t;
                    if (compareAndSetTail(t, node)) {
                        t.next = node;
                        return t;
                    }
                }
            }
        }

        private Node addWaiter(Node mode) {
            Node node = new Node(Thread.currentThread(), mode);
            // Try the fast path of enq; backup to full enq on failure
            Node pred = tail;
            if (pred != null) {
                node.prev = pred;
                if (compareAndSetTail(pred, node)) {
                    pred.next = node;
                    return node;
                }
            }
            enq(node);
            return node;
        }

        private void setHead(Node node) {
            head = node;
            node.thread = null;
            node.prev = null;
        }

        // �����߳�����
        public void unparkSuccessor(){
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
        }

        public void unparkSuccessor(Node node) {
            /*
             * If status is negative (i.e., possibly needing signal) try
             * to clear in anticipation of signalling.  It is OK if this
             * fails or if status is changed by waiting thread.
             */
            int ws = node.waitStatus;
            if (ws < 0)
                compareAndSetWaitStatus(node, ws, 0);

            /*
             * Thread to unpark is held in successor, which is normally
             * just the next node.  But if cancelled or apparently null,
             * traverse backwards from tail to find the actual
             * non-cancelled successor.
             */
            Node s = node.next;
            if (s == null || s.waitStatus > 0) {
                s = null;
                for (Node t = tail; t != null && t != node; t = t.prev)
                    if (t.waitStatus <= 0)
                        s = t;
            }
            if (s != null)
                LockSupport.unpark(s.thread);
        }

        private void doReleaseShared() {
            /*
             * Ensure that a release propagates, even if there are other
             * in-progress acquires/releases.  This proceeds in the usual
             * way of trying to unparkSuccessor of head if it needs
             * signal. But if it does not, status is set to PROPAGATE to
             * ensure that upon release, propagation continues.
             * Additionally, we must loop in case a new node is added
             * while we are doing this. Also, unlike other uses of
             * unparkSuccessor, we need to know if CAS to reset status
             * fails, if so rechecking.
             */
            for (;;) {
                Node h = head;
                if (h != null && h != tail) {
                    int ws = h.waitStatus;
                    if (ws == Node.SIGNAL) {
                        if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                            continue;            // loop to recheck cases
                        unparkSuccessor(h);
                    }
                    else if (ws == 0 &&
                            !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                        continue;                // loop on failed CAS
                }
                if (h == head)                   // loop if head changed
                    break;
            }
        }

        private void setHeadAndPropagate(Node node, int propagate) {
            Node h = head; // Record old head for check below
            setHead(node);
            /*
             * Try to signal next queued node if:
             *   Propagation was indicated by caller,
             *     or was recorded (as h.waitStatus either before
             *     or after setHead) by a previous operation
             *     (note: this uses sign-check of waitStatus because
             *      PROPAGATE status may transition to SIGNAL.)
             * and
             *   The next node is waiting in shared mode,
             *     or we don't know, because it appears null
             *
             * The conservatism in both of these checks may cause
             * unnecessary wake-ups, but only when there are multiple
             * racing acquires/releases, so most need signals now or soon
             * anyway.
             */
            if (propagate > 0 || h == null || h.waitStatus < 0 ||
                    (h = head) == null || h.waitStatus < 0) {
                Node s = node.next;
                if (s == null || s.isShared())
                    doReleaseShared();
            }
        }

        private void cancelAcquire(Node node) {
            // Ignore if node doesn't exist
            if (node == null)
                return;

            node.thread = null;

            // Skip cancelled predecessors
            Node pred = node.prev;
            while (pred.waitStatus > 0)
                node.prev = pred = pred.prev;

            // predNext is the apparent node to unsplice. CASes below will
            // fail if not, in which case, we lost race vs another cancel
            // or signal, so no further action is necessary.
            Node predNext = pred.next;

            // Can use unconditional write instead of CAS here.
            // After this atomic step, other Nodes can skip past us.
            // Before, we are free of interference from other threads.
            node.waitStatus = Node.CANCELLED;

            // If we are the tail, remove ourselves.
            if (node == tail && compareAndSetTail(node, pred)) {
                compareAndSetNext(pred, predNext, null);
            } else {
                // If successor needs signal, try to set pred's next-link
                // so it will get one. Otherwise wake it up to propagate.
                int ws;
                if (pred != head &&
                        ((ws = pred.waitStatus) == Node.SIGNAL ||
                                (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                        pred.thread != null) {
                    Node next = node.next;
                    if (next != null && next.waitStatus <= 0)
                        compareAndSetNext(pred, predNext, next);
                } else {
                    unparkSuccessor(node);
                }

                node.next = node; // help GC
            }
        }

        private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
            int ws = pred.waitStatus;
            if (ws == Node.SIGNAL)
                /*
                 * This node has already set status asking a release
                 * to signal it, so it can safely park.
                 */
                return true;
            if (ws > 0) {
                /*
                 * Predecessor was cancelled. Skip over predecessors and
                 * indicate retry.
                 */
                do {
                    node.prev = pred = pred.prev;
                } while (pred.waitStatus > 0);
                pred.next = node;
            } else {
                /*
                 * waitStatus must be 0 or PROPAGATE.  Indicate that we
                 * need a signal, but don't park yet.  Caller will need to
                 * retry to make sure it cannot acquire before parking.
                 */
                compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
            }
            return false;
        }

        /**
         * Convenience method to interrupt current thread.
         */
        static void selfInterrupt() {
            Thread.currentThread().interrupt();
        }

        private final boolean parkAndCheckInterrupt() {
            LockSupport.park(this);
            return Thread.interrupted();
        }


        final boolean acquireQueued(final Node node, int arg) {
            boolean failed = true;
            try {
                boolean interrupted = false;
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null; // help GC
                        failed = false;
                        return interrupted;
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt())
                        interrupted = true;
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        private void doAcquireInterruptibly(int arg)
                throws InterruptedException {
            final Node node = addWaiter(Node.EXCLUSIVE);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        /**
         * Acquires in exclusive timed mode.
         *
         * @param arg the acquire argument
         * @param nanosTimeout max wait time
         * @return {@code true} if acquired
         */
        private boolean doAcquireNanos(int arg, long nanosTimeout)
                throws InterruptedException {
            if (nanosTimeout <= 0L)
                return false;
            final long deadline = System.nanoTime() + nanosTimeout;
            final Node node = addWaiter(Node.EXCLUSIVE);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                    nanosTimeout = deadline - System.nanoTime();
                    if (nanosTimeout <= 0L)
                        return false;
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            nanosTimeout > spinForTimeoutThreshold)
                        LockSupport.parkNanos(this, nanosTimeout);
                    if (Thread.interrupted())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        /**
         * Acquires in shared uninterruptible mode.
         * @param arg the acquire argument
         */
        private void doAcquireShared(int arg) {
            final Node node = addWaiter(Node.SHARED);
            boolean failed = true;
            try {
                boolean interrupted = false;
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head) {
                        int r = tryAcquireShared(arg);
                        if (r >= 0) {
                            setHeadAndPropagate(node, r);
                            p.next = null; // help GC
                            if (interrupted)
                                selfInterrupt();
                            failed = false;
                            return;
                        }
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt())
                        interrupted = true;
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        /**
         * Acquires in shared interruptible mode.
         * @param arg the acquire argument
         */
        private void doAcquireSharedInterruptibly(int arg)
                throws InterruptedException {
            final Node node = addWaiter(Node.SHARED);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head) {
                        int r = tryAcquireShared(arg);
                        if (r >= 0) {
                            setHeadAndPropagate(node, r);
                            p.next = null; // help GC
                            failed = false;
                            return;
                        }
                    }
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        /**
         * Acquires in shared timed mode.
         *
         * @param arg the acquire argument
         * @param nanosTimeout max wait time
         * @return {@code true} if acquired
         */
        private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
                throws InterruptedException {
            if (nanosTimeout <= 0L)
                return false;
            final long deadline = System.nanoTime() + nanosTimeout;
            final Node node = addWaiter(Node.SHARED);
            boolean failed = true;
            try {
                for (;;) {
                    final Node p = node.predecessor();
                    if (p == head) {
                        int r = tryAcquireShared(arg);
                        if (r >= 0) {
                            setHeadAndPropagate(node, r);
                            p.next = null; // help GC
                            failed = false;
                            return true;
                        }
                    }
                    nanosTimeout = deadline - System.nanoTime();
                    if (nanosTimeout <= 0L)
                        return false;
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            nanosTimeout > spinForTimeoutThreshold)
                        LockSupport.parkNanos(this, nanosTimeout);
                    if (Thread.interrupted())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        // Main exported methods
        protected boolean tryAcquire(int arg) {
            throw new UnsupportedOperationException();
        }


        protected boolean tryRelease(int arg) {
            throw new UnsupportedOperationException();
        }


        protected int tryAcquireShared(int arg) {
            throw new UnsupportedOperationException();
        }

         protected boolean tryReleaseShared(int arg) {
            throw new UnsupportedOperationException();
        }


        protected boolean isHeldExclusively() {
            throw new UnsupportedOperationException();
        }


        public final void acquire(int arg) {
            if (!tryAcquire(arg) &&
                    acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
                selfInterrupt();
        }

        public final void acquireInterruptibly(int arg)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            if (!tryAcquire(arg))
                doAcquireInterruptibly(arg);
        }

        public final boolean tryAcquireNanos(int arg, long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            return tryAcquire(arg) ||
                    doAcquireNanos(arg, nanosTimeout);
        }

        public final boolean release(int arg) {
            if (tryRelease(arg)) {
                Node h = head;
                if (h != null && h.waitStatus != 0)
                    unparkSuccessor(h);
                return true;
            }
            return false;
        }

        public final void acquireShared(int arg) {
            if (tryAcquireShared(arg) < 0)
                doAcquireShared(arg);
        }

        public final void acquireSharedInterruptibly(int arg)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            if (tryAcquireShared(arg) < 0)
                doAcquireSharedInterruptibly(arg);
        }

        public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            return tryAcquireShared(arg) >= 0 ||
                    doAcquireSharedNanos(arg, nanosTimeout);
        }

        public final boolean releaseShared(int arg) {
            if (tryReleaseShared(arg)) {
                doReleaseShared();
                return true;
            }
            return false;
        }

        // Queue inspection methods

        public final boolean hasQueuedThreads() {
            return head != tail;
        }

        public final boolean hasContended() {
            return head != null;
        }

        public final Thread getFirstQueuedThread() {
            // handle only fast path, else relay
            return (head == tail) ? null : fullGetFirstQueuedThread();
        }

        /**
         * Version of getFirstQueuedThread called when fastpath fails
         */
        private Thread fullGetFirstQueuedThread() {
            /*
             * The first node is normally head.next. Try to get its
             * thread field, ensuring consistent reads: If thread
             * field is nulled out or s.prev is no longer head, then
             * some other thread(s) concurrently performed setHead in
             * between some of our reads. We try this twice before
             * resorting to traversal.
             */
            Node h, s;
            Thread st;
            if (((h = head) != null && (s = h.next) != null &&
                    s.prev == head && (st = s.thread) != null) ||
                    ((h = head) != null && (s = h.next) != null &&
                            s.prev == head && (st = s.thread) != null))
                return st;

            /*
             * Head's next field might not have been set yet, or may have
             * been unset after setHead. So we must check to see if tail
             * is actually first node. If not, we continue on, safely
             * traversing from tail back to head to find first,
             * guaranteeing termination.
             */

            Node t = tail;
            Thread firstThread = null;
            while (t != null && t != head) {
                Thread tt = t.thread;
                if (tt != null)
                    firstThread = tt;
                t = t.prev;
            }
            return firstThread;
        }

        /**
         * Returns true if the given thread is currently queued.
         *
         * <p>This implementation traverses the queue to determine
         * presence of the given thread.
         *
         * @param thread the thread
         * @return {@code true} if the given thread is on the queue
         * @throws NullPointerException if the thread is null
         */
        public final boolean isQueued(Thread thread) {
            if (thread == null)
                throw new NullPointerException();
            for (Node p = tail; p != null; p = p.prev)
                if (p.thread == thread)
                    return true;
            return false;
        }

        /**
         * Returns {@code true} if the apparent first queued thread, if one
         * exists, is waiting in exclusive mode.  If this method returns
         * {@code true}, and the current thread is attempting to acquire in
         * shared mode (that is, this method is invoked from {@link
         * #tryAcquireShared}) then it is guaranteed that the current thread
         * is not the first queued thread.  Used only as a heuristic in
         * ReentrantReadWriteLock.
         */
        final boolean apparentlyFirstQueuedIsExclusive() {
            Node h, s;
            return (h = head) != null &&
                    (s = h.next)  != null &&
                    !s.isShared()         &&
                    s.thread != null;
        }


        public final boolean hasQueuedPredecessors() {
            // The correctness of this depends on head being initialized
            // before tail and on head.next being accurate if the current
            // thread is first in queue.
            Node t = tail; // Read fields in reverse initialization order
            Node h = head;
            Node s;
            return h != t &&
                    ((s = h.next) == null || s.thread != Thread.currentThread());
        }


        public final int getQueueLength() {
            int n = 0;
            for (Node p = tail; p != null; p = p.prev) {
                if (p.thread != null)
                    ++n;
            }
            return n;
        }


        public final Collection<Thread> getQueuedThreads() {
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node p = tail; p != null; p = p.prev) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
            return list;
        }


        public final Collection<Thread> getExclusiveQueuedThreads() {
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node p = tail; p != null; p = p.prev) {
                if (!p.isShared()) {
                    Thread t = p.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }

        /**
         * Returns a collection containing threads that may be waiting to
         * acquire in shared mode. This has the same properties
         * as {@link #getQueuedThreads} except that it only returns
         * those threads waiting due to a shared acquire.
         *
         * @return the collection of threads
         */
        public final Collection<Thread> getSharedQueuedThreads() {
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node p = tail; p != null; p = p.prev) {
                if (p.isShared()) {
                    Thread t = p.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }

        /**
         * Returns a string identifying this synchronizer, as well as its state.
         * The state, in brackets, includes the String {@code "State ="}
         * followed by the current value of {@link #getState}, and either
         * {@code "nonempty"} or {@code "empty"} depending on whether the
         * queue is empty.
         *
         * @return a string identifying this synchronizer, as well as its state
         */
        public String toString() {
            int s = getState();
            String q  = hasQueuedThreads() ? "non" : "";
            return super.toString() +
                    "[State = " + s + ", " + q + "empty queue]";
        }


        // Internal support methods for Conditions

        /**
         * Returns true if a node, always one that was initially placed on
         * a condition queue, is now waiting to reacquire on sync queue.
         * @param node the node
         * @return true if is reacquiring
         */
        final boolean isOnSyncQueue(Node node) {
            if (node.waitStatus == Node.CONDITION || node.prev == null)
                return false;
            if (node.next != null) // If has successor, it must be on queue
                return true;
            /*
             * node.prev can be non-null, but not yet on queue because
             * the CAS to place it on queue can fail. So we have to
             * traverse from tail to make sure it actually made it.  It
             * will always be near the tail in calls to this method, and
             * unless the CAS failed (which is unlikely), it will be
             * there, so we hardly ever traverse much.
             */
            return findNodeFromTail(node);
        }

        /**
         * Returns true if node is on sync queue by searching backwards from tail.
         * Called only when needed by isOnSyncQueue.
         * @return true if present
         */
        private boolean findNodeFromTail(Node node) {
            Node t = tail;
            for (;;) {
                if (t == node)
                    return true;
                if (t == null)
                    return false;
                t = t.prev;
            }
        }

        /**
         * Transfers a node from a condition queue onto sync queue.
         * Returns true if successful.
         * @param node the node
         * @return true if successfully transferred (else the node was
         * cancelled before signal)
         */
        final boolean transferForSignal(Node node) {
            /*
             * If cannot change waitStatus, the node has been cancelled.
             */
            if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
                return false;

            /*
             * Splice onto queue and try to set waitStatus of predecessor to
             * indicate that thread is (probably) waiting. If cancelled or
             * attempt to set waitStatus fails, wake up to resync (in which
             * case the waitStatus can be transiently and harmlessly wrong).
             */
            Node p = enq(node);
            int ws = p.waitStatus;
            if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
                LockSupport.unpark(node.thread);
            return true;
        }

        /**
         * Transfers node, if necessary, to sync queue after a cancelled wait.
         * Returns true if thread was cancelled before being signalled.
         *
         * @param node the node
         * @return true if cancelled before the node was signalled
         */
        final boolean transferAfterCancelledWait(Node node) {
            if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
                enq(node);
                return true;
            }
            /*
             * If we lost out to a signal(), then we can't proceed
             * until it finishes its enq().  Cancelling during an
             * incomplete transfer is both rare and transient, so just
             * spin.
             */
            while (!isOnSyncQueue(node))
                Thread.yield();
            return false;
        }

        /**
         * Invokes release with current state value; returns saved state.
         * Cancels node and throws exception on failure.
         * @param node the condition node for this wait
         * @return previous sync state
         */
        final int fullyRelease(Node node) {
            boolean failed = true;
            try {
                int savedState = getState();
                if (release(savedState)) {
                    failed = false;
                    return savedState;
                } else {
                    throw new IllegalMonitorStateException();
                }
            } finally {
                if (failed)
                    node.waitStatus = Node.CANCELLED;
            }
        }

        // Instrumentation methods for conditions

        /**
         * Queries whether the given ConditionObject
         * uses this synchronizer as its lock.
         *
         * @param condition the condition
         * @return {@code true} if owned
         * @throws NullPointerException if the condition is null
         */
        public final boolean owns(ConditionObject condition) {
            return condition.isOwnedBy(this);
        }

        /**
         * Queries whether any threads are waiting on the given condition
         * associated with this synchronizer. Note that because timeouts
         * and interrupts may occur at any time, a {@code true} return
         * does not guarantee that a future {@code signal} will awaken
         * any threads.  This method is designed primarily for use in
         * monitoring of the system state.
         *
         * @param condition the condition
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if exclusive synchronization
         *         is not held
         * @throws IllegalArgumentException if the given condition is
         *         not associated with this synchronizer
         * @throws NullPointerException if the condition is null
         */
        public final boolean hasWaiters(ConditionObject condition) {
            if (!owns(condition))
                throw new IllegalArgumentException("Not owner");
            return condition.hasWaiters();
        }

        /**
         * Returns an estimate of the number of threads waiting on the
         * given condition associated with this synchronizer. Note that
         * because timeouts and interrupts may occur at any time, the
         * estimate serves only as an upper bound on the actual number of
         * waiters.  This method is designed for use in monitoring of the
         * system state, not for synchronization control.
         *
         * @param condition the condition
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if exclusive synchronization
         *         is not held
         * @throws IllegalArgumentException if the given condition is
         *         not associated with this synchronizer
         * @throws NullPointerException if the condition is null
         */
        public final int getWaitQueueLength(ConditionObject condition) {
            if (!owns(condition))
                throw new IllegalArgumentException("Not owner");
            return condition.getWaitQueueLength();
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on the given condition associated with this
         * synchronizer.  Because the actual set of threads may change
         * dynamically while constructing this result, the returned
         * collection is only a best-effort estimate. The elements of the
         * returned collection are in no particular order.
         *
         * @param condition the condition
         * @return the collection of threads
         * @throws IllegalMonitorStateException if exclusive synchronization
         *         is not held
         * @throws IllegalArgumentException if the given condition is
         *         not associated with this synchronizer
         * @throws NullPointerException if the condition is null
         */
        public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
            if (!owns(condition))
                throw new IllegalArgumentException("Not owner");
            return condition.getWaitingThreads();
        }

        /**
         * Condition implementation for a {@link
         * java.util.concurrent.locks.AbstractQueuedSynchronizer} serving as the basis of a {@link
         * Lock} implementation.
         *
         * <p>Method documentation for this class describes mechanics,
         * not behavioral specifications from the point of view of Lock
         * and Condition users. Exported versions of this class will in
         * general need to be accompanied by documentation describing
         * condition semantics that rely on those of the associated
         * {@code AbstractQueuedSynchronizer}.
         *
         * <p>This class is Serializable, but all fields are transient,
         * so deserialized conditions have no waiters.
         */
        public class ConditionObject implements Condition, java.io.Serializable {
            private static final long serialVersionUID = 1173984872572414699L;
            /** First node of condition queue. */
            private transient Node firstWaiter;
            /** Last node of condition queue. */
            private transient Node lastWaiter;

            /**
             * Creates a new {@code ConditionObject} instance.
             */
            public ConditionObject() { }

            // Internal methods

            /**
             * Adds a new waiter to wait queue.
             * @return its new wait node
             */
            private Node addConditionWaiter() {
                Node t = lastWaiter;
                // If lastWaiter is cancelled, clean out.
                if (t != null && t.waitStatus != Node.CONDITION) {
                    unlinkCancelledWaiters();
                    t = lastWaiter;
                }
                Node node = new Node(Thread.currentThread(), Node.CONDITION);
                if (t == null)
                    firstWaiter = node;
                else
                    t.nextWaiter = node;
                lastWaiter = node;
                return node;
            }

            /**
             * Removes and transfers nodes until hit non-cancelled one or
             * null. Split out from signal in part to encourage compilers
             * to inline the case of no waiters.
             * @param first (non-null) the first node on condition queue
             */
            private void doSignal(Node first) {
                do {
                    if ( (firstWaiter = first.nextWaiter) == null)
                        lastWaiter = null;
                    first.nextWaiter = null;
                } while (!transferForSignal(first) &&
                        (first = firstWaiter) != null);
            }

            /**
             * Removes and transfers all nodes.
             * @param first (non-null) the first node on condition queue
             */
            private void doSignalAll(Node first) {
                lastWaiter = firstWaiter = null;
                do {
                    Node next = first.nextWaiter;
                    first.nextWaiter = null;
                    transferForSignal(first);
                    first = next;
                } while (first != null);
            }

            /**
             * Unlinks cancelled waiter nodes from condition queue.
             * Called only while holding lock. This is called when
             * cancellation occurred during condition wait, and upon
             * insertion of a new waiter when lastWaiter is seen to have
             * been cancelled. This method is needed to avoid garbage
             * retention in the absence of signals. So even though it may
             * require a full traversal, it comes into play only when
             * timeouts or cancellations occur in the absence of
             * signals. It traverses all nodes rather than stopping at a
             * particular target to unlink all pointers to garbage nodes
             * without requiring many re-traversals during cancellation
             * storms.
             */
            private void unlinkCancelledWaiters() {
                Node t = firstWaiter;
                Node trail = null;
                while (t != null) {
                    Node next = t.nextWaiter;
                    if (t.waitStatus != Node.CONDITION) {
                        t.nextWaiter = null;
                        if (trail == null)
                            firstWaiter = next;
                        else
                            trail.nextWaiter = next;
                        if (next == null)
                            lastWaiter = trail;
                    }
                    else
                        trail = t;
                    t = next;
                }
            }

            // public methods

            /**
             * Moves the longest-waiting thread, if one exists, from the
             * wait queue for this condition to the wait queue for the
             * owning lock.
             *
             * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
             *         returns {@code false}
             */
            public final void signal() {
                if (!isHeldExclusively())
                    throw new IllegalMonitorStateException();
                Node first = firstWaiter;
                if (first != null)
                    doSignal(first);
            }

            /**
             * Moves all threads from the wait queue for this condition to
             * the wait queue for the owning lock.
             *
             * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
             *         returns {@code false}
             */
            public final void signalAll() {
                if (!isHeldExclusively())
                    throw new IllegalMonitorStateException();
                Node first = firstWaiter;
                if (first != null)
                    doSignalAll(first);
            }

            /**
             * Implements uninterruptible condition wait.
             * <ol>
             * <li> Save lock state returned by {@link #getState}.
             * <li> Invoke {@link #release} with saved state as argument,
             *      throwing IllegalMonitorStateException if it fails.
             * <li> Block until signalled.
             * <li> Reacquire by invoking specialized version of
             *      {@link #acquire} with saved state as argument.
             * </ol>
             */
            public final void awaitUninterruptibly() {
                Node node = addConditionWaiter();
                int savedState = fullyRelease(node);
                boolean interrupted = false;
                while (!isOnSyncQueue(node)) {
                    LockSupport.park(this);
                    if (Thread.interrupted())
                        interrupted = true;
                }
                if (acquireQueued(node, savedState) || interrupted)
                    selfInterrupt();
            }

            /*
             * For interruptible waits, we need to track whether to throw
             * InterruptedException, if interrupted while blocked on
             * condition, versus reinterrupt current thread, if
             * interrupted while blocked waiting to re-acquire.
             */

            /** Mode meaning to reinterrupt on exit from wait */
            private static final int REINTERRUPT =  1;
            /** Mode meaning to throw InterruptedException on exit from wait */
            private static final int THROW_IE    = -1;

            /**
             * Checks for interrupt, returning THROW_IE if interrupted
             * before signalled, REINTERRUPT if after signalled, or
             * 0 if not interrupted.
             */
            private int checkInterruptWhileWaiting(Node node) {
                return Thread.interrupted() ?
                        (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                        0;
            }

            /**
             * Throws InterruptedException, reinterrupts current thread, or
             * does nothing, depending on mode.
             */
            private void reportInterruptAfterWait(int interruptMode)
                    throws InterruptedException {
                if (interruptMode == THROW_IE)
                    throw new InterruptedException();
                else if (interruptMode == REINTERRUPT)
                    selfInterrupt();
            }

            /**
             * Implements interruptible condition wait.
             * <ol>
             * <li> If current thread is interrupted, throw InterruptedException.
             * <li> Save lock state returned by {@link #getState}.
             * <li> Invoke {@link #release} with saved state as argument,
             *      throwing IllegalMonitorStateException if it fails.
             * <li> Block until signalled or interrupted.
             * <li> Reacquire by invoking specialized version of
             *      {@link #acquire} with saved state as argument.
             * <li> If interrupted while blocked in step 4, throw InterruptedException.
             * </ol>
             */
            public final void await() throws InterruptedException {
                if (Thread.interrupted())
                    throw new InterruptedException();
                Node node = addConditionWaiter();
                int savedState = fullyRelease(node);
                int interruptMode = 0;
                while (!isOnSyncQueue(node)) {
                    LockSupport.park(this);
                    if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                        break;
                }
                if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                    interruptMode = REINTERRUPT;
                if (node.nextWaiter != null) // clean up if cancelled
                    unlinkCancelledWaiters();
                if (interruptMode != 0)
                    reportInterruptAfterWait(interruptMode);
            }

            /**
             * Implements timed condition wait.
             * <ol>
             * <li> If current thread is interrupted, throw InterruptedException.
             * <li> Save lock state returned by {@link #getState}.
             * <li> Invoke {@link #release} with saved state as argument,
             *      throwing IllegalMonitorStateException if it fails.
             * <li> Block until signalled, interrupted, or timed out.
             * <li> Reacquire by invoking specialized version of
             *      {@link #acquire} with saved state as argument.
             * <li> If interrupted while blocked in step 4, throw InterruptedException.
             * </ol>
             */
            public final long awaitNanos(long nanosTimeout)
                    throws InterruptedException {
                if (Thread.interrupted())
                    throw new InterruptedException();
                Node node = addConditionWaiter();
                int savedState = fullyRelease(node);
                final long deadline = System.nanoTime() + nanosTimeout;
                int interruptMode = 0;
                while (!isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0L) {
                        transferAfterCancelledWait(node);
                        break;
                    }
                    if (nanosTimeout >= spinForTimeoutThreshold)
                        LockSupport.parkNanos(this, nanosTimeout);
                    if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                        break;
                    nanosTimeout = deadline - System.nanoTime();
                }
                if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                    interruptMode = REINTERRUPT;
                if (node.nextWaiter != null)
                    unlinkCancelledWaiters();
                if (interruptMode != 0)
                    reportInterruptAfterWait(interruptMode);
                return deadline - System.nanoTime();
            }

            /**
             * Implements absolute timed condition wait.
             * <ol>
             * <li> If current thread is interrupted, throw InterruptedException.
             * <li> Save lock state returned by {@link #getState}.
             * <li> Invoke {@link #release} with saved state as argument,
             *      throwing IllegalMonitorStateException if it fails.
             * <li> Block until signalled, interrupted, or timed out.
             * <li> Reacquire by invoking specialized version of
             *      {@link #acquire} with saved state as argument.
             * <li> If interrupted while blocked in step 4, throw InterruptedException.
             * <li> If timed out while blocked in step 4, return false, else true.
             * </ol>
             */
            public final boolean awaitUntil(Date deadline)
                    throws InterruptedException {
                long abstime = deadline.getTime();
                if (Thread.interrupted())
                    throw new InterruptedException();
                Node node = addConditionWaiter();
                int savedState = fullyRelease(node);
                boolean timedout = false;
                int interruptMode = 0;
                while (!isOnSyncQueue(node)) {
                    if (System.currentTimeMillis() > abstime) {
                        timedout = transferAfterCancelledWait(node);
                        break;
                    }
                    LockSupport.parkUntil(this, abstime);
                    if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                        break;
                }
                if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                    interruptMode = REINTERRUPT;
                if (node.nextWaiter != null)
                    unlinkCancelledWaiters();
                if (interruptMode != 0)
                    reportInterruptAfterWait(interruptMode);
                return !timedout;
            }

            /**
             * Implements timed condition wait.
             * <ol>
             * <li> If current thread is interrupted, throw InterruptedException.
             * <li> Save lock state returned by {@link #getState}.
             * <li> Invoke {@link #release} with saved state as argument,
             *      throwing IllegalMonitorStateException if it fails.
             * <li> Block until signalled, interrupted, or timed out.
             * <li> Reacquire by invoking specialized version of
             *      {@link #acquire} with saved state as argument.
             * <li> If interrupted while blocked in step 4, throw InterruptedException.
             * <li> If timed out while blocked in step 4, return false, else true.
             * </ol>
             */
            public final boolean await(long time, TimeUnit unit)
                    throws InterruptedException {
                long nanosTimeout = unit.toNanos(time);
                if (Thread.interrupted())
                    throw new InterruptedException();
                Node node = addConditionWaiter();
                int savedState = fullyRelease(node);
                final long deadline = System.nanoTime() + nanosTimeout;
                boolean timedout = false;
                int interruptMode = 0;
                while (!isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0L) {
                        timedout = transferAfterCancelledWait(node);
                        break;
                    }
                    if (nanosTimeout >= spinForTimeoutThreshold)
                        LockSupport.parkNanos(this, nanosTimeout);
                    if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                        break;
                    nanosTimeout = deadline - System.nanoTime();
                }
                if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                    interruptMode = REINTERRUPT;
                if (node.nextWaiter != null)
                    unlinkCancelledWaiters();
                if (interruptMode != 0)
                    reportInterruptAfterWait(interruptMode);
                return !timedout;
            }

            //  support for instrumentation

            /**
             * Returns true if this condition was created by the given
             * synchronization object.
             *
             * @return {@code true} if owned
             */
            final boolean isOwnedBy(TedissonAbstractQueueSynchronizer sync) {
                return sync == TedissonAbstractQueueSynchronizer.this;
            }

            /**
             * Queries whether any threads are waiting on this condition.
             * Implements {@link java.util.concurrent.locks.AbstractQueuedSynchronizer#hasWaiters(java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject)}.
             *
             * @return {@code true} if there are any waiting threads
             * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
             *         returns {@code false}
             */
            protected final boolean hasWaiters() {
                if (!isHeldExclusively())
                    throw new IllegalMonitorStateException();
                for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                    if (w.waitStatus == Node.CONDITION)
                        return true;
                }
                return false;
            }

            /**
             * Returns an estimate of the number of threads waiting on
             * this condition.
             * Implements {@link java.util.concurrent.locks.AbstractQueuedSynchronizer#getWaitQueueLength(java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject)}.
             *
             * @return the estimated number of waiting threads
             * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
             *         returns {@code false}
             */
            protected final int getWaitQueueLength() {
                if (!isHeldExclusively())
                    throw new IllegalMonitorStateException();
                int n = 0;
                for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                    if (w.waitStatus == Node.CONDITION)
                        ++n;
                }
                return n;
            }

            /**
             * Returns a collection containing those threads that may be
             * waiting on this Condition.
             * Implements {@link java.util.concurrent.locks.AbstractQueuedSynchronizer#getWaitingThreads(java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject)}.
             *
             * @return the collection of threads
             * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
             *         returns {@code false}
             */
            protected final Collection<Thread> getWaitingThreads() {
                if (!isHeldExclusively())
                    throw new IllegalMonitorStateException();
                ArrayList<Thread> list = new ArrayList<Thread>();
                for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                    if (w.waitStatus == Node.CONDITION) {
                        Thread t = w.thread;
                        if (t != null)
                            list.add(t);
                    }
                }
                return list;
            }
        }

        /**
         * Setup to support compareAndSet. We need to natively implement
         * this here: For the sake of permitting future enhancements, we
         * cannot explicitly subclass AtomicInteger, which would be
         * efficient and useful otherwise. So, as the lesser of evils, we
         * natively implement using hotspot intrinsics API. And while we
         * are at it, we do the same for other CASable fields (which could
         * otherwise be done with atomic field updaters).
         */
        private static Unsafe unsafe = null;
        private static final long stateOffset;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long waitStatusOffset;
        private static final long nextOffset;

        static {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                unsafe =(Unsafe) field.get(null);
                // ���������ʹ�� unsafe �������һЩ����
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                stateOffset = unsafe.objectFieldOffset
                        (java.util.concurrent.locks.AbstractQueuedSynchronizer.class.getDeclaredField("state"));
                headOffset = unsafe.objectFieldOffset
                        (java.util.concurrent.locks.AbstractQueuedSynchronizer.class.getDeclaredField("head"));
                tailOffset = unsafe.objectFieldOffset
                        (java.util.concurrent.locks.AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
                waitStatusOffset = unsafe.objectFieldOffset
                        (Node.class.getDeclaredField("waitStatus"));
                nextOffset = unsafe.objectFieldOffset
                        (Node.class.getDeclaredField("next"));

            } catch (Exception ex) { throw new Error(ex); }
        }

        /**
         * CAS head field. Used only by enq.
         */
        private final boolean compareAndSetHead(Node update) {
            return unsafe.compareAndSwapObject(this, headOffset, null, update);
        }

        /**
         * CAS tail field. Used only by enq.
         */
        private final boolean compareAndSetTail(Node expect, Node update) {
            return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
        }

        /**
         * CAS waitStatus field of a node.
         */
        private static final boolean compareAndSetWaitStatus(Node node,
                                                             int expect,
                                                             int update) {
            return unsafe.compareAndSwapInt(node, waitStatusOffset,
                    expect, update);
        }

        /**
         * CAS next field of a node.
         */
        private static final boolean compareAndSetNext(Node node,
                                                       Node expect,
                                                       Node update) {
            return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
        }
}
