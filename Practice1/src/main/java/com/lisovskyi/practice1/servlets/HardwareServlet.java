package com.lisovskyi.practice1.servlets;

import com.sun.management.OperatingSystemMXBean;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

@WebServlet(name = "hardwareServlet", value = "/hardware")
public class HardwareServlet extends HttpServlet {
    private record SystemInfo(
        long totalRAM,
        long freeRAM,
        long ramHeapMax,
        int cpuCount,
        String osName,
        String osVersion,
        String osArchitecture,
        String javaVersion,
        String username,
        double cpuLoad,
        long serverUptime,
        int threadCount,
        String jvmName
    ) {}

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        long   totalRAM         = osMXBean.getTotalMemorySize();
        long   freeRAM          = osMXBean.getFreeMemorySize();
        long   ramHeapMax       = runtime.maxMemory();
        int    cpuCount         = runtime.availableProcessors();
        String osName           = System.getProperty("os.name");
        String osVersion        = System.getProperty("os.version");
        String osArchitecture   = System.getProperty("os.arch");
        String javaVersion      = System.getProperty("java.version");
        String username         = System.getProperty("user.name");
        double cpuLoad          = osMXBean.getCpuLoad();
        long   serverUptime     = runtimeMXBean.getUptime();
        int    threadCount      = threadMXBean.getThreadCount();
        String jvmName          = runtimeMXBean.getVmName();

        SystemInfo systemInfo = new SystemInfo(totalRAM, freeRAM, ramHeapMax, cpuCount, osName, osVersion,
                osArchitecture, javaVersion, username, cpuLoad, serverUptime, threadCount, jvmName
        );

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>System Info</h1>");
        out.println("<p>Total RAM: " + systemInfo.totalRAM() / 1024 / 1024 + " MB</p>");
        out.println("<p>Free RAM: " + systemInfo.freeRAM() / 1024 / 1024 + " MB</p>");
        out.println("<p>CPU cores: " + systemInfo.cpuCount() + "</p>");
        out.println("<p>CPU load: " + String.format("%.2f", systemInfo.cpuLoad()) + "%</p>");
        out.println("<p>OS: " + systemInfo.osName() + " " + systemInfo.osVersion() + "</p>");
        out.println("<p>Architecture: " + systemInfo.osArchitecture() + "</p>");
        out.println("<p>Java: " + systemInfo.javaVersion() + "</p>");
        out.println("<p>JVM: " + systemInfo.jvmName() + "</p>");
        out.println("<p>Uptime: " + systemInfo.serverUptime() / 1000 + " sec</p>");
        out.println("<p>Threads: " + systemInfo.threadCount() + "</p>");
        out.println("<p>User: " + systemInfo.username() + "</p>");
        out.println("</body></html>");
        out.flush();
    }
}
