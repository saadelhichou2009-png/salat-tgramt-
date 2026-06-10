package com.example.utils

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

data class PrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val fajrTimeMs: Long,
    val sunriseTimeMs: Long,
    val dhuhrTimeMs: Long,
    val asrTimeMs: Long,
    val maghribTimeMs: Long,
    val ishaTimeMs: Long
)

enum class CalculationMethod(val displayName: String, val fajrAngle: Double, val ishaAngle: Double, val isUmmAlQura: Boolean = false) {
    ALGERIA("وزارة الشؤون الدينية الجزائرية", 18.0, 17.0),
    MOROCCO("وزارة الأوقاف والشؤون الإسلامية المغربية", 19.0, 17.0),
    EGYPT("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    UMM_AL_QURA("جامعة أم القرى (مكة المكرمة)", 18.5, 0.0, true),
    MWL("رابطة العالم الإسلامي", 18.0, 17.0),
    ISNA("الجمعية الإسلامية لأمريكا الشمالية", 15.0, 15.0)
}

enum class JuristicMethod(val displayName: String, val shadowMultiplier: Double) {
    STANDARD("الشافعي، المالكي، الحنبلي", 1.0),
    HANAFI("الحنفي", 2.0)
}

object PrayerTimesCalculator {

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun dtr(d: Double): Double = d * PI / 180.0
    private fun rtd(r: Double): Double = r * 180.0 / PI

    private fun fixHour(h: Double): Double {
        var temp = h
        while (temp < 0) temp += 24.0
        while (temp >= 24) temp -= 24.0
        return temp
    }

    private fun fixAngle(a: Double): Double {
        var temp = a
        while (temp < 0) temp += 360.0
        while (temp >= 360) temp -= 360.0
        return temp
    }

    fun calculateTimes(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneOffset: Double,
        method: CalculationMethod,
        juristic: JuristicMethod
    ): PrayerTimes {
        val jd = julianDate(year, month, day)
        val d = jd - 2451545.0

        // 1. Calculate Sun Position
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val lambda = fixAngle(q + 1.915 * sin(dtr(g)) + 0.020 * sin(dtr(2.0 * g)))
        val epsilon = 23.439 - 0.00000036 * d

        // Declination
        val declination = rtd(asin(sin(dtr(epsilon)) * sin(dtr(lambda))))

        // Right ascension & Eq of Time
        var alpha = rtd(atan2(cos(dtr(epsilon)) * sin(dtr(lambda)), cos(dtr(lambda))))
        alpha = fixAngle(alpha) / 15.0 // Convert to hours

        val qHours = q / 15.0
        val eot = (qHours - alpha) * 60.0 // Eq of time in minutes

        // 2. Solar noon (transit)
        // Noon = 12h + TimezoneOffset - longitude_hours - EoT_hours
        val baseNoon = 12.0 + timeZoneOffset - (longitude / 15.0) - (eot / 60.0)
        val dhuhrHour = fixHour(baseNoon)

        // 3. Sunrise & Sunset Angle / Hour Angle Calculation
        // standard sunset height angle = -0.833
        val sunriseSunsetAngle = -0.833
        val cosH = (sin(dtr(sunriseSunsetAngle)) - sin(dtr(latitude)) * sin(dtr(declination))) / (cos(dtr(latitude)) * cos(dtr(declination)))
        val h = if (abs(cosH) <= 1.0) rtd(acos(cosH)) / 15.0 else 6.0 // Fallback to 6 hours if impossible

        val sunriseHour = fixHour(dhuhrHour - h)
        val sunsetHour = fixHour(dhuhrHour + h) // Sunset is Maghrib for general calculation

        // 4. Fajr
        val cosHFajr = (sin(dtr(-method.fajrAngle)) - sin(dtr(latitude)) * sin(dtr(declination))) / (cos(dtr(latitude)) * cos(dtr(declination)))
        val hFajr = if (abs(cosHFajr) <= 1.0) rtd(acos(cosHFajr)) / 15.0 else 6.0
        val fajrHour = fixHour(dhuhrHour - hFajr)

        // 5. Asr
        // cot(A) = M + tan(abs(latitude - declination))
        val angleAsrCot = juristic.shadowMultiplier + tan(dtr(abs(latitude - declination)))
        val angleAsr = rtd(atan(1.0 / angleAsrCot))
        val cosHAsr = (sin(dtr(angleAsr)) - sin(dtr(latitude)) * sin(dtr(declination))) / (cos(dtr(latitude)) * cos(dtr(declination)))
        val hAsr = if (abs(cosHAsr) <= 1.0) rtd(acos(cosHAsr)) / 15.0 else 3.0
        val asrHour = fixHour(dhuhrHour + hAsr)

        // 6. Maghrib and Isha
        val maghribHour = sunsetHour // Maghrib is sunset

        val ishaHour = if (method.isUmmAlQura) {
            fixHour(maghribHour + 1.5) // 90 min after Maghrib (1.5 hours)
        } else {
            val cosHIsha = (sin(dtr(-method.ishaAngle)) - sin(dtr(latitude)) * sin(dtr(declination))) / (cos(dtr(latitude)) * cos(dtr(declination)))
            val hIsha = if (abs(cosHIsha) <= 1.0) rtd(acos(cosHIsha)) / 15.0 else 6.0
            fixHour(dhuhrHour + hIsha)
        }

        // Generate Time strings & Epoch Millis for the specific date
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val fajrMs = getEpochMsForHour(calendar, fajrHour)
        val sunriseMs = getEpochMsForHour(calendar, sunriseHour)
        val dhuhrMs = getEpochMsForHour(calendar, dhuhrHour)
        val asrMs = getEpochMsForHour(calendar, asrHour)
        val maghribMs = getEpochMsForHour(calendar, maghribHour)
        val ishaMs = getEpochMsForHour(calendar, ishaHour)

        return PrayerTimes(
            fajr = formatTime(fajrHour),
            sunrise = formatTime(sunriseHour),
            dhuhr = formatTime(dhuhrHour),
            asr = formatTime(asrHour),
            maghrib = formatTime(maghribHour),
            isha = formatTime(ishaHour),
            fajrTimeMs = fajrMs,
            sunriseTimeMs = sunriseMs,
            dhuhrTimeMs = dhuhrMs,
            asrTimeMs = asrMs,
            maghribTimeMs = maghribMs,
            ishaTimeMs = ishaMs
        )
    }

