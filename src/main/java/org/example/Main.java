package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.controller.IndexController;
import org.example.framework.DbStarter;
import org.example.framework.Response;
import org.example.pojo.Attr;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.sql.*;
import java.util.*;
import static spark.Spark.*;

public class Main {

    private static TemplateEngine thymeleafTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCacheable(false);//Thymeleaf不会缓存模板，每次请求时都会重新加载模板。但会增加I/O操作
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    public static void main(String[] args) {
        staticFiles.location("/fepackages"); // 如果文件在 `/src/main/resources/fepackages`
        TemplateEngine templateEngine = thymeleafTemplateEngine();


        get("/index", (req, res) -> {
            Context ctx = new Context();
            return templateEngine.process("index", ctx);
        });

        get("/", (req,res) -> {

            res.redirect("/index");
            return null;
        });

//        get("/businessMode", (req, res) -> {
//            Context ctx = new Context();
//            return templateEngine.process("businessMode", ctx);
//        });

        get("/toMySearch", (req, res) -> {
            Context ctx = new Context();
            return templateEngine.process("mySearch", ctx);
        });

        get("/databaseMode", (req, res) -> {
            Context ctx = new Context();
            return templateEngine.process("databaseMode", ctx);
        });

//        get("/loginIndex", (req, res) -> {
//            Context ctx = new Context();
//            return templateEngine.process("loginIndex", ctx);
//        });

        post("/startDatabase", (req, res) -> {
            try {
                DbStarter.getConnection();
                return "success";
            } catch(Exception e) {
                return "error";
            }
        });

        post("/getAllTable", (req, res) -> {
            Connection conn = DbStarter.getConnection();
            List<String> tableNames = new ArrayList<>();
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
                while (rs.next()) {
                    tableNames.add(rs.getString("name"));
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                halt(500, "Database error!");
                return null;
            }
            res.type("application/json");  // 设置响应的MIME类型
            return DbStarter.getObjectMapper().writeValueAsString(tableNames);
        });

        post("/createTable", (req, res) -> {
            // 解析请求体为Map
            Map<String, Object> payload = DbStarter.getObjectMapper().readValue(req.body(), Map.class);
            String tableName = (String) payload.get("tableName");
            List<Map<String, String>> fields = (List<Map<String, String>>) payload.get("fields");

            StringBuilder createTableQuery = new StringBuilder("CREATE TABLE " + tableName + " (");

            for (Map<String, String> field : fields) {
                String name = field.get("name");
                String type = field.get("type");
                String constraint = field.get("constraint");

                createTableQuery.append(name).append(" ").append(type);
                if (constraint != null && !constraint.trim().isEmpty()) {
                    createTableQuery.append(" ").append(constraint);
                }
                createTableQuery.append(",");
            }

            // 去除最后的逗号
            createTableQuery.setLength(createTableQuery.length() - 1);
            createTableQuery.append(");");

            Connection conn = DbStarter.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableQuery.toString());
                return "success";
            } catch (Exception e) {
                return "创建表失败: " + e.getMessage();
            }
        });

        post("/getTableAttributes", (req, res) -> {
            String tableName = req.queryParams("tableName");
            Connection conn = DbStarter.getConnection();
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
                List<Map<String, Object>> attributes = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> attribute = new HashMap<>();
                    attribute.put("name", rs.getString("name"));
                    attribute.put("type", rs.getString("type"));
                    attribute.put("constraint", rs.getInt("pk") == 1 ? "PRIMARY KEY" : "");  // 简化，只检查是否是主键
                    attributes.add(attribute);
                }

                res.type("application/json");  // 设置响应的MIME类型
                return DbStarter.getObjectMapper().writeValueAsString(attributes);
            } catch (SQLException e) {
                res.status(500);
                return "Error";
            }
        });

        post("/deleteTable", (req, res) -> {
            String tableName = req.queryParams("tableName");
            Connection conn = DbStarter.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP TABLE " + tableName);
                Map<String, Object> response = new HashMap<>();
                res.type("application/json");
                response.put("success", true);
                return DbStarter.getObjectMapper().writeValueAsString(response);

            } catch (SQLException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", e.getMessage());
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }
        });

        get("/viewData/:tableName", (req, res) -> {
            Context ctx = new Context();
            ctx.setVariable("tableName", req.params(":tableName"));
            return thymeleafTemplateEngine().process("viewData", ctx);
        });

        post("/getTableInfo", (req, res) -> {
            String tableName = req.queryParams("tableName");
            String size = req.queryParams("size");
            String page = req.queryParams("page");
            Connection conn = DbStarter.getConnection();
            List<Map<String, Object>> tableData = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT *,ROWID FROM " + tableName + " limit " + size + " offset " + ((Integer.valueOf(page) - 1) * Integer.valueOf(size)));
                ResultSetMetaData metaData = rs.getMetaData();
                // 获取所有字段名
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnName(i));
                }
                // 获取数据
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (String colName : columns) {
                        row.put(colName, rs.getObject(colName));
                    }
                    tableData.add(row);
                }

                // 获取总数据量
                ResultSet resultSet = stmt.executeQuery("SELECT count(*) as count FROM " + tableName);
                resultSet.next();

                Map<String, Object> response = new HashMap<>();
                res.type("application/json");
                response.put("success", true);
                response.put("columns", columns);
                response.put("total",resultSet.getInt("count"));
                response.put("data", tableData);
                return DbStarter.getObjectMapper().writeValueAsString(response);

            } catch (SQLException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", e.getMessage());
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }
        });

        post("/executeSQL", (req, res) -> {
            String sql = req.queryParams("sql");
            Connection conn = DbStarter.getConnection();
            Response response = new Response();
            res.type("application/json");

            // 为安全起见，避免执行查询语句
            if (sql.trim().toUpperCase().startsWith("SELECT")) {

                response.setIsSucccess(false);
                response.setMessage("此处无法查询");
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }

            // 禁止使用Drop语句
            if (sql.trim().toUpperCase().startsWith("DROP")) {
                response.setIsSucccess(false);
                response.setMessage("无法使用DROP语句哦，请通过页面直接删除你想要的数据");
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }
            try (Statement stmt = conn.createStatement()) {
                // 开启事务
                conn.setAutoCommit(false);
                Savepoint savepoint = conn.setSavepoint("savepoint1");

                stmt.executeUpdate(sql);

                response.setIsSucccess(true);
                // 回滚到断点之前状态
                conn.rollback(savepoint);
                return DbStarter.getObjectMapper().writeValueAsString(response);
            } catch (SQLException e) {
                response.setIsSucccess(false);
                response.setMessage(e.getMessage());
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }
        });

        // 真正执行
        post("/doExecuteSQL", (req, res) -> {
            String sql = req.queryParams("sql");
            Connection conn = DbStarter.getConnection();
            Response response = new Response();
            res.type("application/json");

            // 为安全起见，避免执行查询语句
            if (sql.trim().toUpperCase().startsWith("SELECT")) {

                response.setIsSucccess(false);
                response.setMessage("此处无法查询");
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }

            // 禁止使用Drop语句
            if (sql.trim().toUpperCase().startsWith("DROP")) {
                response.setIsSucccess(false);
                response.setMessage("无法使用DROP语句哦，请通过页面直接删除你想要的数据");
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                response.setIsSucccess(true);
                return DbStarter.getObjectMapper().writeValueAsString(response);
            } catch (SQLException e) {
                response.setIsSucccess(false);
                response.setMessage(e.getMessage());
                return DbStarter.getObjectMapper().writeValueAsString(response);
            }
        });

        post("/executeSearchSQL", (request, response) -> {
            String sql = request.queryParams("sql");
            Map<String, Object> result = new HashMap<>();
            if (!sql.toUpperCase().startsWith("SELECT")) {
                result.put("success", false);
                result.put("message", "这里只允许SELECT语句哦");
                response.type("application/json");
                return DbStarter.getObjectMapper().writeValueAsString(result);
            }

            Connection conn = DbStarter.getConnection();


            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                // 获取列名
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(rsmd.getColumnName(i));
                }
                // 获取行数据
                List<List<Object>> data = new ArrayList<>();
                while (rs.next()) {
                    List<Object> rowData = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        rowData.add(rs.getObject(i));
                    }
                    data.add(rowData);
                }
                result.put("success", true);
                result.put("columns", columns);
                result.put("data", data);
            } catch (SQLException e) {
                result.put("success", false);
                result.put("message", "SQL语句不合法哦!" + e.getMessage());
                //result.put("message", e.getMessage());
            }
            response.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        /**
         * 修改数据
         */
        post("/editColumn", (req, resp) -> {
            String table = req.queryParams("table");
            String data = req.queryParams("data");

            JsonNode jsonNode = DbStarter.getObjectMapper().readTree(data);
            // 构建sql语句
            StringBuilder sb = new StringBuilder("update " + table + " set ");
            List<Object> params = new ArrayList<>();
            Iterator<String> stringIterator = jsonNode.fieldNames();
            while (stringIterator.hasNext()) {
                String key = stringIterator.next();
                if ("rowid".equals(key)) {
                    continue;
                }
                params.add(jsonNode.get(key).asText());
                sb.append(key).append("=").append("?").append(",");
            }
            // 删除最后一个,
            sb.deleteCharAt(sb.length() - 1);
            sb.append(" where rowid=?");
            System.out.println(sb);
            Connection conn = DbStarter.getConnection();
            Map<String,Object> result = new HashMap<>();
            try (PreparedStatement pst = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pst.setObject(i+1,params.get(i));
                }
                pst.setInt(params.size() + 1, jsonNode.get("rowid").asInt());
                int i = pst.executeUpdate();
                result.put("success", true);
                result.put("message", "修改成功");
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "修改失败" + e.getMessage());
            }
            resp.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        /**
         * 添加数据
         */
        post("/addColumn", (req, resp) -> {
            String table = req.queryParams("table");
            String data = req.queryParams("data");

            JsonNode jsonNode = DbStarter.getObjectMapper().readTree(data);
            // 构建sql语句
            StringBuilder sb = new StringBuilder("insert into " + table + " (");
            List<Object> params = new ArrayList<>();
            Iterator<String> stringIterator = jsonNode.fieldNames();
            while (stringIterator.hasNext()) {
                String key = stringIterator.next();
                params.add(jsonNode.get(key).asText());
                sb.append(key).append(",");
            }
            // 删除最后一个,
            sb.deleteCharAt(sb.length() - 1);
            sb.append(") values (");
            for (int i = 0; i < params.size() - 1; i++) {
                sb.append("?,");
            }
            sb.append("?)");
            //System.out.println(sb);
            Connection conn = DbStarter.getConnection();
            Map<String,Object> result = new HashMap<>();
            try (PreparedStatement pst = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pst.setObject(i+1,params.get(i));
                }

                int i = pst.executeUpdate();
                result.put("success", true);
                result.put("message", "添加成功");
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "添加失败" + e.getMessage());
            }
            resp.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        /**
         * 删除数据
         */
        post("/deleteColumn", (req, resp) -> {
            String table = req.queryParams("table");
            String data = req.queryParams("data");

            JsonNode jsonNode = DbStarter.getObjectMapper().readTree(data);
            // 构建sql语句
            StringBuilder sb = new StringBuilder("delete from " + table + " where 1=1");
            List<Object> params = new ArrayList<>();
            Iterator<String> stringIterator = jsonNode.fieldNames();
            while (stringIterator.hasNext()) {
                String key = stringIterator.next();
                if ("null".equals(jsonNode.get(key).asText())) {
                    // 为Null 跳过
                    continue;
                }
                params.add(jsonNode.get(key).asText());
                sb.append(" and ").append(key).append("=").append("?");
            }
            //System.out.println(sb);
            Connection conn = DbStarter.getConnection();
            Map<String,Object> result = new HashMap<>();
            try (PreparedStatement pst = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pst.setObject(i+1,params.get(i));
                }

                int i = pst.executeUpdate();
                result.put("success", true);
                result.put("message", "删除成功");
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "删除失败" + e.getMessage());
            }
            resp.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        /**
         * 删除数据
         */
        post("/deleteColumnByRowId", (req, resp) -> {
            String table = req.queryParams("table");
            String data = req.queryParams("data");

            // 构建sql语句
            String sql = "delete from " + table + " where rowid=?";
            Connection conn = DbStarter.getConnection();
            Map<String,Object> result = new HashMap<>();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1,Integer.parseInt(data));
                pst.executeUpdate();
                int i = pst.executeUpdate();
                result.put("success", true);
                result.put("message", "删除成功");
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "删除失败" + e.getMessage());
            }
            resp.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        /**
         * 添加字段
         */
        post("/addAttr", (req, resp) -> {
            String table = req.queryParams("table");
            String name = req.queryParams("name");
            String type = req.queryParams("type");
            String constraint = req.queryParams("constraint");

            // alter table student
            //    add test1 integer;

            String sql = "alter table " + table + " add " + name + " " + type;
            if (constraint != null && !constraint.isEmpty()) {
                sql += " " + constraint;
            }
            System.out.println(sql);
            Connection conn = DbStarter.getConnection();
            Map<String,Object> result = new HashMap<>();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                int i = pst.executeUpdate();
                result.put("success", true);
                result.put("message", "添加成功");
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "添加失败" + e.getMessage());
            }
            resp.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        /**
         * 删除字段
         */
        post("/deleteAttr", (req, resp) -> {
            String table = req.queryParams("table");
            String name = req.queryParams("name");

            // 编辑字段的思路是先创建一个临时表
            // 然后把数据转移过去
            // 最后删除原始表，修改临时表名
            Connection conn = DbStarter.getConnection();
            Map<String,Object> result = new HashMap<>();
            // 1. 获取这个表的创建语句
            // SELECT sql FROM sqlite_master WHERE type='table' AND name='student';
            String sql = "SELECT `sql` FROM sqlite_master WHERE type='table' AND name=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1,table);
                ResultSet resultSet = pst.executeQuery();
                if (!resultSet.next()) {
                    result.put("success", false);
                    result.put("message", "数据表不存在");
                    resp.type("application/json");
                    return DbStarter.getObjectMapper().writeValueAsString(result);
                }
                String createSql = resultSet.getString("sql");
                // 替换字段
                System.out.println(createSql);
                // 先组装一下
                String attrStr = createSql.substring(createSql.indexOf('(') + 1, createSql.length() - 1);
                String[] split = attrStr.split(",");
                StringBuilder oldAttrSb = new StringBuilder("");
                List<Attr> attrList = new ArrayList<>();
                for (String s : split) {
                    String[] tmp = s.trim() .split(" ");
                    // 字段名0 字段类型1 约束条件2
                    Attr attr = new Attr();
                    if (name.equals(tmp[0].trim())) {
                        // 如果是我们要删除的字段
                        continue;
                    }
                    oldAttrSb.append(tmp[0]).append(",");
                    attr.setName(tmp[0].trim());
                    attr.setType(tmp[1].trim());
                    if (tmp.length == 4) {
                        attr.setConstraint(tmp[2] + " " + tmp[3]);
                    } else if (tmp.length == 3) {
                        attr.setConstraint(tmp[2]);
                    }
                    attrList.add(attr);
                }
                // 2. 创建临时数据表
                StringBuilder createTableSqlSb = new StringBuilder("create table " + table + "_tmp (");
                StringBuilder attrSb = new StringBuilder("");
                for (int i = 0; i < attrList.size(); i++) {
                    Attr attr = attrList.get(i);
                    createTableSqlSb.append(attr.getName())
                            .append(" ")
                            .append(attr.getType());
                    if (attr.getConstraint() != null && !attr.getConstraint().isEmpty()) {
                        createTableSqlSb.append(" ").append(attr.getConstraint());
                    }
                    createTableSqlSb.append(",");
                    // 组装字段
                    attrSb.append(attr.getName()).append(",");
                }
                // 删除最后一个,
                createTableSqlSb.deleteCharAt(createTableSqlSb.length() - 1);
                createTableSqlSb.append(")");

                // 创建临时表
                PreparedStatement pst1 = conn.prepareStatement(createTableSqlSb.toString());
                pst1.executeUpdate();

                // 写入数据
                attrSb.deleteCharAt(attrSb.length() - 1);
                oldAttrSb.deleteCharAt(oldAttrSb.length() - 1);

                StringBuilder insertTmpSql = new StringBuilder("insert into " + table + "_tmp (");
                insertTmpSql.append(attrSb).append(") ")
                        .append("select " + oldAttrSb + " from " + table);

                System.out.println(insertTmpSql);
                PreparedStatement pst2 = conn.prepareStatement(insertTmpSql.toString());
                pst2.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "删除失败" + e.getMessage());
            }

            try {
                // 3. 删除原本的数据表
                // drop table student;
                conn.prepareStatement("drop table " + table).executeUpdate();
                // 更改临时表的名字
                // alter table student_dg_tmp
                //    rename to student;
                conn.prepareStatement("alter table " + table + "_tmp " + " rename to " + table).executeUpdate();

                result.put("success", true);
                result.put("message", "删除成功");
            } catch (Exception e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "删除失败" + e.getMessage());
            }
            resp.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        /**
         * 编辑字段
         */
        post("/editAttr", (req, resp) -> {
            String table = req.queryParams("table");
            String name = req.queryParams("name");
            String oldName = req.queryParams("old");
            String type = req.queryParams("type");
            String constraint = req.queryParams("constraint");

            // 编辑字段的思路是先创建一个临时表
            // 然后把数据转移过去
            // 最后删除原始表，修改临时表名
            Connection conn = DbStarter.getConnection();
            Map<String,Object> result = new HashMap<>();
            // 1. 获取这个表的创建语句
            // SELECT sql FROM sqlite_master WHERE type='table' AND name='student';
            String sql = "SELECT `sql` FROM sqlite_master WHERE type='table' AND name=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1,table);
                ResultSet resultSet = pst.executeQuery();
                if (!resultSet.next()) {
                    result.put("success", false);
                    result.put("message", "数据表不存在");
                    resp.type("application/json");
                    return DbStarter.getObjectMapper().writeValueAsString(result);
                }
                String createSql = resultSet.getString("sql");
                // 替换字段
                System.out.println(createSql);
                // 先组装一下
                String attrStr = createSql.substring(createSql.indexOf('(') + 1, createSql.length() - 1);
                String[] split = attrStr.split(",");
                StringBuilder oldAttrSb = new StringBuilder("");
                List<Attr> attrList = new ArrayList<>();
                for (String s : split) {
                    String[] tmp = s.trim() .split(" ");
                    // 字段名0 字段类型1 约束条件2
                    oldAttrSb.append(tmp[0]).append(",");
                    Attr attr = new Attr();
                    if (oldName.equals(tmp[0].trim())) {
                        // 如果是我们要替换的字段
                        attr.setName(name);
                        attr.setType(type);
                        attr.setConstraint(constraint);
                    } else {
                        attr.setName(tmp[0].trim());
                        attr.setType(tmp[1].trim());
                        if (tmp.length == 4) {
                            attr.setConstraint(tmp[2] + " " + tmp[3]);
                        } else if (tmp.length == 3) {
                            attr.setConstraint(tmp[2]);
                        }
                    }
                    attrList.add(attr);
                }
                // 2. 创建临时数据表
                StringBuilder createTableSqlSb = new StringBuilder("create table " + table + "_tmp (");
                StringBuilder attrSb = new StringBuilder("");
                for (int i = 0; i < attrList.size(); i++) {
                    Attr attr = attrList.get(i);
                    createTableSqlSb.append(attr.getName())
                            .append(" ")
                            .append(attr.getType());
                    if (attr.getConstraint() != null && !attr.getConstraint().isEmpty()) {
                        createTableSqlSb.append(" ").append(attr.getConstraint());
                    }
                    createTableSqlSb.append(",");
                    // 组装字段
                    attrSb.append(attr.getName()).append(",");
                }
                // 删除最后一个,
                createTableSqlSb.deleteCharAt(createTableSqlSb.length() - 1);
                createTableSqlSb.append(")");

                // 创建临时表
                PreparedStatement pst1 = conn.prepareStatement(createTableSqlSb.toString());
                pst1.executeUpdate();

                // 写入数据
                // insert into student_dg_tmp(username, real_name, password, group_id, test, test1, test2, test3, test4, test5, test7)
                //select username,
                //       real_name,
                //       password,
                //       group_id,
                //       test,
                //       test1,
                //       test2,
                //       test3,
                //       test4,
                //       test5,
                //       test6
                //from student;

                attrSb.deleteCharAt(attrSb.length() - 1);
                oldAttrSb.deleteCharAt(oldAttrSb.length() - 1);

                StringBuilder insertTmpSql = new StringBuilder("insert into " + table + "_tmp (");
                insertTmpSql.append(attrSb).append(") ")
                        .append("select " + oldAttrSb + " from " + table);

                System.out.println(insertTmpSql);
                PreparedStatement pst2 = conn.prepareStatement(insertTmpSql.toString());
                pst2.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "修改失败" + e.getMessage());
            }

            try {
                // 3. 删除原本的数据表
                // drop table student;
                conn.prepareStatement("drop table " + table).executeUpdate();
                // 更改临时表的名字
                // alter table student_dg_tmp
                //    rename to student;
                conn.prepareStatement("alter table " + table + "_tmp " + " rename to " + table).executeUpdate();

                result.put("success", true);
                result.put("message", "修改成功");
            } catch (Exception e) {
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "修改失败" + e.getMessage());
            }
            resp.type("application/json");
            return DbStarter.getObjectMapper().writeValueAsString(result);
        });

        IndexController.initIndexController();
    }
}