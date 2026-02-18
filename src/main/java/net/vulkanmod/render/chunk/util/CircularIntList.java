package net.vulkanmod.render.chunk.util;

import org.apache.commons.lang3.Validate;

import java.util.Iterator;

public class CircularIntList {
    private int[] list;
    private final int size;

    public CircularIntList(int size) {
        this.size = size;
        this.list = new int[size];
    }

    public CircularIntList(int size, int startIndex) {
        this(size);
        this.updateStartIdx(startIndex);
    }

    public void updateStartIdx(int startIndex) {
        int k = 0;
        for (int i = startIndex; i < size; ++i) {
            list[k++] = i;
        }
        for (int i = 0; i < startIndex; ++i) {
            list[k++] = i;
        }
    }

    public int getNext(int i) {
        return (i + 1 < size) ? list[i + 1] : -1;
    }

    public int getPrevious(int i) {
        return (i - 1 >= 0) ? list[i - 1] : -1;
    }

    public OwnIterator iterator() {
        return new OwnIterator();
    }

    public RangeIterator createRangeIterator() {
        return new RangeIterator();
    }

    public RangeIterator rangeIterator(int startIndex, int endIndex) {
        RangeIterator it = new RangeIterator();
        it.update(startIndex, endIndex);
        return it;
    }

    public class OwnIterator implements Iterator<Integer> {
        private int currentIndex = -1;
        private final int maxIndex = list.length - 1;

        @Override
        public boolean hasNext() {
            return currentIndex < maxIndex;
        }

        @Override
        public Integer next() {
            currentIndex++;
            return list[currentIndex];
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public void restart() {
            this.currentIndex = -1;
        }
    }

    public class RangeIterator implements Iterator<Integer> {
        private int currentIndex;
        private int startIndex;
        private int maxIndex;

        public RangeIterator() {
            this.startIndex = 0;
            this.maxIndex = 0;
            this.currentIndex = -1;
        }

        public void update(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.maxIndex = endIndex;
            Validate.isTrue(this.maxIndex < list.length, "Beyond max size");
            this.restart();
        }

        @Override
        public boolean hasNext() {
            return currentIndex < maxIndex;
        }

        @Override
        public Integer next() {
            currentIndex++;
            return list[currentIndex];
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public void restart() {
            this.currentIndex = this.startIndex - 1;
        }
    }
}
