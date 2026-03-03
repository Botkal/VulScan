package com.vulscan.dashboard.service;

import com.vulscan.dashboard.dto.FeedRefreshJobDto;
import com.vulscan.dashboard.dto.FeedRefreshResultDto;
import com.vulscan.dashboard.entity.FeedType;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class FeedRefreshJobService {

    private final FeedManagementService feedManagementService;
    private final ExecutorService executor;

    private final Map<String, JobState> jobsById = new ConcurrentHashMap<>();
    private final Map<FeedType, String> runningJobByType = new ConcurrentHashMap<>();

    public FeedRefreshJobService(FeedManagementService feedManagementService) {
        this.feedManagementService = feedManagementService;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public synchronized FeedRefreshJobDto start(FeedType type) {
        String existingId = runningJobByType.get(type);
        if (existingId != null) {
            JobState existing = jobsById.get(existingId);
            if (existing != null && !existing.isTerminal()) {
                return existing.toDto();
            }
            runningJobByType.remove(type);
        }

        String jobId = UUID.randomUUID().toString();
        JobState state = new JobState(jobId, type, OffsetDateTime.now());
        jobsById.put(jobId, state);
        runningJobByType.put(type, jobId);

        Future<?> future = executor.submit(() -> runJob(state));
        state.future = future;
        return state.toDto();
    }

    public List<FeedRefreshJobDto> startEnabled() {
        List<FeedRefreshJobDto> jobs = new ArrayList<>();
        List<FeedType> orderedTypes = List.of(FeedType.KEV, FeedType.EPSS, FeedType.NVD);
        for (FeedType type : orderedTypes) {
            jobs.add(start(type));
        }
        return jobs;
    }

    public List<FeedRefreshJobDto> listJobs() {
        return jobsById.values().stream()
                .sorted(Comparator.comparing((JobState s) -> s.startedAt).reversed())
                .map(JobState::toDto)
                .toList();
    }

    public FeedRefreshJobDto cancel(String jobId) {
        JobState state = jobsById.get(jobId);
        if (state == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        if (state.isTerminal()) {
            return state.toDto();
        }

        Future<?> future = state.future;
        boolean cancelled = future != null && future.cancel(true);
        if (cancelled) {
            state.status = "CANCELLED";
            state.message = "Cancellation requested";
            state.finishedAt = OffsetDateTime.now();
            runningJobByType.remove(state.type, state.jobId);
        }
        return state.toDto();
    }

    private void runJob(JobState state) {
        try {
            state.status = "RUNNING";
            state.message = "Refresh in progress...";
            FeedRefreshResultDto result = feedManagementService.refresh(state.type);
            state.itemsRead = result.itemsRead();
            state.upserted = result.upserted();
            state.message = result.message();
            state.status = "SUCCESS";
        } catch (CancellationException ex) {
            state.status = "CANCELLED";
            state.message = "Refresh cancelled";
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted() || causedByCancellation(ex)) {
                state.status = "CANCELLED";
                state.message = "Refresh cancelled";
            } else {
                state.status = "FAILED";
                state.message = truncate(ex.getMessage(), 500);
            }
        } finally {
            state.finishedAt = OffsetDateTime.now();
            runningJobByType.remove(state.type, state.jobId);
        }
    }

    private static boolean causedByCancellation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof CancellationException || current instanceof InterruptedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String truncate(String input, int max) {
        if (input == null) {
            return null;
        }
        if (input.length() <= max) {
            return input;
        }
        return input.substring(0, max);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class JobState {
        private final String jobId;
        private final FeedType type;
        private final OffsetDateTime startedAt;
        private volatile String status = "QUEUED";
        private volatile String message = "Queued";
        private volatile OffsetDateTime finishedAt;
        private volatile Integer itemsRead;
        private volatile Integer upserted;
        private volatile Future<?> future;

        private JobState(String jobId, FeedType type, OffsetDateTime startedAt) {
            this.jobId = jobId;
            this.type = type;
            this.startedAt = startedAt;
        }

        private boolean isTerminal() {
            return "SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
        }

        private FeedRefreshJobDto toDto() {
            return new FeedRefreshJobDto(
                    jobId,
                    type.name(),
                    status,
                    message,
                    startedAt,
                    finishedAt,
                    itemsRead,
                    upserted
            );
        }
    }
}
