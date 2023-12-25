package org.example.framework;

public class RoleCharger {
    private static String role = null;
    private static String username = null;
    private static String name = null;

    public static void setRole(String input){
        role = input;
    }

    public static String getRole(){
        return role;
    }

    public static String getUsername(){
        return username;
    }

    public static void setUsername(String input){
        username = input;
    }

    public static String getRealname(){
        return name;
    }

    public static void setRealname(String input){
        name = input;
    }

    public static boolean isStudent(){
        if("student".equals(role)){
            return true;
        }
        return false;
    }

    public static boolean isTeacher(){
        if("teacher".equals(role)){
            return true;
        }
        return false;
    }

    public static boolean isAdmin(){
        if("admin".equals(role)){
            return true;
        }
        return false;
    }
}
