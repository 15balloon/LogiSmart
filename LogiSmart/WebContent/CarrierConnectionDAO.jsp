<%@ page import="java.sql.*" %>
<%@ page import="org.json.simple.JSONObject" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>

<%
	String jdbcDriver = "jdbc:mysql://logismart.cafe24.com/logismart";
	String dbUser = "logismart";
	String dbPass = "Logi2017253012";
	
	Connection conn = null;
	PreparedStatement pstmt = null;
	
	try {
		request.setCharacterEncoding("UTF-8");
		String id = request.getParameter("strings1");
		String connectionState = request.getParameter("strings2");
		int state = 0;
		
		if (connectionState.equals("connect")) {
			state = 1;
		}
		
		JSONObject jObject = new JSONObject();
		
		String driver = "com.mysql.jdbc.Driver";
		Class.forName(driver);
		conn = DriverManager.getConnection(jdbcDriver, dbUser, dbPass);
		
		System.out.println("id: " + id);
		
		String insert_state = "UPDATE bluetooth SET b_conn = ? WHERE b_carrier = ?;";
		
		pstmt = conn.prepareStatement(insert_state);
		pstmt.setInt(1, state);
		pstmt.setInt(2, Integer.parseInt(id));
		
		int update = pstmt.executeUpdate();
		
		if (update > 0) {
			System.out.println("Update Complete");
			jObject.put("result", "success");
		}
		else {
			System.out.println("Update Fail");
			jObject.put("result", "fail");
		}
		System.out.println(jObject.toJSONString());
		out.println(jObject.toJSONString());
		
	} catch (SQLException se) {
		se.printStackTrace();
	} finally {
		try {
			pstmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
%>