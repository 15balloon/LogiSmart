package push;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CheckNoti {
	
	public int checkSafe(float thermo, ResultSet result2) throws SQLException {
				
		int check = 0; // 0:fine, 1:caution, 2:danger
		int upper = result2.getInt("bbs_upper");
		int lower = result2.getInt("bbs_lower");
		int upperc = upper - upper/10;
		int lowerc = lower + lower/10;
		
		if (thermo >= upper || thermo <= lower) {
			check = 2;
		}
		else if (thermo >= upperc || thermo <= lowerc) {
			check = 1;
		}
		else {
			check = 0;
		}
		
		return check;
	}
}