    private fun getEpochMsForHour(baseCal: Calendar, decimalHour: Double): Long {
        val cal = (baseCal.clone() as Calendar)
        val hourInt = decimalHour.toInt()
        val minuteDouble = (decimalHour - hourInt) * 60.0
        val minuteInt = minuteDouble.toInt()
        val secondInt = ((minuteDouble - minuteInt) * 60.0).toInt()

        cal.set(Calendar.HOUR_OF_DAY, hourInt)
        cal.set(Calendar.MINUTE, minuteInt)
        cal.set(Calendar.SECOND, secondInt)
        return cal.timeInMillis
    }

    private fun formatTime(hour: Double): String {
        val h = hour.toInt()
        val m = ((hour - h) * 60.0).roundToInt()
        
        // Handle rounding rollover (e.g. 5:60 becomes 6:00)
        var actualH = h
        var actualM = m
        if (actualM == 60) {
            actualM = 0
            actualH = (actualH + 1) % 24
        }
        
        val padH = actualH.toString().padStart(2, '0')
        val padM = actualM.toString().padStart(2, '0')
        return "$padH:$padM"
    }

    fun calculateQiblaDirection(latitude: Double, longitude: Double): Double {
        val meccaLat = dtr(21.422524)
        val meccaLon = dtr(39.826182)
        val currLat = dtr(latitude)
        val currLon = dtr(longitude)

        val dLon = meccaLon - currLon

        val y = sin(dLon)
        val x = cos(currLat) * tan(meccaLat) - sin(currLat) * cos(dLon)

        val qiblaRad = atan2(y, x)
        var qiblaDeg = rtd(qiblaRad)

        qiblaDeg = (qiblaDeg + 360.0) % 360.0
        return qiblaDeg
    }

    // Hijri Calendar approximation (tabular)
    // For standard display purposes
    fun getHijriDate(calendar: Calendar): String {
        // Tabular Islamic Calendar Algorithm
        val jd = julianDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        val l = jd.toInt() - 1948440 + 10632
        val n = (l - 1) / 10631
        val r = l - 1 - n * 10631
        val j = (10951 - r) / 3268
        val h = r + j * 3268
        var year = n * 30 + 30 - j
        val m = (h * 30 + 15) / 10631
        val month = m + 1
        val day = h - (m * 10631 + 15) / 30

        val monthNamesAr = arrayOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        
        if (month in 1..12) {
            return "$day ${monthNamesAr[month - 1]} $year هـ"
        }
        return "$day شوال $year هـ" // default safe fallback
    }
}
