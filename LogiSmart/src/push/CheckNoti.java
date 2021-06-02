package push;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CheckNoti {
	
	public int checkSafe(ResultSet result1, ResultSet result2) throws SQLException {
				
		int check = 0; // 0:fine, 1:caution, 2:danger
		int nthermo = 0;
		int upper = result2.getInt("bbs_upper");
		int lower = result2.getInt("bbs_lower");
		int upperc = upper - upper/10;
		int lowerc = lower + lower/10;
		
		while (result1.next()) {
			nthermo = result1.getInt("t_data");
			if (nthermo >= upper || nthermo <= lower) {
				check = 2;
				break;
			}
			else if (nthermo >= upperc || nthermo <= lowerc) {
				check = 1;
				break;
			}
			else {
				check = 0;
			}
		}
		
		return check;
	}
}