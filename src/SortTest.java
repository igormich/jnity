import java.util.ArrayList;
import java.util.Collections;

public class SortTest<E> extends ArrayList<E>{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3821410480800576366L;

	private static final int N = 100000;

	private int getCount = 0;
	private int setCount = 0;
	
	@Override
	public E get(int index) {
		getCount++;
		return super.get(index);
	}

	@Override
	public E set(int index, E element) {
		setCount++;
		return super.set(index, element);
	}

	public static void main(String[] args) {
		SortTest<Integer> baseList = new SortTest<>();
		for(int i=N;i>0;i--)
			baseList.add(i);
		Collections.shuffle(baseList);
		
		SortTest<Integer> list = new SortTest<>();
		for(int i=0;i<baseList.size();i++)
			list.add(baseList.get(i));
		list.getCount = 0;
		list.setCount = 0;
		poorBubbleSort(list);
		System.out.println("poorBubbleSort");
		System.out.println(list.getCount);
		System.out.println(list.setCount);
		
		list = new SortTest<>();
		for(int i=0;i<baseList.size();i++)
			list.add(baseList.get(i));
		list.getCount = 0;
		list.setCount = 0;
		smartBubbleSort(list);
		System.out.println("smartBubbleSort");
		System.out.println(list.getCount);
		System.out.println(list.setCount);
		
		list = new SortTest<>();
		for(int i=0;i<baseList.size();i++)
			list.add(baseList.get(i));
		list.getCount = 0;
		list.setCount = 0;
		smart2BubbleSort(list);
		System.out.println("smart2BubbleSort");
		System.out.println(list.getCount);
		System.out.println(list.setCount);
		System.out.println(list);
		System.out.println();
		System.out.println(N);
		System.out.println(N * Math.log(N)/Math.log(2));
		System.out.println(N * N);
	}
	private static void poorBubbleSort(SortTest<Integer> sortTest) {
		int start = 0;
		int end = sortTest.size()-1;
		boolean hasBeenChanged;
		do {
			hasBeenChanged = false;
			for(int i=start;i<end;i++) {
				int a = sortTest.get(i);
				int b = sortTest.get(i+1);
				if(a > b) {
					sortTest.set(i, b);
					sortTest.set(i + 1, a);
					hasBeenChanged = true;
				}
			}
		} while (hasBeenChanged);
	}
	private static void betterBubbleSort(SortTest<Integer> sortTest) {
		int start = 0;
		int end = sortTest.size()-1;
		boolean hasBeenChanged;
		do {
			hasBeenChanged = false;
			for(int i=start;i<end;i++) {
				int a = sortTest.get(i);
				int b = sortTest.get(i+1);
				if(a > b) {
					sortTest.set(i, b);
					sortTest.set(i + 1, a);
					hasBeenChanged = true;
				}
			}
			end--;
		} while (hasBeenChanged);
	}
	private static void smartBubbleSort(SortTest<Integer> sortTest) {
		int start = 0;
		int end = sortTest.size()-1;
		boolean hasBeenChanged;
		do {
			hasBeenChanged = false;
			for(int i=start;i<end;i++) {
				int a = sortTest.get(i);
				int b = sortTest.get(i+1);
				if(a > b) {
					sortTest.set(i, b);
					sortTest.set(i + 1, a);
					hasBeenChanged = true;
				}
			}
			end--;
			for(int i=end;i>start;i--) {
				int a = sortTest.get(i);
				int b = sortTest.get(i-1);
				if(a < b) {
					sortTest.set(i, b);
					sortTest.set(i - 1, a);
					hasBeenChanged = true;
				}
			}
			start++;
		} while (hasBeenChanged);
	}
	private static void smart2BubbleSort(SortTest<Integer> sortTest) {
		int step = sortTest.size() / 10 + 1;
		int start = 0;
		int end = sortTest.size()-1;
		boolean hasBeenChanged;
		do {
			hasBeenChanged = false;
			for(int i=start;i<end-step;i++) {
				int a = sortTest.get(i);
				int b = sortTest.get(i+step);
				if(a > b) {
					sortTest.set(i, b);
					sortTest.set(i + step, a);
					hasBeenChanged = true;
				}
			}
			end--;
			for(int i=end;i>start+step;i--) {
				int a = sortTest.get(i);
				int b = sortTest.get(i-step);
				if(a < b) {
					sortTest.set(i, b);
					sortTest.set(i - step, a);
					hasBeenChanged = true;
				}
			}
			start++;
		} while (hasBeenChanged);
		smartBubbleSort(sortTest);
	}
}
