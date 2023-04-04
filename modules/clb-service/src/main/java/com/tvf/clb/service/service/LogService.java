package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.LogLevel;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LogService {

    public Flux<String> streamLog() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("powershell.exe", "-Command", "Get-Content", "-Path", AppConstant.DEFAULT_LOG_FILE, "-Wait");
        try {
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            return Flux.fromStream(bufferedReader.lines());
        } catch (IOException e) {
            return Flux.error(e);
        }
    }

    public String getLogByDate(LocalDate date, LogLevel logLevel) {

        StringBuilder logs = new StringBuilder();

        List<File> logFilesInDate = getAllLogFilesByNameContainsAndLevel(date.toString(), logLevel);

        if (date.equals(LocalDate.now())) {
            logFilesInDate.add(new File(AppConstant.DEFAULT_LOG_FILE));
        }

        if (CollectionUtils.isEmpty(logFilesInDate)) {
            return AppConstant.NOT_FOUND;
        }

        for (File logFile : logFilesInDate) {
            if (readLogs(logFile, logs)) {
                return AppConstant.NOT_FOUND;
            }
        }

        return logs.toString();
    }

    public String getLogByDateAndHour(LocalDate date, String hour, LogLevel logLevel) {

        StringBuilder logs = new StringBuilder();

        String fileName = String.format("%s_%s", date, hour);
        List<File> logFiles = getAllLogFilesByNameContainsAndLevel(fileName, logLevel);

        if (CollectionUtils.isEmpty(logFiles)) {
            return AppConstant.NOT_FOUND;
        }

        for (File logFile : logFiles) {
            if (readLogs(logFile, logs)) {
                return AppConstant.NOT_FOUND;
            }
        }

        return logs.toString();
    }

    private boolean readLogs(File logFile, StringBuilder logs) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                logs.append(line);
                logs.append(AppConstant.LINE_BREAK_HTML);
            }
        } catch (IOException e) {
            log.error("Can not read log from file {}", logFile.getName());
            return true;
        }
        return false;
    }

    public List<File> getAllLogFilesByNameContainsAndLevel(String name, LogLevel logLevel) {

        String folderPath = AppConstant.DEFAULT_LOG_PATH + (logLevel != null ? logLevel.getFolder() : "");

        File logFolder = new File(folderPath);

        File[] allLogFiles = logFolder.listFiles();

        if (allLogFiles == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(allLogFiles)
                .filter(logFile -> logFile.getName().contains(name))
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    }
}
