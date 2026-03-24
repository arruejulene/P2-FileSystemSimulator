package proyecto2so.ds;

public class DynamicArray<T> {
    private Object[] values;
    private int size;

    public DynamicArray() {
        this.values = new Object[10];
        this.size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(T value) {
        ensureCapacity(size + 1);
        values[size] = value;
        size++;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Index fuera de rango: " + index);
        }
        return (T) values[index];
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            values[i] = null;
        }
        size = 0;
    }

    public Object[] toArray() {
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = values[i];
        }
        return result;
    }

    private void ensureCapacity(int required) {
        if (required <= values.length) {
            return;
        }

        int newCapacity = values.length * 2;
        while (newCapacity < required) {
            newCapacity *= 2;
        }

        Object[] newValues = new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newValues[i] = values[i];
        }
        values = newValues;
    }
}
