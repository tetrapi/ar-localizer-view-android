package co.netguru.android.arlocalizeralternative.feature.compass

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import co.netguru.android.arlocalizeralternative.feature.location.LocationData
import co.netguru.android.arlocalizeralternative.feature.location.LocationProvider
import co.netguru.android.arlocalizeralternative.feature.orientation.OrientationData
import co.netguru.android.arlocalizeralternative.feature.orientation.OrientationProvider
import co.netguru.android.arlocalizeralternative.common.Result
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.math.roundToInt

class CompassRepositoryTest {

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var compassRepository: CompassRepository
    @Mock
    private lateinit var locationProvider: LocationProvider
    @Mock
    private lateinit var orientationProvider: OrientationProvider

    @Mock
    lateinit var observer: Observer<Result<CompassData>>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        compassRepository = CompassRepository(orientationProvider, locationProvider)
        compassRepository.compassStateLiveData.observeForever(observer)
    }

    @Test
    fun `should get location updates after starting observation`() {
        val publishSubject = PublishSubject.create<LocationData>()
        val latitude = 50.0
        val longitude = 20.0
        val compassLocation = LocationData(latitude, longitude)
        `when`(locationProvider.getLocationUpdates()).thenReturn(publishSubject.toFlowable(
            BackpressureStrategy.BUFFER))

        compassRepository.startLocationObservation()
        publishSubject.onNext(compassLocation)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.currentLocation == compassLocation)

        val latitude2 = 60.0
        val longitude2 = -20.0
        val compassLocation2 = LocationData(latitude2, longitude2)

        publishSubject.onNext(compassLocation2)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.currentLocation == compassLocation2)
    }

    @Test
    fun `should not get location updates without starting observation`() {
        val publishSubject = PublishSubject.create<LocationData>()
        val latitude = 50.0
        val longitude = 20.0
        val compassLocation = LocationData(latitude, longitude)
        `when`(locationProvider.getLocationUpdates()).thenReturn(publishSubject.toFlowable(BackpressureStrategy.BUFFER))

        publishSubject.onNext(compassLocation)

        assert(compassRepository.compassStateLiveData.value == null)
    }

    @Test
    fun `should get orientation updates after starting observation`() {
        val publishSubject = PublishSubject.create<OrientationData>()
        val currentAzimuth = 50f
        val azimuthData = OrientationData(currentAzimuth, 0f)
        `when`(orientationProvider.getSensorUpdates()).thenReturn(publishSubject.toFlowable(BackpressureStrategy.BUFFER))

        compassRepository.startSensorObservation()
        publishSubject.onNext(azimuthData)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.orientationData.currentAzimuth == currentAzimuth)

        val currentAzimuth2 = 30f
        val azimuthData2 = OrientationData(currentAzimuth2,0f)

        publishSubject.onNext(azimuthData2)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.orientationData.currentAzimuth == currentAzimuth2)
    }

    @Test
    fun `should not get orientation updates without starting observation`() {
        val publishSubject = PublishSubject.create<OrientationData>()
        val currentAzimuth = 50f
        val azimuthData = OrientationData(currentAzimuth, 0f)
        `when`(orientationProvider.getSensorUpdates()).thenReturn(publishSubject.toFlowable(BackpressureStrategy.BUFFER))

        publishSubject.onNext(azimuthData)

        assert(compassRepository.compassStateLiveData.value == null)
    }


    @Test
    fun `should stop location updates after stopping observation`() {
        val publishSubject = PublishSubject.create<LocationData>()
        val latitude = 50.0
        val longitude = 20.0
        val compassLocation = LocationData(latitude, longitude)
        `when`(locationProvider.getLocationUpdates()).thenReturn(publishSubject.toFlowable(BackpressureStrategy.BUFFER))

        compassRepository.startLocationObservation()
        publishSubject.onNext(compassLocation)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.currentLocation == compassLocation)

        val latitude2 = 60.0
        val longitude2 = -20.0
        val compassLocation2 = LocationData(latitude2, longitude2)

        compassRepository.stopLocationObservation()
        publishSubject.onNext(compassLocation2)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.currentLocation != compassLocation2)
    }

    @Test
    fun `should stop orientation updates after stopping observation`() {
        val publishSubject = PublishSubject.create<OrientationData>()
        val currentAzimuth = 50f
        val azimuthData = OrientationData(currentAzimuth, 0f)
        `when`(orientationProvider.getSensorUpdates()).thenReturn(publishSubject.toFlowable(BackpressureStrategy.BUFFER))

        compassRepository.startSensorObservation()
        publishSubject.onNext(azimuthData)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.orientationData.currentAzimuth == currentAzimuth)

        val currentAzimuth2 = 30f
        val azimuthData2 = OrientationData(currentAzimuth2, 0f)

        compassRepository.stopSensorObservation()
        publishSubject.onNext(azimuthData2)

        assert(compassRepository.compassStateLiveData.value is Result.Success)
        assert((compassRepository.compassStateLiveData.value as Result.Success).data.orientationData.currentAzimuth != currentAzimuth2)
    }

    @Test
    fun `should calculate correct heading angle based on azimuth and location`() {
        val locationPublishSubject = PublishSubject.create<LocationData>()
        var compassLocation = LocationData(51.70255082465981, 19.82147216796875)
        `when`(locationProvider.getLocationUpdates()).thenReturn(locationPublishSubject.toFlowable(BackpressureStrategy.BUFFER))

        compassRepository.startLocationObservation()
        locationPublishSubject.onNext(compassLocation)

        val orientationPublishSubject = PublishSubject.create<OrientationData>()
        `when`(orientationProvider.getSensorUpdates()).thenReturn(orientationPublishSubject.toFlowable(BackpressureStrategy.BUFFER))
        compassRepository.destination = LocationData(51.782355138660385,20.156112670898438)
        compassRepository.startSensorObservation()


        //1
        var azimuthData = OrientationData(50f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        var success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(19, success.data.currentDestinationAzimuth.roundToInt())

        //2
        azimuthData = OrientationData(320f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(109, success.data.currentDestinationAzimuth.roundToInt())

        //3
        azimuthData = OrientationData(0f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(69, success.data.currentDestinationAzimuth.roundToInt())

        //4
        azimuthData = OrientationData(360f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(69, success.data.currentDestinationAzimuth.roundToInt())


        //5
        azimuthData = OrientationData(160f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(269, success.data.currentDestinationAzimuth.roundToInt())


        // II
        compassLocation = LocationData(51.782355138660385,20.156112670898438)
        locationPublishSubject.onNext(compassLocation)
        compassRepository.destination = LocationData(51.70255082465981, 19.82147216796875)

        //1
        azimuthData = OrientationData(50f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(199, success.data.currentDestinationAzimuth.roundToInt())

        //2
        azimuthData = OrientationData(320f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(289, success.data.currentDestinationAzimuth.roundToInt())

        //3
        azimuthData = OrientationData(0f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(249, success.data.currentDestinationAzimuth.roundToInt())

        //4
        azimuthData = OrientationData(360f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(249, success.data.currentDestinationAzimuth.roundToInt())


        //5
        azimuthData = OrientationData(160f, 0f)
        orientationPublishSubject.onNext(azimuthData)

        success = (compassRepository.compassStateLiveData.value as Result.Success)
        org.junit.Assert.assertEquals(89, success.data.currentDestinationAzimuth.roundToInt())
    }
}