package com.lisovskyi.practice1.servlets;

import java.io.*;

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(name = "surnameServlet", value = "/surname")
public class SurnameServlet extends HttpServlet {
    private String message;

    @Override
    public void init() {
        message = "Lisovskyi Arsenii";
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + message + "</h1>");
        out.println("</body></html>");
        out.flush();
    }
}
