/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.ds;

/**
 *
 * @author ani
 */

public class Queue<T> {
    private final SinglyLinkedList<T> list;

    public Queue() {
        list = new SinglyLinkedList<>();
    }

    public boolean isEmpty() { return list.isEmpty(); }
    public int size() { return list.size(); }

    public void enqueue(T value) {
        list.addLast(value);
    }

    public T dequeue() {
        return list.removeFirst();
    }

    public T peek() {
        return list.peekFirst();
    }

    public Object[] toArray() {
        return list.toArray();
    }
}
