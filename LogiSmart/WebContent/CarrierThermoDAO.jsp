<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.JSONObject" %>
<%@ page import="push.*" %>  
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>

<%
	String jdbcDriver = "jdbc:mysql://logismart.cafe24.com/logismart?characterEncoding=UTF-8&serverTimezone=UTC";
	String dbUser = "logismart";
	String dbPass = "Logi2017253012";
	
	Connection conn = null;
	PreparedStatement pstmt = null;
	PreparedStatement pstmt1 = null;
	PreparedStatement pstmt2 = null;
	PreparedStatement pstmt3 = null;
	PreparedStatement pstmt4 = null;
	ResultSet result1 = null;
	ResultSet result2 = null;
	ResultSet result3 = null;
	ResultSet result4 = null;
	
	try {
		request.setCharacterEncoding("UTF-8");
		String id = request.getParameter("strings1");
		int thermo = Integer.parseInt(request.getParameter("strings2"));
		
		JSONObject jObject = new JSONObject();
		
		String driver = "com.mysql.jdbc.Driver";
		Class.forName(driver);
		conn = DriverManager.getConnection(jdbcDriver, dbUser, dbPass);
		
		System.out.println("id: " + id);
		
		String insert_thermo = "INSERT INTO temper(t_id, t_data, t_time) VALUES(?, ?, NOW())";
		
		pstmt = conn.prepareStatement(insert_thermo);
		pstmt.setInt(1, Integer.parseInt(id));
		pstmt.setInt(2, thermo);
		
		int insert = pstmt.executeUpdate();
		
		String get_thermo = "SELECT * FROM temper WHERE t_id = ? ORDER BY t_time DESC LIMIT 1;";
		
		pstmt1 = conn.prepareStatement(get_thermo);
		pstmt1.setInt(1, Integer.parseInt(id));
		
		result1 = pstmt1.executeQuery();
		
		result1.next();
		Date time = result1.getDate("t_time");
		
		String search_thermo = "SELECT * FROM managebbs WHERE bbs_carrierID = ?;";
		
		pstmt2 = conn.prepareStatement(search_thermo);
		pstmt2.setInt(1, Integer.parseInt(id));
		
		result2 = pstmt2.executeQuery();
		
		result2.next();
		
		String search_bt = "SELECT * FROM bluetooth WHERE b_carrier = ?;";
		
		pstmt3 = conn.prepareStatement(search_bt);
		pstmt3.setInt(1, Integer.parseInt(id));
		
		result3 = pstmt3.executeQuery();
		
		result3.next();
		
		String b_name = result3.getString("b_name");
		
		String search_token = "SELECT * FROM manager WHERE m_Name = ?;";
		
		pstmt4 = conn.prepareStatement(search_token);
		String m_name = result2.getString("bbs_manager");
		pstmt4.setString(1, m_name);
		
		result4 = pstmt4.executeQuery();
		
		result4.next();
		
		String m_Token = result4.getString("m_Token");
		
		if (insert > 0) {
			System.out.println("Insert Complete");
			jObject.put("result", "success");
			
			if (!m_Token.isEmpty()) {
				CheckNoti noti = new CheckNoti();
				int check = noti.checkSafe(thermo, result2);
				
				System.out.println("check : " + check);
				
				if (check != 0) {
					
					if (ListNoti.searchList(b_name, time)) {
						
						SendNoti send = new SendNoti();
						
						switch (check) {
						case 1:
							send.sendMsg("경고", b_name + " 물품의 온도가 한계 온도에 근접했습니다.", m_Token);
							break;
						case 2:
							send.sendMsg("위험", b_name + " 물품의 온도가 한계 온도를 넘었습니다.", m_Token);
							break;
						}
					}
				}
			}
		}
		else {
			System.out.println("Insert Fail");
			jObject.put("result", "fail");
		}
		System.out.println(jObject.toJSONString());
		out.println(jObject.toJSONString());
		
	} catch (SQLException se) {
		se.printStackTrace();
	} finally {
		try {
			result4.close();
			result3.close();
			result2.close();
			result1.close();
			pstmt4.close();
			pstmt3.close();
			pstmt2.close();
			pstmt1.close();
			pstmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
%>