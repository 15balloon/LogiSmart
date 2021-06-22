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
			
			String admName = null;
			int connect = 0;
			int driverId = -1;
			String driverName = null;
			String driverPhone = null;
			
			String search_ble = "SELECT * FROM bluetooth WHERE b_thing = ?;";
			pstmt2 = conn.prepareStatement(search_ble);
			pstmt2.setInt(1, result1.getInt("bbs_num"));
			
			result2 = pstmt2.executeQuery();
			if (result2.next()) {
				result2.getString("b_num");
				
				if (!result2.wasNull()) {
					admName = result2.getString("b_name");
					connect = result2.getInt("b_conn");
				}
				else {
					admName = "null";
					connect = 0;
				}
			}
			else {
				admName = "null";
				connect = 0;
			}
			
			int bbs_carrierID = result1.getInt("bbs_carrierID");
			
			if (bbs_carrierID != 0) {
				String search_carrier = "SELECT * FROM carriers WHERE c_id = ?;";
				pstmt3 = conn.prepareStatement(search_carrier);
				pstmt3.setInt(1, bbs_carrierID);
				
				result3 = pstmt3.executeQuery();
				result3.next();
				
				driverId = result3.getInt("c_id");
				driverName = result3.getString("c_name");
				driverPhone = result3.getString("c_phone");
			}
			else {
				driverId = -1;
				driverName = "null";
				driverPhone = "null";
			}
			
			JSONObject jObject = new JSONObject();
			jObject.put("name", admName);
			jObject.put("from", result1.getString("bbs_start"));
			jObject.put("to", result1.getString("bbs_arrival"));
			jObject.put("connect", connect);
			jObject.put("driverId", driverId);
			jObject.put("driverName", driverName);
			jObject.put("driverPhone", driverPhone);
			jObject.put("shipName", result1.getString("bbs_name"));
			jObject.put("upper", result1.getInt("bbs_upper"));
			jObject.put("lower", result1.getInt("bbs_lower"));
			jArray.add(jObject);
			
			System.out.println(jObject);
			
			pstmt2.clearParameters();
			
			if (bbs_carrierID != 0)
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