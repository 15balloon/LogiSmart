<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.JSONObject" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
    
<%
	String jdbcDriver = "jdbc:mysql://logismart.cafe24.com/logismart?characterEncoding=UTF-8&serverTimezone=UTC";
	String dbUser = "logismart";
	String dbPass = "Logi2017253012";
	
	Connection conn = null;
	PreparedStatement pstmt1 = null;
	PreparedStatement pstmt2 = null;
	PreparedStatement pstmt3 = null;
	ResultSet result1 = null;
	ResultSet result2 = null;
	ResultSet result3 = null;
	
	try {
		request.setCharacterEncoding("UTF-8");
		String id = request.getParameter("strings1");
		
		JSONObject jObject = new JSONObject();
		
		String driver = "com.mysql.jdbc.Driver";
		Class.forName(driver);
		conn = DriverManager.getConnection(jdbcDriver, dbUser, dbPass);
		
		String get_gps = "SELECT * FROM locate WHERE l_id = ? ORDER BY l_time DESC LIMIT 1;";
		String get_thermo = "SELECT * FROM temper WHERE t_id = ? ORDER BY t_time DESC LIMIT 1;";
		
		pstmt1 = conn.prepareStatement(get_gps);
		pstmt1.setInt(1, Integer.parseInt(id));
		
		result1 = pstmt1.executeQuery();
		
		pstmt2 = conn.prepareStatement(get_thermo);
		pstmt2.setInt(1, Integer.parseInt(id));
		
		result2 = pstmt2.executeQuery();
		
		String get_conn = "SELECT * FROM bluetooth WHERE b_carrier = ?";
		
		pstmt3 = conn.prepareStatement(get_conn);
		pstmt3.setInt(1, Integer.parseInt(id));
		
		result3 = pstmt3.executeQuery();
		
		if (result1.next() && result2.next() && result3.next()) {
			System.out.println("Search Complete");
			jObject.put("result", "success");
		 	jObject.put("lat", result1.getString("l_wido"));
			jObject.put("lon", result1.getString("l_gyeongdo").trim());
			jObject.put("thermo", result2.getInt("t_data"));
			jObject.put("conn", result3.getInt("b_conn"));
		}
		else {
			System.out.println("Search Fail");
			jObject.put("result", "fail");
		}
		System.out.println(jObject.toJSONString());
		out.println(jObject.toJSONString());
		
	} catch (SQLException se) {
		se.printStackTrace();
	} finally {
		try {
			result3.close();
			result2.close();
			result1.close();
			pstmt3.close();
			pstmt2.close();
			pstmt2.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
%>