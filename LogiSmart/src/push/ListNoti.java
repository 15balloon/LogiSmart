package push;

import java.util.Date;
import java.util.HashMap;

public class ListNoti {
	private static HashMap<String, Date> list = new HashMap<String, Date>();
	
	public static void setList(String device, Date time) {
		list.put(device, time);
	}
	
	public static HashMap<String, Date> getList() {
		return list;
	}
	
	public static boolean searchList(String device, Date time) {
		System.out.println(list);
		
		if (list.containsKey(device)) {
			Date prevTime = list.get(device);
			long diff = time.getTime() - prevTime.getTime();
			if (diff/1000 >= 180) {
				setList(device, time);
				System.out.println("After 3min");
				return true;
			}
		}
		else {
			System.out.println("list add");
			setList(device, time);
			return true;
		}
		System.out.println("Before 3min");
		return false;
	}
}
