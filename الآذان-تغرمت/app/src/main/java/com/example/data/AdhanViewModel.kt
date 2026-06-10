package com.example.data

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.utils.CalculationMethod
import com.example.utils.JuristicMethod
import com.example.utils.PrayerTimes
import com.example.utils.PrayerTimesCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdhanViewModel(application: Application) : AndroidViewModel(application), SensorEventListener, LocationListener {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.dao())

    // --- Time State (for countdown ticks) ---
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // --- Settings / Input States ---
    private val _latitude = MutableStateFlow(35.7971) // Default to Taghramt, Morocco
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(-5.4053)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _locationName = MutableStateFlow("تغرامت (المغرب)")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _calcMethod = MutableStateFlow(CalculationMethod.MOROCCO)
    val calcMethod: StateFlow<CalculationMethod> = _calcMethod.asStateFlow()

    private val _juristicMethod = MutableStateFlow(JuristicMethod.STANDARD)
    val juristicMethod: StateFlow<JuristicMethod> = _juristicMethod.asStateFlow()

    // --- Prayer Calculation Stream ---
    val prayerTimes: StateFlow<PrayerTimes> = combine(
        _latitude, _longitude, _calcMethod, _juristicMethod
    ) { lat, lon, method, juristic ->
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzOffset = TimeZone.getDefault().rawOffset / 3600000.0

        PrayerTimesCalculator.calculateTimes(
            year, month, day, lat, lon, tzOffset, method, juristic
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrayerTimesCalculator.calculateTimes(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            35.7971, -5.4053, 1.0, CalculationMethod.MOROCCO, JuristicMethod.STANDARD
        )
    )

    // --- Compass States ---
    private val _deviceHeading = MutableStateFlow(0f)
    val deviceHeading: StateFlow<Float> = _deviceHeading.asStateFlow()

    // --- Location Permission / System States ---
    val isRtl = true

    // Sensors
    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var locationManager: LocationManager? = null

    // For today's date formatted
    val currentDateStr: String
        get() {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
            return sdf.format(Date())
        }

    val hijriDateStr: String
        get() {
            val cal = Calendar.getInstance()
            return PrayerTimesCalculator.getHijriDate(cal)
        }

    // --- Room Reactive States ---
    val todayDateKey: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

    val todayPrayerRecord: StateFlow<PrayerRecord> = repository.getPrayerRecord(todayDateKey)
        .map { it ?: PrayerRecord(todayDateKey) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PrayerRecord(todayDateKey)
        )

    val dbDhikrList: StateFlow<List<DhikrCount>> = repository.allDhikrs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Pre-defined built-in Dhikrs to initialize if empty
    private val standardDhikrs = listOf(
        DhikrCount("subhanallah", "سبحان الله"),
        DhikrCount("alhamdulillah", "الحمد لله"),
        DhikrCount("allahuakbar", "الله أكبر"),
        DhikrCount("lailahaillallah", "لا إله إلا الله"),
        DhikrCount("astaghfirullah", "أستغفر الله")
    )

    // Currently selected dhikr type in screen
    private val _selectedDhikrId = MutableStateFlow("subhanallah")
    val selectedDhikrId: StateFlow<String> = _selectedDhikrId.asStateFlow()

    // --- Adhan Player States (Hassan II Mosque Voice / مسجد الحسن الثاني) ---
    private val _isAdhanAutoEnabled = MutableStateFlow(true)
    val isAdhanAutoEnabled = _isAdhanAutoEnabled.asStateFlow()

    private val _isPlayingAdhan = MutableStateFlow(false)
    val isPlayingAdhan = _isPlayingAdhan.asStateFlow()

    private val _adhanErrorMessage = MutableStateFlow<String?>(null)
    val adhanErrorMessage = _adhanErrorMessage.asStateFlow()

    private val _activeAdhanTitle = MutableStateFlow<String>("أذان مسجد الحسن الثاني بالدار البيضاء")
    val activeAdhanTitle = _activeAdhanTitle.asStateFlow()

    private var adhanMediaPlayer: android.media.MediaPlayer? = null
    private var lastPlayedAdhanKey = ""

    init {
        // Tick timer for real-time clock, countdown and auto-adhan trigger
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _currentTime.value = now
                if (_isAdhanAutoEnabled.value) {
                    checkAndTriggerAutoAdhan(now)
                }
                delay(1000)
            }
        }

        // Initialize built-in dhikrs in database
        viewModelScope.launch {
            standardDhikrs.forEach { dhikr ->
                repository.saveDhikr(dhikr)
            }
        }

        // Setup Sensors
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        registerSensor()

        // Setup Location
        locationManager = application.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    fun selectPresetCity(name: String, lat: Double, lon: Double) {
        _locationName.value = name
        _latitude.value = lat
        _longitude.value = lon
    }

    fun updateCoordinatesManual(lat: Double, lon: Double) {
        _locationName.value = "موقع مخصص"
        _latitude.value = lat
        _longitude.value = lon
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        _calcMethod.value = method
    }

    fun updateJuristicMethod(method: JuristicMethod) {
        _juristicMethod.value = method
    }

    // --- Sensor Listeners ---
    fun registerSensor() {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregisterSensor() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ORIENTATION) {
            val degree = event.values[0]
            _deviceHeading.value = degree
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- Location Manager Listener ---
    fun requestLocationUpdates() {
        try {
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (isGpsEnabled) {
                locationManager?.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null)
            } else if (isNetworkEnabled) {
                locationManager?.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null)
            }
        } catch (e: SecurityException) {
            // Permission not granted yet or failed
        }
    }

    override fun onLocationChanged(location: Location) {
        _latitude.value = location.latitude
        _longitude.value = location.longitude
        _locationName.value = "الموقع الحالي تلقائياً"
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    // --- Room Database Write Methods ---

    fun updatePrayerTracking(prayerName: String, status: Int) {
        viewModelScope.launch {
            val currentRecord = todayPrayerRecord.value
            val updated = when (prayerName.lowercase()) {
                "fajr" -> currentRecord.copy(fajrStatus = status)
                "dhuhr" -> currentRecord.copy(dhuhrStatus = status)
                "asr" -> currentRecord.copy(asrStatus = status)
                "maghrib" -> currentRecord.copy(maghribStatus = status)
                "isha" -> currentRecord.copy(ishaStatus = status)
                else -> currentRecord
            }
            repository.savePrayerRecord(updated)
        }
    }

    fun setSelectedDhikr(id: String) {
        _selectedDhikrId.value = id
    }

    fun incrementDhikrCount() {
        val currentId = _selectedDhikrId.value
        val list = dbDhikrList.value
        val activeDhikr = list.find { it.id == currentId } ?: standardDhikrs.find { it.id == currentId }
        activeDhikr?.let {
            viewModelScope.launch {
                val newCount = it.count + 1
                repository.updateDhikrCount(currentId, newCount)
            }
        }
    }

    fun resetDhikrCount() {
        val currentId = _selectedDhikrId.value
        viewModelScope.launch {
            repository.updateDhikrCount(currentId, 0)
        }
    }

    fun addNewCustomDhikr(arabicText: String) {
        if (arabicText.isNotBlank()) {
            val id = "custom_" + UUID.randomUUID().toString().take(6)
            viewModelScope.launch {
                repository.saveDhikr(DhikrCount(id, arabicText, 0))
                _selectedDhikrId.value = id
            }
        }
    }

    fun toggleAdhanAuto(enabled: Boolean) {
        _isAdhanAutoEnabled.value = enabled
    }

    private fun checkAndTriggerAutoAdhan(nowMs: Long) {
        val stateFlow = @Suppress("SENSELESS_COMPARISON") if (prayerTimes == null) null else prayerTimes
        val activeTimes = stateFlow?.value ?: return
        val todayKey = todayDateKey // "yyyy-MM-dd"
        
        // Localized current time in HH:mm
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        val currentTimeFormatted = sdf.format(Date(nowMs))
        
        val prayers = listOf(
            Triple("fajr", activeTimes.fajr, "الفجر"),
            Triple("dhuhr", activeTimes.dhuhr, "الظهر"),
            Triple("asr", activeTimes.asr, "العصر"),
            Triple("maghrib", activeTimes.maghrib, "المغرب"),
            Triple("isha", activeTimes.isha, "العشاء")
        )
        
        for (prayer in prayers) {
            val prayerId = prayer.first
            val prayerTimeStr = prayer.second
            val prayerNameArabic = prayer.third
            
            if (currentTimeFormatted == prayerTimeStr) {
                val triggerKey = "$todayKey:$prayerId"
                if (lastPlayedAdhanKey != triggerKey) {
                    lastPlayedAdhanKey = triggerKey
                    playAdhan(prayerNameArabic)
                }
                break
            }
        }
    }

    fun playAdhan(prayerName: String = "الحسن الثاني") {
        viewModelScope.launch {
            try {
                _adhanErrorMessage.value = null
                stopAdhan()
                
                _isPlayingAdhan.value = true
                _activeAdhanTitle.value = "صوت مسجد الحسن الثاني بالدار البيضاء - أذان $prayerName"
                
                val adhanUrl = "https://www.salaat.cz/audio/adhan/Hassan-II.mp3"
                adhanMediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(adhanUrl)
                    setOnPreparedListener {
                        start()
                    }
                    setOnErrorListener { _, _, _ ->
                        triggerSystemFallbackTone()
                        _adhanErrorMessage.value = "لم نتمكن من بث أذان مسجد الحسن الثاني لعدم توفر الإنترنت. تم تشغيل تنبيه مدمج."
                        _isPlayingAdhan.value = false
                        true
                    }
                    setOnCompletionListener {
                        stopAdhan()
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                triggerSystemFallbackTone()
                _adhanErrorMessage.value = "خطأ تشغيل: ${e.localizedMessage}. تم تشغيل المقوم البديل."
                _isPlayingAdhan.value = false
            }
        }
    }

    fun stopAdhan() {
        try {
            adhanMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            adhanMediaPlayer = null
            _isPlayingAdhan.value = false
            _activeAdhanTitle.value = "أذان مسجد الحسن الثاني بالدار البيضاء"
        }
    }

    private fun triggerSystemFallbackTone() {
        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(getApplication(), notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            try {
                val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 1000)
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        unregisterSensor()
        stopAdhan()
    }
}
