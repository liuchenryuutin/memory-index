package org.lccy.lucene.memory.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/05/13 11:45 <br>
 * @author: liuchen11
 */
public class DateUtil {

    public static final String ORACLE_DATETIME_FORMAT = "yyyy-MM-dd HH24:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_FORMAT2 = "yyyyMMdd HH:mm:ss";
    public static final String DATETIME_FORMAT3 = "yyyy/MM/dd HH:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_FORMAT2 = "yyyy/MM/dd";
    public static final String YYYYMMDD = "yyyyMMdd";
    public static final String YYYYMM = "yyyyMM";
    public static final String YYYYMMDDHHMM = "yyyyMMddHHmm";
    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    public static final String yyyyMMddHHmmssSSS = "yyyyMMddHHmmssSSS";
    public static final DateTimeFormatter DEFAUT_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

    /**
     * 获取当前时间，秒为空，例如：2023-07-13 12:13:00
     */
    public static String getCurDateNoSecond(String pattern) {
        LocalDateTime now = LocalDateTime.now().withSecond(0);
        if(!StringUtil.isEmpty(pattern)) {
            return DateTimeFormatter.ofPattern(pattern).format(now);
        } else {
            return DEFAUT_FORMATTER.format(now);
        }
    }

    /**
     * 日期转long
     * @param dateStr
     * @param formats
     * @return
     */
    public static Long convertTime(String dateStr, String... formats) {
        Long result = null;
        if(StringUtil.isEmpty(dateStr)) {
            return result;
        }
        for(String format : formats) {
            if(StringUtil.isEmpty(format)) {
                continue;
            }
            if(dateStr.length() == format.length()) {
                try {
                    Date date = new SimpleDateFormat(format).parse(dateStr);
                    result = date.getTime();
                } catch (ParseException e) {
                }
                return result;
            }
        }
        return result;
    }
}
