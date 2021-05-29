<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.JSONObject" %>
<%@ page import="org.json.simple.JSONArray" %>
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
		String name = request.getParameter("strings1");
		
		JSONObject jMain = new JSONObject();
		JSONArray jArray = new JSONArray();
		boolean check = false;
		
		String driver = "com.mysql.jdbc.Driver";
		Class.forName(driver);
		conn = DriverManager.getConnection(jdbcDriver, dbUser, dbPass);
		
		System.out.println("name: " + name);
		
		String search_bbs = "SELECT * FROM managebbs WHERE bbs_manager = ?;";
		
		pstmt1 = conn.prepareStatement(search_bbs);
		pstmt1.setString(1, name);
		
		result1 = pstmt1.executeQuery();
		
		while (result1.next()) {
			check = true;
			
			String search_ble = "SELECT * FROM bluetooth WHERE b_thing = ?;";
			pstmt2 = conn.prepareStatement(search_ble);
			pstmt2.setInt(1, result1.getInt("bbs_num"));
			
			result2 = pstmt2.executeQuery();
			result2.next();
			
			String search_carrier = "SELECT * FROM carriers WHERE c_id = ?;";
			pstmt3 = conn.prepareStatement(search_carrier);
			pstmt3.setInt(1, result2.getInt("bbs_carrierID"));
			
			result3 = pstmt3.executeQuery();
			result3.next();
			
			JSONObject jObject = new JSONObject();
			jObject.put("name", result2.getString("b_name"));
			jObject.put("from", result1.getInt("bbs_start"));
			jObject.put("to", result1.getInt("bbs_arrival"));
			jObject.put("connect", result2.getInt("b_conn"));
			jObject.put("driverName", result3.getString("c_name"));
			jObject.put("driverPhone", result3.getString("c_phone"));
			jObject.put("shipName", result1.getString("bbs_name"));
			jObject.put("upper", result1.getString("bbs_upper"));
			jObject.put("lower", result1.getString("bbs_lower"));
			jArray.add(jObject);
			
			pstmt2.clearParameters();
			pstmt3.clearParameters();
		}
		
		if (check) {
			System.out.println("Search Success");
			jMain.put("result", "success");
			jMain.put("data", jArray);
		}
		else {
			System.out.println("Search Fail");
			jMain.put("result", "fail");
		}
		System.out.println(jMain.toJSONString());
		out.println(jMain.toJSONString());
		
	} catch (SQLException se) {
		se.printStackTrace();
	} finally {
		try {
			result3.close();
			result2.close();
			result1.close();
			pstmt3.close();
			pstmt2.close();
			pstmt1.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
%>