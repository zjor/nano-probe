package cz.royz.probe;

import com.google.gson.Gson;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProbeServlet extends HttpServlet {

    private static final String PROPERTY_KEY_NAME = "name";
    private static final String ATTRIBUTE_STATE_NAME = "stateName";

    private static final Pattern pattern = Pattern.compile(".*/(.*)$");

    private MBeanServer mBeanServer;

    private ObjectName query;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);

        try {
            query = new ObjectName("Catalina:j2eeType=WebModule,*");
        } catch (MalformedObjectNameException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        List<Map<String, String>> webapps = new ArrayList<>();

        for (ObjectName name : mBeanServer.queryNames(query, null)) {

            Matcher m = pattern.matcher(name.getKeyProperty(PROPERTY_KEY_NAME));
            if (m.find()) {
                String appName = m.group(1);
                if (appName.length() == 0) {
                    appName = "ROOT";
                }
                String status;
                try {
                    status = ("" + mBeanServer.getAttribute(name, ATTRIBUTE_STATE_NAME)).toLowerCase();
                } catch (Exception e) {
                    throw new ServletException(e);
                }
                webapps.add(mapOf("name", appName, "status", status));
            }
        }
        resp.setHeader("Content-Type", "application/json");
        resp.getWriter().write((new Gson()).toJson(webapps));
    }

    private Map<String, String> mapOf(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }
}
