package com.tvf.clb.base.utils;

import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.entity.Race;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

@Component
@Slf4j
public class ExcelUtils {
    @Value("${path-file.excel-file}")
    private String path;

    public String getVanueId(RaceDto race){
        String vanueId = null;
        try(FileInputStream fileInputStream = new FileInputStream(path);
            Workbook workbook= new XSSFWorkbook(fileInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String meetingName = String.valueOf(row.getCell(1));
                String countryCode = String.valueOf(row.getCell(4));
                String raceType = String.valueOf(row.getCell(5));
                String state = String.valueOf(row.getCell(6));
                if (Objects.equals(meetingName,race.getMeetingName().toUpperCase()) && Objects.equals(countryCode,race.getCountryCode().toUpperCase()) && Objects.equals(raceType,ConvertBase.convertRaceType(race.getRaceType())) && Objects.equals(state,race.getState())) {
                    vanueId = String.valueOf(row.getCell(0));
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return vanueId;
    }
}
