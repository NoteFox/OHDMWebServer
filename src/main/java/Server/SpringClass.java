package Server;

import Classes.Coords;
import Classes.QueueRequest;
import Server.FTPServer.FTPService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import static Server.StaticVariables.*;

@SpringBootApplication
@RestController
public class SpringClass {

    public static Service serviceInstance;
    public static Thread serviceThread;

    public static FTPService ftpInstance;
    public static Thread ftpThread;

    public static void main(String[] args) {
        // initializes Static Variables by reading them from init.txt
        StaticVariables.init();

        try {
            setUpLogger();
        } catch (IOException e) {
            System.err.println("wasn't able to create loggerFiles ");
        }

        serviceInstance = new Service();
        serviceThread = new Thread(serviceInstance);
        serviceThread.start();

        ftpInstance = new FTPService();
        ftpThread = new Thread(ftpInstance);
        ftpThread.start();

        System.getProperties().put("server.port", webPort);
        SpringApplication.run(SpringClass.class, args);
    }

    private static void setUpLogger() throws IOException {
        if (!(new File(StaticVariables.webServiceLogFile).exists())) {
            new File(StaticVariables.logDefaultDir).mkdirs();
            new File(StaticVariables.webServiceLogFile).createNewFile();
        }

        if (!(new File(StaticVariables.ftpLogFile).exists())) {
            new File(StaticVariables.logDefaultDir).mkdirs();
            new File(StaticVariables.ftpLogFile);
        }
    }

    /** method to queue a map into the "REQUEST QUEUE" */
    public void queue_map(QueueRequest q) {
        serviceInstance.WORK_QUEUE.add(q);
        if (!serviceInstance.watcherWorking)
            serviceThread.interrupt();
    }

    @GetMapping("/service")
    public String webServiceStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("<head> <title> WebService status</title> </head>");
        sb.append("<p> Service Running = " + serviceInstance.serviceRunning + "</p>");
        sb.append("<p> WatcherThread currently working = " + serviceInstance.watcherWorking + "</p>");
        sb.append("<p> current Queue length = " + serviceInstance.WORK_QUEUE.size() + "</p>");
        sb.append("<p> QUEUE LIST: </p>");
        int i = 0;
        for (QueueRequest r: serviceInstance.WORK_QUEUE) {
            sb.append("<p>" + r.getMapName() + "</p>");
            sb.append("<p> ------------------------- </p>");
            sb.append("<p> - Position : " + i + "</p>");
            sb.append("<p> - Date : " + r.getDate() + "</p>");
            sb.append("<p> - Classes.Coords : \n" + r.getPrintableCoordsString() + "</p>");
            i++;
        }
        return sb.toString();
    }

    @GetMapping("/ftp")
    public String ftpServiceStatus() {
        return ("yeah... working on that");
    }

    // 192.168.178.35:8080/request?name=mapname&coords=13.005,15.123_13.005,15.123_13.005,15.123_13.005,15.123_13.005,15.123&date=2117-12-11
    @GetMapping("/request")
    public String request(@RequestParam(value = "name", defaultValue = "noname") String mapname,
                          @RequestParam(value = "coords", defaultValue = "13.005,15.123_13.005,15.123_13.005,15.123_13.005,15.123_13.005,15.123") String coords,
                          @RequestParam(value = "date", defaultValue = "2000-12-11") String date) {

        /**
         endpoint for map requests
         creates a new map with coordinates, date and name provided in the request.
         returns 200 and does nothing if a map with same name already exists.
         returns 201 on success
         returns 400 on missing/bad request parameters
         500 on failure while creating map
         */

        List<Coords> coordinates = new ArrayList<>();

        QueueRequest q = null;

        try {
            String[] coordsS = coords.split("_");
            float x;
            float y;

            try {
                for (String s : coordsS) {
                    x = Float.parseFloat(s.split(",")[0]);
                    y = Float.parseFloat(s.split(",")[1]);
                    coordinates.add(new Coords(x, y));
                }
            } catch (Exception e) {
                serviceInstance.log(e.toString());
            }

            q = new QueueRequest(coordinates, date, mapname);

            serviceInstance.log("given Data: MapName" + mapname + " | Date: " + date + " | coords: \n" + q.getPrintableCoordsString());

            if (coordinates.get(0).toString().equals(coordinates.get(coordinates.size() - 1).toString())) ;
            serviceInstance.log("fixing coordinates");
            coordinates.add(coordinates.get(0));

            // coord_string = str([str(x) for x in coordinates]).replace("'", "").replace("[", "").replace("]", "")

            q.setMapName(serviceInstance.sanitize_mapName(mapname));

            serviceInstance.log("mapname: " + q.getMapName());

            if (serviceInstance.map_exist(q.getMapName()))
                return "Map already exists";

            if (serviceInstance.queueAlreadyContainsMapWithGivenName(q.getMapName()))
                return "Map is already beeing created";

            // TODO: check if map was already requested, but an error was thrown
        /*if queue_map(coord_string, date, mapname) == "Error";
            return "Error while requesting map", 500*/

            queue_map(q);

            return "Map " + q.getMapName() + ".map with data from " + q.getDate() + " will be created. Check back later!\n";

            //except ValueError as e:
            //# abort(400)
            //raise e
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/test")
    public String test() {
        return ("<head> <title> Running ? </title> </head> <body> <h1> Am I running? </h1> <p> i guess i do </p> </body>");
    }


}
