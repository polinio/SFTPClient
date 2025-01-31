package com.example;

import org.testng.TestListenerAdapter;
import org.testng.TestNG;

public class StartTest {
    public static void main(String[] args) {
        TestListenerAdapter tla = new TestListenerAdapter();
        TestNG testng = new TestNG();
        testng.setTestClasses(new Class[] { SFTPClientTest.class });
        testng.addListener(tla);
        testng.run();
    }
}
