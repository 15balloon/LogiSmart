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
		
		System.out.println("id: " + id);
		
		String search_thing = "SELECT b_name, b_thing FROM bluetooth WHERE b_carrier = ?;";
		
		pstmt1 = conn.prepareStatement(search_thing);
		pstmt1.setInt(1, Integer.parseInt(id));
		
		result1 = pstmt1.executeQuery();
		
		result1.next();
		
		String ble_name = result1.getString(1);
		int thing = result1.getInt(2);
		
		String search_accept = "SELECT * FROM managebbs WHERE bbs_num = ?;";
		
		pstmt2 = conn.prepareStatement(search_accept);
		pstmt2.setInt(1, thing);
		
		result2 = pstmt2.executeQuery();
		
		if (result2.next()) {
			String managerName = result2.getString("bbs_manager"); // name

			String search_manager = "SELECT * FROM manager WHERE m_Name = ?;";
			
			pstmt3 = conn.prepareStatement(search_manager);
			pstmt3.setString(1, managerName);
			
			result3 = pstmt3.executeQuery();
			
			result3.next();
			
			System.out.println("Search Complete");
			jObject.put("result", "success");
			jObject.put("ble", ble_name);
			jObject.put("manager", result3.getString("m_Name"));
			jObject.put("phone", result3.getString("m_Phone"));
			jObject.put("ship", result2.getString("bbs_name"));
			jObject.put("from", result2.getString("bbs_start"));
			jObject.put("to", result2.getString("bbs_arrival"));
			jObject.put("upper", result2.getString("bbs_upper"));
			jObject.put("lower", result2.getString("bbs_lower"));
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
			pstmt1.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
%>