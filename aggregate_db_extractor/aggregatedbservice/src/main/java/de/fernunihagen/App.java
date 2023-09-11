package de.fernunihagen;

import io.javalin.Javalin;

public class App {
    public static void main(final String[] args) {
        final var controller = new Controller();

        Javalin.create()
                .updateConfig(t -> {
                    t.showJavalinBanner = false;
                })
                .get("/status", Controller::status)
                .post("/extract", controller::startExecution)
                .get("/jobs/{jobId}", controller::getJobState)
                .get("/jobs/{jobId}/results", controller::getJobResults)
                .start(7000);
    }
}
