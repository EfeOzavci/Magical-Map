import java.util.Comparator;

public class PriorityQueue<E> {
    private Object[] heap;                    // Internal array representing the binary heap
    private int size;                         // Current number of elements in the priority queue
    private Comparator<? super E> comparator; // Comparator to determine the order of elements

    private static final int INITIAL_CAPACITY = 11; // Default initial capacity

    public PriorityQueue(Comparator<? super E> comparator) {
        this.comparator = comparator;         // Store the comparator for element ordering
        this.heap = new Object[INITIAL_CAPACITY]; // Initialize the heap array
        this.size = 0;                        // Initially empty
    }

    public boolean add(E e) {
        ensureCapacity(size + 1);             // Ensure internal array has room for the new element
        heap[size] = e;                       // Place the new element at the end of the heap
        siftUp(size);                         // Restore heap order by sifting up if necessary
        size++;                               // Increment size after successful addition
        return true;                          // Always returns true since insertion succeeds
    }

    @SuppressWarnings("unchecked")
    public E poll() {
        if (isEmpty()) {
            return null;                      // If empty, return null since there's no element to poll
        }
        E result = (E) heap[0];               // The top of the heap (index 0) is the highest priority element
        size--;                               // Decrement the size since we're removing the top element
        heap[0] = heap[size];                 // Move the last element in the heap to the top
        heap[size] = null;                    // Clear out the moved element's old position
        siftDown(0);                          // Restore heap order by sifting down from the root
        return result;                        // Return the removed element
    }

    public boolean isEmpty() {
        return size == 0;                     // True if no elements are present
    }

    // Internal helper methods

    private void ensureCapacity(int capacity) {
        // If required capacity exceeds current array length, resize
        if (capacity > heap.length) {
            int newCap = heap.length * 2;     // Double the capacity
            if (newCap < capacity) {
                newCap = capacity;            // If doubling isn't enough, use the requested capacity
            }
            Object[] newHeap = new Object[newCap]; // Create a new array with the expanded capacity
            System.arraycopy(heap, 0, newHeap, 0, size); // Copy existing elements
            heap = newHeap;                   // Update the reference to the new, larger heap
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(E a, E b) {
        // Uses the comparator to determine the order between two elements
        return comparator.compare(a, b);
    }

    @SuppressWarnings("unchecked")
    private void siftUp(int idx) {
        int parent = (idx - 1) >>> 1;         // Parent index in a binary heap: (index-1)/2 using bit shift
        Object x = heap[idx];                 // The element that may need to be moved up
        // While not at the root and the element is smaller (higher priority) than its parent
        while (idx > 0 && compare((E)x, (E)heap[parent]) < 0) {
            heap[idx] = heap[parent];         // Move the parent down
            idx = parent;                     // Update index to parent's position
            parent = (parent - 1) >>> 1;      // Compute new parent's index
        }
        heap[idx] = x;                        // Place the element in its correct position
    }

    @SuppressWarnings("unchecked")
    private void siftDown(int idx) {
        int half = size >>> 1;                // Halfway point, beyond which there are no children
        Object x = heap[idx];                 // The root element that needs to be moved down if necessary
        while (idx < half) {
            int left = (idx << 1) + 1;        // Left child index: 2*idx + 1
            int right = left + 1;             // Right child index: left + 1
            int smallest = left;              // Assume left child is smaller (higher priority)
            // If right child exists and is smaller than the left child, choose right child
            if (right < size && compare((E)heap[right], (E)heap[left]) < 0) {
                smallest = right;
            }
            // If the parent element x is already smaller or equal than the smallest child, stop
            if (compare((E)heap[smallest], (E)x) >= 0) {
                break;
            }
            // Otherwise, swap the parent with the smaller child to restore heap order
            heap[idx] = heap[smallest];
            idx = smallest;                   // Move down to the child's position and continue
        }
        heap[idx] = x;                        // Place the element in its final position
    }
}