package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.framework.DbStarter;
import org.example.framework.MessageResponse;
import org.example.framework.Response;
import org.example.framework.RoleCharger;
import spark.QueryParamsMap;

import java.sql.*;
import java.util.*;

import static spark.Spark.halt;
import static spark.Spark.post;

public class IndexController {
    public static void initIndexController(){
//        post("/login", (req, res) -> {
//            String role = req.queryParams("role");
//            String username = req.queryParams("username");
//            String password = req.queryParams("password");
//            Connection conn = DbStarter.getConnection();
//            String sql = "SELECT * FROM " + role + " WHERE username = ? AND password = ?";
//
//            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//                pstmt.setString(1, username);
//                pstmt.setString(2, password);
//                ResultSet rs = pstmt.executeQuery();
//                if (rs.next()) {
//                    RoleCharger.setRole(role);
//                    RoleCharger.setUsername(username);
//                    RoleCharger.setRealname(rs.getString("real_name"));
//                    return DbStarter.getObjectMapper().writeValueAsString(new Response(true));
//                } else {
//                    return DbStarter.getObjectMapper().writeValueAsString(new Response(false));
//                }
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//        });
//
//        post("/getMessage", (req, res) -> {
//            Connection conn = DbStarter.getConnection();
//            int type = Integer.parseInt(req.queryParams("messageType"));
//            String username = RoleCharger.getUsername(); // 获取用户名
//            String sql = "select m.content,m.sender,m.recipient,m.send_date from message m where m.type = ? and (m.recipient = ? or m.sender = ?)";
//            if(type==1){ //当类型是博客评论时
//                sql = "select m.content,m.sender,m.recipient,m.send_date,b.blog_title from message m inner join blog b on b.blog_id = m.blog_id" +
//                        " where m.type = ? and (m.recipient = ? or m.sender = ?)";
//            }
//
//            List<Map<String, Object>> results = new ArrayList<>();
//            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//                pstmt.setInt(1, type);
//                pstmt.setString(2, username);
//                pstmt.setString(3, username);
//                ResultSet rs = pstmt.executeQuery();
//                // 获取结果集的元数据
//                ResultSetMetaData metaData = rs.getMetaData();
//                int columnCount = metaData.getColumnCount();
//
//                while (rs.next()) {
//                    Map<String, Object> row = new HashMap<>();
//                    for (int i = 1; i <= columnCount; i++) {
//                        String columnName = metaData.getColumnName(i);
//                        Object columnValue = rs.getObject(i);
//                        row.put(columnName, columnValue);
//                    }
//                    results.add(row);
//                }
//                rs.close();
//            } catch (SQLException e) {
//                halt(500, "Database error!");
//                return null;
//            }
//            // 构造执行的SQL语句字符串
//            String executedSQL = String.format("select m.content,m.sender,m.recipient,m.send_date from message m where m.type = %d and (m.recipient = '%s' or m.sender = '%s')", type, username, username);
//            if (type == 1) {
//                executedSQL = String.format("select m.content,m.sender,m.recipient,m.send_date,b.blog_title from message m inner join blog b on b.blog_id = m.blog_id where m.type = %d and (m.recipient = '%s' or m.sender = '%s')", type, username, username);
//            }
//            MessageResponse response = new MessageResponse(results, executedSQL,RoleCharger.getUsername());
//            res.type("application/json");
//            return DbStarter.getObjectMapper().writeValueAsString(response);
//        });

        post("/deleteMessageTry", (req, res) -> {
            return RoleCharger.isAdmin();
        });
    }
}