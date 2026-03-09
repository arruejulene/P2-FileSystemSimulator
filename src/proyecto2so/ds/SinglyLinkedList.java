/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.ds;

/**
 *
 * @author ani
 */

public class SinglyLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public SinglyLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public T peekFirst() {
        return head == null ? null : head.value;
    }

    public void addLast(T value) {
        Node<T> n = new Node<>(value);
        if (tail == null) {
            head = n;
            tail = n;
        } else {
            tail.next = n;
            tail = n;
        }
        size++;
    }

    public void addFirst(T value) {
        Node<T> n = new Node<>(value);
        if (head == null) {
            head = n;
            tail = n;
        } else {
            n.next = head;
            head = n;
        }
        size++;
    }

    public T removeFirst() {
        if (head == null) return null;

        T value = head.value;
        head = head.next;
        size--;

        if (head == null) tail = null;
        return value;
    }

    
    public boolean remove(T value) {
        if (head == null) return false;

        if (equalsValue(head.value, value)) {
            removeFirst();
            return true;
        }

        Node<T> prev = head;
        Node<T> cur = head.next;

        while (cur != null) {
            if (equalsValue(cur.value, value)) {
                prev.next = cur.next;
                if (cur == tail) tail = prev;
                size--;
                return true;
            }
            prev = cur;
            cur = cur.next;
        }

        return false;
    }

    public void forEach(Visitor<T> visitor) {
        Node<T> cur = head;
        while (cur != null) {
            visitor.visit(cur.value);
            cur = cur.next;
        }
    }

    public Object[] toArray() {
        Object[] result = new Object[size];
        int index = 0;
        Node<T> cur = head;

        while (cur != null) {
            result[index] = cur.value;
            index++;
            cur = cur.next;
        }

        return result;
    }

    public interface Visitor<T> {
        void visit(T value);
    }

    private boolean equalsValue(T a, T b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
