package org.example.framework;

import java.util.List;
import java.util.Map;

public class MessageResponse {
        private List<Map<String, Object>> data;
        private String executedSQL;
        private String username;

    public MessageResponse(List<Map<String, Object>> data, String executedSQL, String username) {
        this.data = data;
        this.executedSQL = executedSQL;
        this.username = username;
    }

    public List<Map<String, Object>> getData(){
        return data;
    }

    public void setData(List<Map<String, Object>> data){
        this.data = data;
    }

    public String getExecutedSQL(){
        return executedSQL;
    }

    public void setExecutedSQL(String executedSQL){
        this.executedSQL = executedSQL;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }
}
