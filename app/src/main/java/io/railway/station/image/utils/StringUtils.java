package io.railway.station.image.utils;

public class StringUtils {

    // Получение сокращенного названия станции
    public static String getShortStation(String station, int limit) {
        if (station.length() <= limit) {
            return station;
        } else {
            String pattern = "аоеёиуэюя";
            int end = limit - 1;
            // Skip symbols from pattern
            while (pattern.indexOf(station.charAt(end)) != -1) {
                end--;
            }
            return station.substring(0, end + 1);
        }
    }

}
