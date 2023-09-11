package de.fernunihagen;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fernunihagen.models.ExtractionQueueItem;
import de.fernunihagen.models.ExtractionRequest;
import de.fernunihagen.models.ExtractionResponse;
import de.fernunihagen.models.ResultResponse;
import de.fernunihagen.models.StatusResponse;
import io.javalin.http.Context;

public class Controller implements IUpdateRequestState {
    public static final String JOB_ID_NOT_FOUND_TEXT = "unknown job id";
    public static final String STATE_WAITING = "waiting";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_ABORTED = "aborted";
    public static final String STATE_FINISHED = "finished";

    private final SynchronousQueue<ExtractionQueueItem> extractionQueue;
    private final Map<String, String> extractionRequestStates;
    private final ExtractionWorkerThread extractionWorkerThread;

    public Controller() {
        extractionQueue = new SynchronousQueue<>();
        extractionRequestStates = new HashMap<>();

        extractionWorkerThread = new ExtractionWorkerThread(extractionQueue, this);
        extractionWorkerThread.start();
    }

    public static void status(final Context ctx) {
        ctx.json(new StatusResponse(STATE_RUNNING));
    }

    public void startExecution(final Context ctx) throws InterruptedException {
        final var req = ctx.bodyAsClass(ExtractionRequest.class);
        final var jobId = generateJobId(req.getUri());

        final var queueItem = new ExtractionQueueItem(jobId, req.getUri(), req.getUser(), req.getPassword());

        updateRequestState(jobId, STATE_WAITING);
        extractionQueue.put(queueItem);
        ctx.json(new ExtractionResponse(jobId));
    }

    public void getJobState(final Context ctx) {
        final var state = extractionRequestStates.getOrDefault(ctx.pathParam("jobId"), JOB_ID_NOT_FOUND_TEXT);
        if (JOB_ID_NOT_FOUND_TEXT.equals(state)) {
            ctx.status(404);
        } else {
            ctx.json(new StatusResponse(state));
        }
    }

    public void getJobResults(final Context ctx) throws IOException {
        final var jobId = ctx.pathParam("jobId");
        final var state = extractionRequestStates.getOrDefault(jobId, JOB_ID_NOT_FOUND_TEXT);
        if (STATE_FINISHED.equals(state)) {
            ctx.json(new ResultResponse(getResultsForJob(jobId)));
        } else {
            ctx.status(409);
            ctx.json(new StatusResponse(state));
        }
    }

    public static String generateJobId(final String uri) {
        return "job_" + new Date().getTime() / 1000 + "_" + uri.replaceAll("[^A-Za-z0-9]", "");
    }

    private Object getResultsForJob(final String jobId) throws IOException {
        final var f = Utils.getJsonSchemaFileForJobId(jobId);
        final var o = new ObjectMapper();
        return o.readValue(Paths.get(f).toFile(), List.class);
    }

    public void updateRequestState(final String jobId, final String newState) {
        System.out.println("job " + jobId + " is now " + newState);
        extractionRequestStates.put(jobId, newState);
    }
}
