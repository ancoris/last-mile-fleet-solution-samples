package com.example.backend;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.backend.auth.grpcservice.AuthenticatedGrpcServiceProvider;
import com.example.backend.json.BackendConfigGsonProvider;
import com.google.gson.Gson;

@Singleton
public final class SpeedingServlet extends HttpServlet {
    private final ServletState servletState;
    private final AuthenticatedGrpcServiceProvider grpcServiceProvider;

    private SpeedingInfo speedingInfo = new SpeedingInfo();

    @Inject
    public SpeedingServlet(
            ServletState servletState, AuthenticatedGrpcServiceProvider grpcServiceProvider) {
        this.servletState = servletState;
        this.grpcServiceProvider = grpcServiceProvider;
    }

    private static final Logger logger = Logger.getLogger(SpeedingServlet.class.getName());

    @Override
    public void init() throws ServletException {
        logger.info("speeding init");

        super.init();

        try (FileInputStream inputStream = new FileInputStream(getSpeedingFile())) {
            logger.info("speeding init: 1");
            if (inputStream.available() > 0) {
                byte[] bytes = new byte[inputStream.available()];
                int bytesRead = inputStream.read(bytes);
                logger.info(String.format("Read %d bytes from file", bytesRead));
                String content = new String(bytes, UTF_8);
                SpeedingInfo info = BackendConfigGsonProvider.get().fromJson(content, SpeedingInfo.class);
                this.speedingInfo = info;
            }
            logger.info("speeding init: 2");
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
        logger.info("speeding init: 3");
    }

    private File getSpeedingFile() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File f = new File(tmpDir, "maps-fleet-demo-speeding-info.json.tmp");
        logger.info("speeding file: " + f.getAbsolutePath());
        return f;
    }

    @Override
    public void destroy() {
        logger.info("speeding destroy");

        String toWrite = BackendConfigGsonProvider.get().toJson(this.speedingInfo);

        logger.info("speeding destroy: 1");
        try (FileOutputStream fileOutputStream = new FileOutputStream(getSpeedingFile())) {
            logger.info("speeding destroy: 2");
            fileOutputStream.write(toWrite.getBytes(UTF_8));
            logger.info("speeding destroy: 3");
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
        logger.info("speeding destroy: 4");

        super.destroy();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("speeding doGet");
        response.setCharacterEncoding("UTF-8");

        response.setContentType("application/json");

        PrintWriter responseWriter = response.getWriter();
        responseWriter.print(BackendConfigGsonProvider.get().toJson(this.speedingInfo.getInfo()));
        responseWriter.flush();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("speeding doPost");
        ServletInputStream content = request.getInputStream();
        logger.info(content.toString());
        InputStreamReader reader = new InputStreamReader(content, UTF_8);
        logger.info(reader.toString());

        Gson gson = BackendConfigGsonProvider.get();
        try {
            SpeedingData speedingData = gson.fromJson(reader, SpeedingData.class);
            logger.info(speedingData.toString());
            this.speedingInfo.add(speedingData);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
            logger.info("speeding doPost failed");
        } finally {
            reader.close();
        }

        logger.info(this.speedingInfo.toString());

        response.setStatus(201);

        PrintWriter responseWriter = response.getWriter();
        responseWriter.print("Created");
        responseWriter.flush();
    }

    static class SpeedingInfo {
        private final Map<String, List<SpeedingData>> info;

        public SpeedingInfo() {
            this.info = new HashMap<>();
        }

        public Map<String, List<SpeedingData>> add(SpeedingData speedingData) {
            if (this.info.containsKey(speedingData.getVehicleId())) {
                this.info.get(speedingData.getVehicleId()).add(speedingData);
            } else {
                ArrayList<SpeedingData> data = new ArrayList<>();
                data.add(speedingData);
                this.info.put(speedingData.getVehicleId(), data);
            }
            return info;
        }

        public Map<String, List<SpeedingData>> getInfo() {
            return info;
        }

        public String toString() {
            return String.format("SpeedingInfo(info=%s)", info);
        }
    }

    static class SpeedingData {
        private final String vehicleId;
        private final long timestamp;
        private final String percentageAboveLimit;
        private final String speedAlertSeverity;

        public SpeedingData(String vehicleId, long timestamp, String percentageAboveLimit,
                String speedAlertSeverity) {
            this.vehicleId = vehicleId;
            this.timestamp = timestamp;
            this.percentageAboveLimit = percentageAboveLimit;
            this.speedAlertSeverity = speedAlertSeverity;
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public Instant getTimestamp() {
            return Instant.ofEpochMilli(timestamp);
        }

        public String getPercentageAboveLimit() {
            return percentageAboveLimit;
        }

        public String getSpeedAlertSeverity() {
            return speedAlertSeverity;
        }

        public String toString() {
            return String.format(
                    "SpeedingData(vehicleId=%s, timestamp=%s, percentageAboveLimit=%s, speedAlertSeverity=%s)",
                    vehicleId, timestamp, percentageAboveLimit, speedAlertSeverity);
        }
    }
}
